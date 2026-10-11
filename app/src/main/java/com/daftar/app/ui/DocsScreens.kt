package com.daftar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.daftar.app.R
import com.daftar.app.data.Doc
import com.daftar.app.data.DocKind
import com.daftar.app.data.DocLine
import com.daftar.app.data.DocStatus
import com.daftar.app.data.Product
import com.daftar.app.domain.LineCalc
import com.daftar.app.domain.Money
import com.daftar.app.domain.Totals
import com.daftar.app.domain.computeTotals
import com.daftar.app.domain.lineNet
import com.daftar.app.domain.parseDecimal
import com.daftar.app.domain.trimNumber
import com.daftar.app.domain.vatFromNet
import com.daftar.app.io.PdfExporter
import java.io.File

private val KINDS = listOf(DocKind.INVOICE, DocKind.QUOTATION, DocKind.PURCHASE)

@Composable
fun statusLabel(status: String): String = stringResource(
    when (status) {
        DocStatus.PAID -> R.string.status_paid
        DocStatus.UNPAID -> R.string.status_unpaid
        DocStatus.OPEN -> R.string.status_open
        DocStatus.ACCEPTED -> R.string.status_accepted
        else -> R.string.status_declined
    },
)

@Composable
fun statusColor(status: String): Color {
    val money = LocalMoneyColors.current
    return when (status) {
        DocStatus.PAID, DocStatus.ACCEPTED -> money.positive
        DocStatus.UNPAID, DocStatus.OPEN -> money.due
        else -> money.negative
    }
}

private fun statusesFor(kind: String): List<String> =
    if (kind == DocKind.QUOTATION) {
        listOf(DocStatus.OPEN, DocStatus.ACCEPTED, DocStatus.DECLINED)
    } else {
        listOf(DocStatus.UNPAID, DocStatus.PAID)
    }

// ------------------------------------------------------------------ list

/** Filter chips: 0 = all; for invoices and purchases 1 unpaid, 2 paid, 3 overdue; for quotations 1 open, 2 accepted, 3 declined. */
private fun matchesFilter(doc: Doc, filter: Int, today: Long): Boolean {
    if (filter == 0) return true
    return if (doc.kind == DocKind.QUOTATION) {
        doc.status == listOf(DocStatus.OPEN, DocStatus.ACCEPTED, DocStatus.DECLINED)[filter - 1]
    } else {
        when (filter) {
            1 -> doc.status == DocStatus.UNPAID
            2 -> doc.status == DocStatus.PAID
            else -> doc.status == DocStatus.UNPAID && doc.dueDay != null && doc.dueDay < today
        }
    }
}

