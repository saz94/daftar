package com.daftar.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.data.AppDatabase
import com.daftar.app.data.Backup
import com.daftar.app.data.Business
import com.daftar.app.data.Doc
import com.daftar.app.data.DocKind
import com.daftar.app.data.DocLine
import com.daftar.app.data.DocStatus
import com.daftar.app.data.Loan
import com.daftar.app.data.LoanPayment
import com.daftar.app.data.Prefs
import com.daftar.app.data.Product
import com.daftar.app.data.Shareholder
import com.daftar.app.data.Txn
import com.daftar.app.data.TxnType
import com.daftar.app.data.net
import com.daftar.app.data.seedDemo
import com.daftar.app.domain.InvoiceParser
import com.daftar.app.domain.ParsedInvoice
import com.daftar.app.domain.Period
import com.daftar.app.domain.groupRows
import com.daftar.app.io.OcrScanner
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** What a scanned photo produced, waiting to be opened in the purchase form. */
class ScanDraft(val imagePath: String, val parsed: ParsedInvoice?)

data class Summary(val income: Long, val expenses: Long, val outputVat: Long, val inputVat: Long) {
    val profit: Long get() = income - expenses
    val vatDue: Long get() = outputVat - inputVat
}

/**
 * Income = sales invoices + income entries. Expenses = purchase invoices + expense entries.
 * All excluding VAT; VAT is tracked separately because it is owed to (or reclaimed from) the FTA.
 * Quotations never count.
 */
fun summarize(docs: List<Doc>, txns: List<Txn>, range: LongRange?): Summary {
    var income = 0L
    var expenses = 0L
    var outputVat = 0L
    var inputVat = 0L
    for (d in docs) {
        if (range != null && d.day !in range) continue
        when (d.kind) {
            DocKind.INVOICE -> {
                income += d.net
                outputVat += d.vat
            }
            DocKind.PURCHASE -> {
                expenses += d.net
                inputVat += d.vat
            }
        }
    }
    for (t in txns) {
        if (range != null && t.day !in range) continue
        if (t.type == TxnType.INCOME) {
            income += t.net
            outputVat += t.vat
        } else {
            expenses += t.net
            inputVat += t.vat
        }
    }
    return Summary(income, expenses, outputVat, inputVat)
}

