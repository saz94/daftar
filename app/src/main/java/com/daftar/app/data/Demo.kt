package com.daftar.app.data

import java.time.LocalDate

/**
 * Sample data, used only for taking screenshots of the app: it is loaded when the app is started
 * with the "demo" launch flag and has no businesses yet. Nothing in the normal app reaches it.
 */
suspend fun AppDao.seedDemo() {
    if (allBusinesses().isNotEmpty()) return
    val today = LocalDate.now()
    val b = insert(
        Business(
            name = "Noor Trading",
            trn = "100234567890003",
            address = "Warehouse 12, Al Quoz 3, Dubai",
            phone = "+971 4 338 1234",
            email = "accounts@noortrading.example",
            createdAt = 1,
        ),
    )
    insert(Business(name = "Sahel Coffee", colorIndex = 3, createdAt = 2))

    insert(Product(businessId = b, name = "Linen shirt", sku = "LS-01", unit = "pcs", price = 18000, cost = 9500, stock = 42.0))
    insert(Product(businessId = b, name = "Cotton abaya", sku = "CA-07", unit = "pcs", price = 32000, cost = 17000, stock = 15.0))
    insert(Product(businessId = b, name = "Leather sandals", sku = "SD-11", unit = "pair", price = 24000, cost = 12000, stock = 8.0))
    insert(Product(businessId = b, name = "Gift wrapping", unit = "pcs", price = 1500, cost = 400, stock = 200.0))

    insert(Shareholder(businessId = b, name = "Saeed Al Marri", invested = 150_000_00, joinedDay = today.minusYears(1).toEpochDay()))
    insert(Shareholder(businessId = b, name = "Mariam Khalid", invested = 90_000_00, joinedDay = today.minusYears(1).toEpochDay()))
    insert(Shareholder(businessId = b, name = "Omar Trading Holdings", invested = 60_000_00, joinedDay = today.minusMonths(7).toEpochDay()))

    val loan = insert(
        Loan(
            businessId = b, lender = "Emirates Business Bank", principal = 80_000_00, interestRate = 6.5,
            startDay = today.minusMonths(8).toEpochDay(), dueDay = today.plusDays(18).toEpochDay(),
        ),
    )
    for (m in 1..4) {
        insert(LoanPayment(businessId = b, loanId = loan, amount = 10_000_00, day = today.minusMonths(m.toLong()).toEpochDay()))
    }

    val customers = listOf("Al Maha Boutique", "بيت الخياطة", "Gulf Uniforms FZE", "Layla Hassan", "Desert Rose Trading")
    var n = 1
    suspend fun invoice(customer: String, day: LocalDate, qty: Double, price: Long, status: String, due: LocalDate?) {
        val net = Math.round(qty * price)
        val vat = Math.round(net * 0.05)
        saveDoc(
            Doc(
                businessId = b, kind = DocKind.INVOICE, number = "INV-" + n.toString().padStart(4, '0'),
                partyName = customer, partyTrn = if (n % 2 == 0) "100555444333222" else "", partyAddress = "Dubai, UAE",
                day = day.toEpochDay(), dueDay = due?.toEpochDay(), status = status,
                subtotal = net, vat = vat, total = net + vat, notes = "Thank you for your business.",
            ),
            listOf(
                DocLine(description = "Linen shirt, assorted sizes", qty = qty, unitPrice = price),
            ),
        )
        n++
    }
    val monthStart = today.withDayOfMonth(1)
    val amounts = listOf(14.0 to 18000L, 9.0 to 32000L, 22.0 to 18000L, 6.0 to 24000L, 30.0 to 18000L, 12.0 to 32000L)
    for (back in 5 downTo 1) {
        val start = monthStart.minusMonths(back.toLong())
        val (q1, p1) = amounts[back % amounts.size]
        val (q2, p2) = amounts[(back + 2) % amounts.size]
        invoice(customers[back % 5], start.plusDays(3), q1 + back, p1, DocStatus.PAID, start.plusDays(17))
        invoice(customers[(back + 1) % 5], start.plusDays(15), q2, p2, DocStatus.PAID, start.plusDays(29))
    }
    invoice(customers[0], today.minusDays(40), 8.0, 32000, DocStatus.UNPAID, today.minusDays(10))
    invoice(customers[1], today.minusDays(6), 16.0, 18000, DocStatus.UNPAID, today.plusDays(8))
    invoice(customers[2], today.minusDays(3), 25.0, 18000, DocStatus.PAID, today.plusDays(11))
    invoice(customers[4], today.minusDays(1), 10.0, 24000, DocStatus.UNPAID, today.plusDays(13))

    var q = 1
    for ((i, status) in listOf(DocStatus.OPEN, DocStatus.OPEN, DocStatus.ACCEPTED).withIndex()) {
        val net = (40 + 15 * i) * 18000L
        saveDoc(
            Doc(
                businessId = b, kind = DocKind.QUOTATION, number = "QUO-" + q.toString().padStart(4, '0'),
                partyName = customers[(i + 2) % 5], day = today.minusDays(2L + 4 * i).toEpochDay(),
                dueDay = today.plusDays(20).toEpochDay(), status = status,
                subtotal = net, vat = Math.round(net * 0.05), total = net + Math.round(net * 0.05),
            ),
            listOf(DocLine(description = "Staff uniforms, embroidered", qty = 40.0 + 15 * i, unitPrice = 18000)),
        )
        q++
    }

    val suppliers = listOf("Al Noor Textiles", "City Stationery", "Falcon Packaging", "Dubai Thread Co.")
    for (back in 5 downTo 0) {
        val day = monthStart.minusMonths(back.toLong()).plusDays(1)
        if (day.isAfter(today)) continue
        val net = (9000L + 1400 * back) * 100
        saveDoc(
            Doc(
                businessId = b, kind = DocKind.PURCHASE, number = "S-" + (4100 + back), partyName = suppliers[back % 4],
                partyTrn = "100987654321009", day = day.toEpochDay(),
                status = if (back == 0) DocStatus.UNPAID else DocStatus.PAID,
                subtotal = net, vat = Math.round(net * 0.05), total = net + Math.round(net * 0.05),
            ),
            emptyList(),
        )
    }

    for (back in 5 downTo 0) {
        val start = monthStart.minusMonths(back.toLong())
        fun day(offset: Long): Long = minOf(start.plusDays(offset), today).toEpochDay()
        insert(Txn(businessId = b, type = TxnType.EXPENSE, category = "Rent", description = "Warehouse rent", party = "Al Quoz Properties", net = 6_500_00, day = day(0)))
        insert(Txn(businessId = b, type = TxnType.EXPENSE, category = "Salaries", description = "Staff salaries", net = 9_800_00 + back * 150_00L, day = day(1)))
        insert(Txn(businessId = b, type = TxnType.EXPENSE, category = "Marketing", description = "Instagram campaign", party = "Meta", net = 1_900_00 + (back % 3) * 600_00L, vat = 95_00, vatRate = 5.0, day = day(2)))
        insert(Txn(businessId = b, type = TxnType.EXPENSE, category = "Utilities", description = "DEWA and internet", net = 1_150_00, vat = 57_50, vatRate = 5.0, day = day(2)))
        insert(Txn(businessId = b, type = TxnType.INCOME, category = "Sales", description = "Cash sales, pop-up stall", net = 3_200_00 + (5 - back) * 450_00L, vat = 160_00, vatRate = 5.0, day = day(2)))
    }
}