@Composable
fun DocsScreen(vm: AppViewModel, go: Go) {
    val business by vm.business.collectAsState()
    val docs by vm.docs.collectAsState()
    val b = business ?: return

    val kind = KINDS[vm.docsTab.coerceIn(0, 2)]
    val filter = vm.docsFilter.coerceIn(0, 3)
    val today = today()
    var query by rememberSaveable { mutableStateOf("") }
    val ofKind = remember(docs, kind) { docs.filter { it.kind == kind } }
    val list = remember(ofKind, filter, query, today) {
        val q = query.trim()
        ofKind.filter { doc ->
            matchesFilter(doc, filter, today) &&
                (q.isEmpty() || doc.partyName.contains(q, ignoreCase = true) || doc.number.contains(q, ignoreCase = true))
        }
    }
    val waiting = remember(ofKind) {
        ofKind.filter { it.status == DocStatus.UNPAID || it.status == DocStatus.OPEN }.sumOf { it.total }
    }
    val groups = remember(list) { list.groupBy { LocalDate.ofEpochDay(it.day).withDayOfMonth(1) } }
    val monthFormat = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

    val addLabel = stringResource(
        when (kind) {
            DocKind.INVOICE -> R.string.new_invoice
            DocKind.QUOTATION -> R.string.new_quotation
            else -> R.string.add_purchase
        },
    )
    val onAdd: () -> Unit = {
        if (kind == DocKind.PURCHASE) {
            vm.scanOpen = true
        } else {
            go.to("doc/$kind/0")
        }
    }
    val filterLabels = if (kind == DocKind.QUOTATION) {
        listOf(stringResource(R.string.all), stringResource(R.string.status_open), stringResource(R.string.status_accepted), stringResource(R.string.status_declined))
    } else {
        listOf(stringResource(R.string.all), stringResource(R.string.status_unpaid), stringResource(R.string.status_paid), stringResource(R.string.status_overdue))
    }

    AppScreen(
        title = stringResource(R.string.tab_documents),
        bottomBar = { BottomTabs("docs", go) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Segmented(
                options = listOf(
                    stringResource(R.string.invoices),
                    stringResource(R.string.quotations),
                    stringResource(R.string.purchases),
                ),
                selected = vm.docsTab,
                onSelect = {
                    vm.docsTab = it
                    vm.docsFilter = 0
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (ofKind.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Description,
                    title = stringResource(
                        when (kind) {
                            DocKind.INVOICE -> R.string.empty_invoices_title
                            DocKind.QUOTATION -> R.string.empty_quotations_title
                            else -> R.string.empty_purchases_title
                        },
                    ),
                    body = stringResource(
                        when (kind) {
                            DocKind.INVOICE -> R.string.empty_invoices_body
                            DocKind.QUOTATION -> R.string.empty_quotations_body
                            else -> R.string.empty_purchases_body
                        },
                    ),
                    actionLabel = addLabel,
                    onAction = onAdd,
                )
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_documents)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filterLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = filter == index,
                            onClick = { vm.docsFilter = index },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50),
                        )
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (waiting > 0 && filter == 0 && query.isBlank()) {
                        item(key = "waiting") {
                            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(if (kind == DocKind.QUOTATION) R.string.open_label else R.string.unpaid_label),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        Money.format(waiting) + " " + b.currency,
                                        style = MaterialTheme.typography.titleMedium.tabular(),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        item(key = "nothing") {
                            Text(
                                stringResource(R.string.nothing_matches),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    for ((month, monthDocs) in groups) {
                        item(key = "m" + month.toEpochDay()) {
                            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(month.format(monthFormat), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text(
                                    Money.format(monthDocs.sumOf { it.total }),
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(monthDocs, key = { it.id }) { doc ->
                            DocRow(doc, today) {
                                go.to(if (doc.kind == DocKind.PURCHASE) "doc/${doc.kind}/${doc.id}" else "preview/${doc.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocRow(doc: Doc, today: Long, onClick: () -> Unit) {
    val overdue = doc.status == DocStatus.UNPAID && doc.dueDay != null && doc.dueDay < today
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(doc.partyName, 42)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    doc.partyName.ifBlank { stringResource(R.string.no_name) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(doc.number, formatDay(doc.day)).filter { it.isNotBlank() }.joinToString("   "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Amount(doc.total)
                Spacer(Modifier.height(4.dp))
                if (overdue) {
                    StatusPill(stringResource(R.string.status_overdue), LocalMoneyColors.current.negative)
                } else {
                    StatusPill(statusLabel(doc.status), statusColor(doc.status))
                }
            }
        }
    }
}

// ------------------------------------------------------------------ editor

data class LineDraft(
    val description: String,
    val qty: Double,
    val unitPrice: Long,
    val vatRate: Double,
    val productId: Long? = null,
)

/** Lets the list of item lines be saved and restored along with the rest of the form. */
private val LinesSaver = listSaver<SnapshotStateList<LineDraft>, List<Any>>(
    save = { list ->
        list.map { listOf(it.description, it.qty, it.unitPrice, it.vatRate, it.productId ?: -1L) }
    },
    restore = { saved ->
        saved.map { v ->
            LineDraft(v[0] as String, v[1] as Double, v[2] as Long, v[3] as Double, (v[4] as Long).takeIf { it >= 0L })
        }.toMutableStateList()
    },
)

@Composable
fun DocEditorScreen(vm: AppViewModel, go: Go, kind: String, id: Long) {
    val business by vm.business.collectAsState()
    val products by vm.products.collectAsState()
    val b = business ?: return
    val context = LocalContext.current
    val isPurchase = kind == DocKind.PURCHASE

    // rememberSaveable: a half-written invoice survives a theme or language change, and Android
    // closing the app in the background while the camera is open.
    var docId by rememberSaveable { mutableLongStateOf(id) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var number by rememberSaveable { mutableStateOf("") }
    var partyName by rememberSaveable { mutableStateOf("") }
    var partyTrn by rememberSaveable { mutableStateOf("") }
    var partyAddress by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableLongStateOf(today()) }
    var dueDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var status by rememberSaveable { mutableStateOf(if (kind == DocKind.QUOTATION) DocStatus.OPEN else if (isPurchase) DocStatus.PAID else DocStatus.UNPAID) }
    var notes by rememberSaveable { mutableStateOf("") }
    var discount by rememberSaveable { mutableStateOf("") }
    var manualNet by rememberSaveable { mutableStateOf("") }
    var manualVat by rememberSaveable { mutableStateOf("") }
    var imagePath by rememberSaveable { mutableStateOf<String?>(null) }
    var scanMessage by rememberSaveable { mutableStateOf<Int?>(null) }
    val lines = rememberSaveable(saver = LinesSaver) { mutableStateListOf<LineDraft>() }
    var saving by remember { mutableStateOf(false) }

    var editingLine by remember { mutableStateOf<Int?>(null) }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var imageOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (loaded) return@LaunchedEffect
        if (id != 0L) {
            val stored = vm.loadDoc(id)
            if (stored == null) {
                go.backNow()
                return@LaunchedEffect
            }
            val (doc, storedLines) = stored
            number = doc.number
            partyName = doc.partyName
            partyTrn = doc.partyTrn
            partyAddress = doc.partyAddress
            day = doc.day
            dueDay = doc.dueDay
            status = doc.status
            notes = doc.notes
            discount = Money.input(doc.discount)
            imagePath = doc.imagePath
            lines.clear()
            lines.addAll(storedLines.map { LineDraft(it.description, it.qty, it.unitPrice, it.vatRate, it.productId) })
            if (storedLines.isEmpty()) {
                manualNet = Money.input(doc.subtotal)
                manualVat = Money.input(doc.vat)
            }
        } else {
            number = when (kind) {
                DocKind.INVOICE -> docNumber(b.invoicePrefix, b.nextInvoiceNo)
                DocKind.QUOTATION -> docNumber(b.quotePrefix, b.nextQuoteNo)
                else -> ""
            }
            val draft = vm.pendingScan
            if (isPurchase && draft != null) {
                vm.pendingScan = null
                imagePath = draft.imagePath
                val p = draft.parsed
                if (p == null || !p.foundAnything) {
                    scanMessage = R.string.scan_result_none
                } else {
                    partyName = p.vendor
                    partyTrn = p.trn
                    number = p.number
                    p.day?.let { day = it }
                    if (p.lines.isNotEmpty()) {
                        // use the VAT rate the supplier actually charged, which may be 0
                        val net = p.subtotal ?: 0L
                        val parsedVat = p.vat
                        val rate = if (net > 0L && parsedVat != null) Math.round(parsedVat * 10000.0 / net) / 100.0 else b.vatRate
                        lines.addAll(p.lines.map { LineDraft(it.description, it.qty, it.unitPrice, rate) })
                    }
                    manualNet = Money.input(p.subtotal ?: 0L)
                    manualVat = Money.input(p.vat ?: 0L)
                    scanMessage = if (p.vatEstimated || p.total == null) R.string.scan_result_partial else R.string.scan_result_ok
                }
            }
        }
        loaded = true
    }

    val totals: Totals = if (lines.isNotEmpty()) {
        computeTotals(lines.map { LineCalc(it.qty, it.unitPrice, it.vatRate) }, Money.parse(discount) ?: 0L)
    } else {
        Totals(Money.parse(manualNet) ?: 0L, 0L, Money.parse(manualVat) ?: 0L)
    }
    val hasAmount = if (isPurchase) lines.isNotEmpty() || totals.total > 0L else lines.isNotEmpty()
    val valid = partyName.isNotBlank() && hasAmount

    fun save(then: (Doc, List<DocLine>) -> Unit) {
        if (saving) return
        if (!valid) {
            showErrors = true
            return
        }
        saving = true
        val doc = Doc(
            id = docId,
            businessId = b.id,
            kind = kind,
            number = number.trim(),
            partyName = partyName.trim(),
            partyTrn = partyTrn.trim(),
            partyAddress = partyAddress.trim(),
            day = day,
            dueDay = dueDay,
            status = status,
            notes = notes.trim(),
            subtotal = totals.subtotal,
            discount = totals.discount,
            vat = totals.vat,
            total = totals.total,
            imagePath = imagePath,
        )
        val docLines = lines.map {
            DocLine(productId = it.productId, description = it.description, qty = it.qty, unitPrice = it.unitPrice, vatRate = it.vatRate)
        }
        vm.saveDoc(doc, docLines) { savedId ->
            docId = savedId
            saving = false
            then(doc.copy(id = savedId), docLines)
        }
    }

    val title = stringResource(
        when (kind) {
            DocKind.INVOICE -> if (id == 0L) R.string.new_invoice else R.string.invoice
            DocKind.QUOTATION -> if (id == 0L) R.string.new_quotation else R.string.quotation
            else -> if (id == 0L) R.string.add_purchase else R.string.purchase_invoice
        },
    )

    AppScreen(
        title = title,
        onBack = { go.back() },
        actions = {
            if (!isPurchase) {
                IconButton(onClick = {
                    save { doc, docLines ->
                        try {
                            PdfExporter.share(context, PdfExporter.export(context, b, doc, docLines))
                        } catch (e: Exception) {
                            // nothing installed that can receive a PDF
                        }
                    }
                }) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = stringResource(R.string.share_pdf))
                }
            }
            if (docId != 0L) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (kind == DocKind.QUOTATION) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.convert_to_invoice)) },
                                onClick = {
                                    menuOpen = false
                                    save { doc, _ ->
                                        vm.convertToInvoice(doc.id) { invoiceId -> go.replace("doc/${DocKind.INVOICE}/$invoiceId") }
                                    }
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        FormColumn(padding) {
            scanMessage?.let { message ->
                Panel(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        stringResource(message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            imagePath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = stringResource(R.string.original_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClickLabel = stringResource(R.string.view_photo)) { imageOpen = true },
                )
            }

            SectionLabel(stringResource(if (isPurchase) R.string.supplier else R.string.customer))
            Field(
                label = stringResource(if (isPurchase) R.string.supplier_name else R.string.customer_name),
                value = partyName,
                onChange = { partyName = it },
                isError = showErrors && partyName.isBlank(),
                supporting = if (showErrors && partyName.isBlank()) stringResource(R.string.required) else null,
            )
            Field(stringResource(R.string.trn_optional), partyTrn, { partyTrn = it }, keyboard = KeyboardType.Number)
            if (!isPurchase) {
                Field(stringResource(R.string.address_optional), partyAddress, { partyAddress = it })
            }

            SectionLabel(stringResource(R.string.details))
            Field(
                stringResource(if (isPurchase) R.string.supplier_invoice_number else R.string.number),
                number,
                { number = it },
                keyboard = KeyboardType.Ascii,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(stringResource(R.string.date), day, { if (it != null) day = it }, Modifier.weight(1f))
                DateField(
                    stringResource(if (kind == DocKind.QUOTATION) R.string.valid_until else R.string.due_date),
                    dueDay,
                    { dueDay = it },
                    Modifier.weight(1f),
                    optional = true,
                )
            }

            SectionLabel(stringResource(R.string.items))
            if (lines.isNotEmpty()) {
                CardList {
                    lines.forEachIndexed { index, line ->
                        if (index > 0) RowDivider()
                        ListRow(
                            title = line.description,
                            subtitle = trimNumber(line.qty) + " × " + Money.format(line.unitPrice) +
                                "  ·  " + stringResource(R.string.vat) + " " + trimNumber(line.vatRate) + "%",
                            onClick = { editingLine = index },
                            trailing = { Amount(lineNet(line.qty, line.unitPrice)) },
                        )
                    }
                }
            }
            OutlinedButton(onClick = { editingLine = -1 }, modifier = Modifier.fillMaxWidth(), shape = FieldShape) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_item))
            }
            if (showErrors && !hasAmount) {
                Text(
                    stringResource(if (isPurchase) R.string.error_need_amount else R.string.error_need_item),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (isPurchase && lines.isEmpty()) {
                Text(
                    stringResource(R.string.or_enter_totals),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyField(stringResource(R.string.amount_excl_vat), manualNet, { manualNet = it }, b.currency)
                MoneyField(stringResource(R.string.vat), manualVat, { manualVat = it }, b.currency)
                TextButton(onClick = {
                    manualVat = Money.input(vatFromNet(Money.parse(manualNet) ?: 0L, b.vatRate))
                }) {
                    Text(stringResource(R.string.calculate_vat, trimNumber(b.vatRate)))
                }
            }
            if (lines.isNotEmpty() && !isPurchase) {
                MoneyField(stringResource(R.string.discount), discount, { discount = it }, b.currency)
            }

            Panel(Modifier.fillMaxWidth()) {
                FigureRow(stringResource(R.string.subtotal), totals.subtotal)
                if (totals.discount > 0L) FigureRow(stringResource(R.string.discount), totals.discount, prefix = "-")
                FigureRow(stringResource(R.string.vat), totals.vat)
                Spacer(Modifier.height(4.dp))
                RowDividerPlain()
                Spacer(Modifier.height(4.dp))
                FigureRow(stringResource(R.string.total_with_currency, b.currency), totals.total, bold = true)
            }

            SectionLabel(stringResource(R.string.status))
            val statuses = statusesFor(kind)
            Segmented(
                options = statuses.map { statusLabel(it) },
                selected = statuses.indexOf(status).coerceAtLeast(0),
                onSelect = { status = statuses[it] },
            )

            Field(stringResource(R.string.notes), notes, { notes = it }, singleLine = false)
            SaveButton(onClick = {
                val isNew = docId == 0L
                save { doc, _ ->
                    // a new invoice or quotation goes straight to its preview, ready to share
                    if (isNew && !isPurchase) go.replace("preview/${doc.id}") else go.back()
                }
            })
            Spacer(Modifier.height(8.dp))
        }
    }

    editingLine?.let { index ->
        LineDialog(
            initial = lines.getOrNull(index),
            products = products,
            defaultVat = b.vatRate,
            currency = b.currency,
            useCost = isPurchase,
            onDismiss = { editingLine = null },
            onSave = { line ->
                if (index in lines.indices) lines[index] = line else lines.add(line)
                editingLine = null
            },
            onDelete = if (index in lines.indices) {
                {
                    lines.removeAt(index)
                    editingLine = null
                }
            } else {
                null
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_document_title),
            body = stringResource(R.string.delete_cannot_undo),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                vm.deleteDoc(Doc(id = docId, imagePath = imagePath)) { go.back() }
            },
            onDismiss = { confirmDelete = false },
        )
    }

    val shownImage = imagePath
    if (imageOpen && shownImage != null) {
        Dialog(onDismissRequest = { imageOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(onClickLabel = stringResource(R.string.close)) { imageOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = File(shownImage),
                    contentDescription = stringResource(R.string.original_photo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun RowDividerPlain() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun LineDialog(
    initial: LineDraft?,
    products: List<Product>,
    defaultVat: Double,
    currency: String,
    useCost: Boolean,
    onDismiss: () -> Unit,
    onSave: (LineDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var qty by remember { mutableStateOf(trimNumber(initial?.qty ?: 1.0)) }
    var price by remember { mutableStateOf(Money.input(initial?.unitPrice ?: 0L)) }
    var vat by remember { mutableStateOf(trimNumber(initial?.vatRate ?: defaultVat)) }
    var productId by remember { mutableStateOf(initial?.productId) }
    var pickerOpen by remember { mutableStateOf(false) }

    val qtyValue = parseDecimal(qty)
    val priceValue = Money.parse(price)
    val vatValue = parseDecimal(vat)
    val valid = description.isNotBlank() && qtyValue != null && qtyValue > 0.0 &&
        priceValue != null && priceValue >= 0L && vatValue != null && vatValue >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.add_item else R.string.edit_item)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (products.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { pickerOpen = true }, modifier = Modifier.fillMaxWidth(), shape = FieldShape) {
                            Text(stringResource(R.string.choose_product))
                        }
                        DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                            for (product in products) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(product.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                Money.format(if (useCost) product.cost else product.price) + " " + currency,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        description = product.name
                                        price = Money.input(if (useCost) product.cost else product.price)
                                        vat = trimNumber(product.vatRate)
                                        productId = product.id
                                        pickerOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
                Field(stringResource(R.string.description), description, { description = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(stringResource(R.string.quantity), qty, { qty = it }, Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                    Field(stringResource(R.string.vat_rate), vat, { vat = it }, Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "%")
                }
                MoneyField(stringResource(R.string.unit_price), price, { price = it }, currency)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        LineDraft(
                            description = description.trim(),
                            qty = qtyValue ?: 1.0,
                            unitPrice = priceValue ?: 0L,
                            vatRate = vatValue ?: 0.0,
                            productId = productId,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
