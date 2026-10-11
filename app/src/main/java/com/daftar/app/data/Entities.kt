package com.daftar.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// Amounts are Long minor units (fils). Dates are epoch days (LocalDate.toEpochDay()).

object DocKind {
    const val INVOICE = "INVOICE"
    const val QUOTATION = "QUOTATION"
    const val PURCHASE = "PURCHASE"
}

object DocStatus {
    const val UNPAID = "UNPAID"
    const val PAID = "PAID"
    const val OPEN = "OPEN"
    const val ACCEPTED = "ACCEPTED"
    const val DECLINED = "DECLINED"
}

object TxnType {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"
}

@Serializable
@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val trn: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val currency: String = "AED",
    val vatRate: Double = 5.0,
    val invoicePrefix: String = "INV-",
    val quotePrefix: String = "QUO-",
    val nextInvoiceNo: Int = 1,
    val nextQuoteNo: Int = 1,
    val colorIndex: Int = 0,
    val createdAt: Long = 0,
)

@Serializable
@Entity(tableName = "shareholders", indices = [Index("businessId")])
data class Shareholder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 0,
    val name: String = "",
    val phone: String = "",
    val invested: Long = 0,
    /** Ownership percentage. Null means "work it out from the amount invested". */
    val sharePercent: Double? = null,
    val joinedDay: Long = 0,
    val note: String = "",
)

@Serializable
@Entity(tableName = "loans", indices = [Index("businessId")])
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 0,
    val lender: String = "",
    val principal: Long = 0,
    val interestRate: Double = 0.0,
    val startDay: Long = 0,
    val dueDay: Long? = null,
    val note: String = "",
)

@Serializable
@Entity(tableName = "loan_payments", indices = [Index("businessId"), Index("loanId")])
data class LoanPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 0,
    val loanId: Long = 0,
    val amount: Long = 0,
    val day: Long = 0,
    val note: String = "",
)

/** A simple income or expense entry that has no invoice behind it. */
@Serializable
@Entity(tableName = "txns", indices = [Index("businessId")])
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 0,
    val type: String = TxnType.EXPENSE,
    val category: String = "",
    val description: String = "",
    val party: String = "",
    /** Amount excluding VAT. */
    val net: Long = 0,
    val vat: Long = 0,
    val vatRate: Double = 0.0,
    val day: Long = 0,
)

@Serializable
@Entity(tableName = "products", indices = [Index("businessId")])
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 0,
    val name: String = "",
    val sku: String = "",
    val unit: String = "",
    val price: Long = 0,
    val cost: Long = 0,
    val vatRate: Double = 5.0,
    val stock: Double = 0.0,
)

/** A sales invoice, a quotation, or a purchase invoice received from a supplier. */
@Serializable
@Entity(tableName = "docs", indices = [Index("businessId")])
data class Doc(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 0,
    val kind: String = DocKind.INVOICE,
    val number: String = "",
    val partyName: String = "",
    val partyTrn: String = "",
    val partyAddress: String = "",
    val day: Long = 0,
    val dueDay: Long? = null,
    val status: String = DocStatus.UNPAID,
    val notes: String = "",
    val subtotal: Long = 0,
    val discount: Long = 0,
    val vat: Long = 0,
    val total: Long = 0,
    /** Photo of the original paper invoice, when it was imported from a picture. */
    val imagePath: String? = null,
)

/** Amount excluding VAT, after discount. */
val Doc.net: Long get() = subtotal - discount

@Serializable
@Entity(tableName = "doc_lines", indices = [Index("docId"), Index("businessId")])
data class DocLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docId: Long = 0,
    val businessId: Long = 0,
    val productId: Long? = null,
    val description: String = "",
    val qty: Double = 1.0,
    val unitPrice: Long = 0,
    val vatRate: Double = 5.0,
    val position: Int = 0,
)

@Serializable
data class Backup(
    val version: Int = 1,
    val businesses: List<Business> = emptyList(),
    val shareholders: List<Shareholder> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val loanPayments: List<LoanPayment> = emptyList(),
    val txns: List<Txn> = emptyList(),
    val products: List<Product> = emptyList(),
    val docs: List<Doc> = emptyList(),
    val lines: List<DocLine> = emptyList(),
)
