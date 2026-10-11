package com.daftar.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppDao {

    // ---- businesses
    @Query("SELECT * FROM businesses ORDER BY createdAt, id")
    abstract fun businesses(): Flow<List<Business>>

    @Insert
    abstract suspend fun insert(item: Business): Long

    @Update
    abstract suspend fun update(item: Business)

    @Query("UPDATE businesses SET nextInvoiceNo = nextInvoiceNo + 1 WHERE id = :id")
    abstract suspend fun bumpInvoiceNo(id: Long)

    @Query("UPDATE businesses SET nextQuoteNo = nextQuoteNo + 1 WHERE id = :id")
    abstract suspend fun bumpQuoteNo(id: Long)

    @Query("DELETE FROM businesses WHERE id = :id")
    abstract suspend fun deleteBusinessRow(id: Long)

    // ---- shareholders
    @Query("SELECT * FROM shareholders WHERE businessId = :businessId ORDER BY invested DESC, id")
    abstract fun shareholders(businessId: Long): Flow<List<Shareholder>>

    @Insert
    abstract suspend fun insert(item: Shareholder): Long

    @Update
    abstract suspend fun update(item: Shareholder)

    @Query("DELETE FROM shareholders WHERE id = :id")
    abstract suspend fun deleteShareholder(id: Long)

    // ---- loans
    @Query("SELECT * FROM loans WHERE businessId = :businessId ORDER BY startDay DESC, id DESC")
    abstract fun loans(businessId: Long): Flow<List<Loan>>

    @Query("SELECT * FROM loan_payments WHERE businessId = :businessId ORDER BY day DESC, id DESC")
    abstract fun loanPayments(businessId: Long): Flow<List<LoanPayment>>

    @Insert
    abstract suspend fun insert(item: Loan): Long

    @Update
    abstract suspend fun update(item: Loan)

    @Insert
    abstract suspend fun insert(item: LoanPayment): Long

    @Query("DELETE FROM loan_payments WHERE id = :id")
    abstract suspend fun deleteLoanPayment(id: Long)

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    abstract suspend fun deletePaymentsOfLoan(loanId: Long)

    @Query("DELETE FROM loans WHERE id = :id")
    abstract suspend fun deleteLoanRow(id: Long)

    @Transaction
    open suspend fun deleteLoan(id: Long) {
        deletePaymentsOfLoan(id)
        deleteLoanRow(id)
    }

    // ---- income and expenses
    @Query("SELECT * FROM txns WHERE businessId = :businessId ORDER BY day DESC, id DESC")
    abstract fun txns(businessId: Long): Flow<List<Txn>>

    @Insert
    abstract suspend fun insert(item: Txn): Long

    @Update
    abstract suspend fun update(item: Txn)

    @Query("DELETE FROM txns WHERE id = :id")
    abstract suspend fun deleteTxn(id: Long)

    // ---- products
    @Query("SELECT * FROM products WHERE businessId = :businessId ORDER BY name COLLATE NOCASE")
    abstract fun products(businessId: Long): Flow<List<Product>>

    @Insert
    abstract suspend fun insert(item: Product): Long

    @Update
    abstract suspend fun update(item: Product)

    @Query("DELETE FROM products WHERE id = :id")
    abstract suspend fun deleteProduct(id: Long)

    // ---- invoices, quotations, purchases
    @Query("SELECT * FROM docs WHERE businessId = :businessId ORDER BY day DESC, id DESC")
    abstract fun docs(businessId: Long): Flow<List<Doc>>

    @Query("SELECT * FROM docs WHERE id = :id")
    abstract suspend fun doc(id: Long): Doc?

    @Query("SELECT * FROM doc_lines WHERE docId = :docId ORDER BY position, id")
    abstract suspend fun linesOf(docId: Long): List<DocLine>

    @Insert
    abstract suspend fun insert(item: Doc): Long

    @Update
    abstract suspend fun update(item: Doc)

    @Insert
    abstract suspend fun insertLines(items: List<DocLine>)

    @Query("DELETE FROM doc_lines WHERE docId = :docId")
    abstract suspend fun deleteLinesOf(docId: Long)

    @Query("DELETE FROM docs WHERE id = :id")
    abstract suspend fun deleteDocRow(id: Long)

    @Query("UPDATE docs SET status = :status WHERE id = :id")
    abstract suspend fun setDocStatus(id: Long, status: String)

    /** Saves a document with its lines. New invoices and quotations use up the next number. */
    @Transaction
    open suspend fun saveDoc(doc: Doc, lines: List<DocLine>): Long {
        val isNew = doc.id == 0L
        val id = if (isNew) {
            insert(doc)
        } else {
            update(doc)
            doc.id
        }
        deleteLinesOf(id)
        insertLines(
            lines.mapIndexed { index, line ->
                line.copy(id = 0, docId = id, businessId = doc.businessId, position = index)
            },
        )
        if (isNew) {
            when (doc.kind) {
                DocKind.INVOICE -> bumpInvoiceNo(doc.businessId)
                DocKind.QUOTATION -> bumpQuoteNo(doc.businessId)
            }
        }
        return id
    }

    @Transaction
    open suspend fun deleteDoc(id: Long) {
        deleteLinesOf(id)
        deleteDocRow(id)
    }

    // ---- deleting a whole business
    @Query("DELETE FROM shareholders WHERE businessId = :id")
    abstract suspend fun deleteShareholdersOf(id: Long)

    @Query("DELETE FROM loans WHERE businessId = :id")
    abstract suspend fun deleteLoansOf(id: Long)

    @Query("DELETE FROM loan_payments WHERE businessId = :id")
    abstract suspend fun deleteLoanPaymentsOf(id: Long)

    @Query("DELETE FROM txns WHERE businessId = :id")
    abstract suspend fun deleteTxnsOf(id: Long)

    @Query("DELETE FROM products WHERE businessId = :id")
    abstract suspend fun deleteProductsOf(id: Long)

    @Query("DELETE FROM docs WHERE businessId = :id")
    abstract suspend fun deleteDocsOf(id: Long)

    @Query("DELETE FROM doc_lines WHERE businessId = :id")
    abstract suspend fun deleteDocLinesOf(id: Long)

    @Transaction
    open suspend fun deleteBusiness(id: Long) {
        deleteShareholdersOf(id)
        deleteLoanPaymentsOf(id)
        deleteLoansOf(id)
        deleteTxnsOf(id)
        deleteProductsOf(id)
        deleteDocLinesOf(id)
        deleteDocsOf(id)
        deleteBusinessRow(id)
    }

    /** Runs several writes as one, which is both safer and much faster. */
    @Transaction
    open suspend fun inTransaction(block: suspend () -> Unit) {
        block()
    }

    // ---- backup and restore
    @Query("SELECT * FROM businesses")
    abstract suspend fun allBusinesses(): List<Business>

    @Query("SELECT * FROM shareholders")
    abstract suspend fun allShareholders(): List<Shareholder>

    @Query("SELECT * FROM loans")
    abstract suspend fun allLoans(): List<Loan>

    @Query("SELECT * FROM loan_payments")
    abstract suspend fun allLoanPayments(): List<LoanPayment>

    @Query("SELECT * FROM txns")
    abstract suspend fun allTxns(): List<Txn>

    @Query("SELECT * FROM products")
    abstract suspend fun allProducts(): List<Product>

    @Query("SELECT * FROM docs")
    abstract suspend fun allDocs(): List<Doc>

    @Query("SELECT * FROM doc_lines")
    abstract suspend fun allDocLines(): List<DocLine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun restoreBusinesses(items: List<Business>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun restoreShareholders(items: List<Shareholder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun restoreLoans(items: List<Loan>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun restoreLoanPayments(items: List<LoanPayment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun restoreTxns(items: List<Txn>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun restoreProducts(items: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun restoreDocs(items: List<Doc>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun restoreDocLines(items: List<DocLine>)

    @Query("DELETE FROM businesses")
    abstract suspend fun clearBusinesses()

    @Query("DELETE FROM shareholders")
    abstract suspend fun clearShareholders()

    @Query("DELETE FROM loans")
    abstract suspend fun clearLoans()

    @Query("DELETE FROM loan_payments")
    abstract suspend fun clearLoanPayments()

    @Query("DELETE FROM txns")
    abstract suspend fun clearTxns()

    @Query("DELETE FROM products")
    abstract suspend fun clearProducts()

    @Query("DELETE FROM docs")
    abstract suspend fun clearDocs()

    @Query("DELETE FROM doc_lines")
    abstract suspend fun clearDocLines()

    suspend fun exportAll(): Backup = Backup(
        businesses = allBusinesses(),
        shareholders = allShareholders(),
        loans = allLoans(),
        loanPayments = allLoanPayments(),
        txns = allTxns(),
        products = allProducts(),
        docs = allDocs(),
        lines = allDocLines(),
    )

    /** Replaces everything on the phone with the contents of a backup file. */
    @Transaction
    open suspend fun restoreAll(backup: Backup) {
        clearDocLines()
        clearDocs()
        clearProducts()
        clearTxns()
        clearLoanPayments()
        clearLoans()
        clearShareholders()
        clearBusinesses()
        // logos and scanned photos live outside the backup file; keep a link only if the file is here
        restoreBusinesses(backup.businesses.map { b -> b.copy(logoPath = b.logoPath?.takeIf { File(it).exists() }) })
        restoreShareholders(backup.shareholders)
        restoreLoans(backup.loans)
        restoreLoanPayments(backup.loanPayments)
        restoreTxns(backup.txns)
        restoreProducts(backup.products)
        restoreDocs(backup.docs.map { d -> d.copy(imagePath = d.imagePath?.takeIf { File(it).exists() }) })
        restoreDocLines(backup.lines)
    }
}

@Database(
    entities = [
        Business::class, Shareholder::class, Loan::class, LoanPayment::class,
        Txn::class, Product::class, Doc::class, DocLine::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** Version 2 adds the business logo and the "imported from" fields on documents. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE businesses ADD COLUMN logoPath TEXT")
                db.execSQL("ALTER TABLE docs ADD COLUMN source TEXT")
                db.execSQL("ALTER TABLE docs ADD COLUMN externalId TEXT")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "daftar.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