fun today(): Long = LocalDate.now().toEpochDay()

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    val prefs = Prefs(app)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    private val businessesSource: Flow<List<Business>?> = dao.businesses()

    /** null while the database is still loading; an empty list means first launch. */
    val businesses: StateFlow<List<Business>?> =
        businessesSource.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val business: StateFlow<Business?> =
        combine(businesses, prefs.currentBusinessId) { list, id ->
            list?.firstOrNull { it.id == id } ?: list?.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val businessId: Flow<Long> = business.map { it?.id ?: 0L }.distinctUntilChanged()

    private fun <T> scoped(query: (Long) -> Flow<List<T>>): StateFlow<List<T>> =
        businessId.flatMapLatest { id -> query(id).onStart { emit(emptyList()) } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val shareholders: StateFlow<List<Shareholder>> = scoped { dao.shareholders(it) }
    val loans: StateFlow<List<Loan>> = scoped { dao.loans(it) }
    val loanPayments: StateFlow<List<LoanPayment>> = scoped { dao.loanPayments(it) }
    val txns: StateFlow<List<Txn>> = scoped { dao.txns(it) }
    val products: StateFlow<List<Product>> = scoped { dao.products(it) }
    val docs: StateFlow<List<Doc>> = scoped { dao.docs(it) }

    // ---- screen state that should survive moving between tabs
    var period by mutableStateOf(Period())
    var docsTab by mutableIntStateOf(0)
    /** Which filter chip is on in the documents list; see matchesFilter. */
    var docsFilter by mutableIntStateOf(0)
    var moneyTab by mutableIntStateOf(0)

    /** The "create something" sheet behind the centre + button, and the scan-a-bill sheet. */
    var createOpen by mutableStateOf(false)
    var scanOpen by mutableStateOf(false)

    // ---- businesses
    fun selectBusiness(id: Long) = prefs.setCurrentBusiness(id)

    fun saveBusiness(item: Business, onDone: () -> Unit = {}) = viewModelScope.launch {
        if (item.id == 0L) {
            val id = dao.insert(item.copy(createdAt = System.currentTimeMillis()))
            prefs.setCurrentBusiness(id)
        } else {
            dao.update(item)
        }
        onDone()
    }

    fun deleteBusiness(id: Long, onDone: () -> Unit = {}) = viewModelScope.launch {
        dao.deleteBusiness(id)
        onDone()
    }

    // ---- shareholders
    fun saveShareholder(item: Shareholder) = viewModelScope.launch {
        if (item.id == 0L) dao.insert(item) else dao.update(item)
    }

    fun deleteShareholder(id: Long) = viewModelScope.launch { dao.deleteShareholder(id) }

    // ---- loans
    fun saveLoan(item: Loan) = viewModelScope.launch {
        if (item.id == 0L) dao.insert(item) else dao.update(item)
    }

    fun deleteLoan(id: Long) = viewModelScope.launch { dao.deleteLoan(id) }

    fun addLoanPayment(item: LoanPayment) = viewModelScope.launch { dao.insert(item) }

    fun deleteLoanPayment(id: Long) = viewModelScope.launch { dao.deleteLoanPayment(id) }

    // ---- income and expenses
    fun saveTxn(item: Txn) = viewModelScope.launch {
        if (item.id == 0L) dao.insert(item) else dao.update(item)
    }

    fun deleteTxn(id: Long) = viewModelScope.launch { dao.deleteTxn(id) }

    // ---- products
    fun saveProduct(item: Product) = viewModelScope.launch {
        if (item.id == 0L) dao.insert(item) else dao.update(item)
    }

    fun deleteProduct(id: Long) = viewModelScope.launch { dao.deleteProduct(id) }

    // ---- documents
    suspend fun loadDoc(id: Long): Pair<Doc, List<DocLine>>? {
        val doc = dao.doc(id) ?: return null
        return doc to dao.linesOf(id)
    }

    fun saveDoc(doc: Doc, lines: List<DocLine>, onSaved: (Long) -> Unit) = viewModelScope.launch {
        onSaved(dao.saveDoc(doc, lines))
    }

    fun setDocStatus(id: Long, status: String) = viewModelScope.launch { dao.setDocStatus(id, status) }

    fun deleteDoc(doc: Doc, onDone: () -> Unit) = viewModelScope.launch {
        dao.deleteDoc(doc.id)
        doc.imagePath?.let { path -> withContext(Dispatchers.IO) { File(path).delete() } }
        onDone()
    }

    /** Copies an accepted quotation into a new unpaid invoice and returns the invoice id. */
    fun convertToInvoice(quoteId: Long, onDone: (Long) -> Unit) = viewModelScope.launch {
        val b = business.value ?: return@launch
        val (quote, lines) = loadDoc(quoteId) ?: return@launch
        val invoice = quote.copy(
            id = 0,
            kind = DocKind.INVOICE,
            number = docNumber(b.invoicePrefix, b.nextInvoiceNo),
            day = today(),
            dueDay = null,
            status = DocStatus.UNPAID,
        )
        val id = dao.saveDoc(invoice, lines)
        dao.setDocStatus(quoteId, DocStatus.ACCEPTED)
        onDone(id)
    }

    // ---- importing an invoice from a picture
    var scanning by mutableStateOf(false)
        private set
    var pendingScan: ScanDraft? = null

    fun newScanFile(): File {
        val dir = File(getApplication<Application>().filesDir, "scans").apply { mkdirs() }
        return File(dir, "scan_" + System.currentTimeMillis() + ".jpg")
    }

    fun scan(file: File, onReady: () -> Unit) = viewModelScope.launch {
        val b = business.value
        scanning = true
        val parsed = try {
            val lines = OcrScanner.read(file)
            withContext(Dispatchers.Default) {
                InvoiceParser.parse(groupRows(lines), b?.trn ?: "", b?.vatRate ?: 5.0)
            }
        } catch (e: Throwable) {
            // unreadable or oversized picture: fall back to typing the details in
            null
        }
        pendingScan = ScanDraft(file.absolutePath, parsed)
        scanning = false
        onReady()
    }

    /** Copies a picture chosen from the gallery into the app, then reads it. */
    fun importAndScan(uri: Uri, onReady: () -> Unit) = viewModelScope.launch {
        scanning = true
        val file = newScanFile()
        val copied = withContext(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } != null
            } catch (e: Exception) {
                false
            }
        }
        if (copied) {
            scan(file, onReady)
        } else {
            scanning = false
        }
    }

    /** Screenshot support only; see data/Demo.kt. */
    fun seedDemo() = viewModelScope.launch {
        dao.seedDemo()
        // a drawn stand-in logo, so screenshots show where a real one goes
        val first = dao.allBusinesses().firstOrNull() ?: return@launch
        if (first.logoPath != null) return@launch
        val path = withContext(Dispatchers.IO) {
            val bitmap = Bitmap.createBitmap(640, 220, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            paint.color = 0xFF2340B8.toInt()
            canvas.drawCircle(110f, 110f, 100f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(110f, 110f, 46f, paint)
            paint.color = 0xFF141A2E.toInt()
            paint.textSize = 132f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("noor", 240f, 156f, paint)
            val dir = File(getApplication<Application>().filesDir, "logos").apply { mkdirs() }
            val file = File(dir, "demo_logo.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.absolutePath
        }
        dao.update(first.copy(logoPath = path))
    }

    // ---- logo
    /** Copies a chosen picture into the app as a small PNG and returns its path, or null. */
    fun importLogo(uri: Uri, onDone: (String?) -> Unit) = viewModelScope.launch {
        val path = withContext(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val bounds = BitmapFactory.Options()
                bounds.inJustDecodeBounds = true
                app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1200) sample *= 2
                val options = BitmapFactory.Options()
                options.inSampleSize = sample
                val bitmap = app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                if (bitmap == null) {
                    null
                } else {
                    val dir = File(app.filesDir, "logos").apply { mkdirs() }
                    val file = File(dir, "logo_" + System.currentTimeMillis() + ".png")
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    file.absolutePath
                }
            } catch (e: Throwable) {
                null
            }
        }
        onDone(path)
    }

    // ---- backup
    fun exportBackup(uri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            try {
                val text = json.encodeToString(dao.exportAll())
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                } != null
            } catch (e: Exception) {
                false
            }
        }
        onDone(ok)
    }

    fun importBackup(uri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            try {
                val text = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) }
                if (text == null) {
                    false
                } else {
                    val backup = json.decodeFromString<Backup>(text)
                    dao.restoreAll(backup)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
        onDone(ok)
    }
}

fun docNumber(prefix: String, next: Int): String = prefix + next.toString().padStart(4, '0')
