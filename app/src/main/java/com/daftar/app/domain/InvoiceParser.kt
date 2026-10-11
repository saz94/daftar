package com.daftar.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** One line of recognised text with its position on the photo (pixels). */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

data class ParsedLine(val description: String, val qty: Double, val unitPrice: Long, val amount: Long)

data class ParsedInvoice(
    val vendor: String = "",
    val trn: String = "",
    val number: String = "",
    val day: Long? = null,
    val subtotal: Long? = null,
    val vat: Long? = null,
    val total: Long? = null,
    /** True when VAT was not printed clearly and was worked out from the total instead. */
    val vatEstimated: Boolean = false,
    val lines: List<ParsedLine> = emptyList(),
) {
    val foundAnything: Boolean
        get() = vendor.isNotEmpty() || trn.isNotEmpty() || number.isNotEmpty() || day != null || total != null
}

/**
 * Text recognition returns fragments in reading-block order, which splits "Total" from its amount
 * when they sit at opposite edges of the page. This stitches fragments that share a baseline back
 * into visual rows, left to right.
 */
fun groupRows(lines: List<OcrLine>): List<String> {
    if (lines.isEmpty()) return emptyList()
    val sorted = lines.sortedBy { (it.top + it.bottom) / 2 }
    val rows = ArrayList<MutableList<OcrLine>>()
    var rowCenter = 0.0
    for (line in sorted) {
        val center = (line.top + line.bottom) / 2.0
        val height = (line.bottom - line.top).coerceAtLeast(1)
        val current = rows.lastOrNull()
        if (current != null && Math.abs(center - rowCenter) <= height * 0.55) {
            current.add(line)
            rowCenter = current.map { (it.top + it.bottom) / 2.0 }.average()
        } else {
            rows.add(mutableListOf(line))
            rowCenter = center
        }
    }
    return rows.map { row -> row.sortedBy { it.left }.joinToString("   ") { it.text.trim() } }
}

object InvoiceParser {

    private val NUMBER = Regex("""\d[\d,]*(?:\.\d+)?""")
    private val TRN = Regex("""(?<!\d)100\d{12}(?!\d)""")
    private val DIGIT_GAP = Regex("""(?<=\d)[ \-](?=\d)""")
    private val INVOICE_NO = Regex(
        """\b(?:tax\s+invoice|invoice|inv|bill|receipt)\b\s*(?:number|num|no|#)?\s*[.:#\-]*\s*((?=[A-Za-z/\-]*\d)[A-Za-z0-9][A-Za-z0-9/\-]{0,24})""",
        RegexOption.IGNORE_CASE,
    )
    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private const val MONTH_ALT = "jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec"
    private val DATE_YMD = Regex("""(?<!\d)(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})(?!\d)""")
    private val DATE_DMY = Regex("""(?<!\d)(\d{1,2})[-/.](\d{1,2})[-/.](\d{4}|\d{2})(?!\d)""")
    private val DATE_D_MON_Y = Regex("""(?<!\d)(\d{1,2})[\s\-./]*($MONTH_ALT)[a-z]*[\s,\-./]*(\d{4}|\d{2})(?!\d)""", RegexOption.IGNORE_CASE)
    private val DATE_MON_D_Y = Regex("""\b($MONTH_ALT)[a-z]*\.?\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{4})(?!\d)""", RegexOption.IGNORE_CASE)
    private val WORD_VAT = Regex("""\b(?:vat|tax)\b""", RegexOption.IGNORE_CASE)
    private val VENDOR_SKIP = Regex(
        """tax invoice|^invoice|receipt|\btrn\b|\btel\b|phone|mobile|\bdate\b|www\.|@|p\.?o\.? ?box|\bfax\b|cashier|welcome|thank""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(
        rawRows: List<String>,
        ownTrn: String = "",
        defaultVatRate: Double = 5.0,
        today: LocalDate = LocalDate.now(),
    ): ParsedInvoice {
        val rows = rawRows.map { normalizeDigits(it).trim() }.filter { it.isNotEmpty() }
        if (rows.isEmpty()) return ParsedInvoice()

        val trn = findTrn(rows, ownTrn)
        val number = findNumber(rows)
        val day = findDate(rows, today)
        val vendor = findVendor(rows)

        var subtotal: Long? = null
        var total: Long? = null
        var totalRank = 0
        val vatCandidates = ArrayList<Long>()

        for (row in rows) {
            val l = row.lowercase()
            val amounts = amountsIn(row)
            if (amounts.isEmpty()) continue
            val value = amounts.last()
            val inclusive = l.contains("incl") || l.contains("with vat") || l.contains("with tax")
            val isSubtotal = !inclusive && (
                l.contains("subtotal") || l.contains("sub total") || l.contains("sub-total") ||
                    l.contains("excl") || l.contains("before vat") || l.contains("before tax") ||
                    l.contains("taxable") || l.contains("net amount") || l.contains("without vat")
                )
            val isVat = !inclusive && !isSubtotal && WORD_VAT.containsMatchIn(l) &&
                !l.contains("tax invoice") && !l.contains("trn") && !l.contains("registration") &&
                !l.contains("tax id") && !l.contains("tax no")
            val rank = when {
                isSubtotal || isVat -> 0
                l.contains("grand total") || l.contains("amount due") || l.contains("total due") ||
                    l.contains("balance due") || l.contains("net payable") || l.contains("total payable") ||
                    l.contains("amount payable") || l.contains("total amount") || (inclusive && l.contains("total")) -> 3
                l.contains("total") -> 2
                else -> 0
            }
            if (isSubtotal) subtotal = value
            if (isVat) vatCandidates.add(value)
            if (rank > totalRank || (rank == totalRank && rank > 0 && value > (total ?: 0L))) {
                total = value
                totalRank = rank
            }
        }

        // Receipts with no labelled total: fall back to the largest figure printed with two decimals.
        if (total == null && subtotal == null) {
            total = rows.flatMap { priceLikeAmounts(it) }.maxOrNull()
        }

        var vat: Long? = null
        val t = total
        val s = subtotal
        if (t != null && s != null && vatCandidates.contains(t - s)) {
            vat = t - s
        } else if (t != null) {
            vat = vatCandidates.filter { it < t / 2 + 1 }.maxOrNull()
        } else {
            vat = vatCandidates.lastOrNull()
        }

        var estimated = false
        if (t != null && vat != null) {
            subtotal = t - vat
        } else if (t != null && s != null && s <= t) {
            vat = t - s
        } else if (t != null) {
            vat = vatFromGross(t, defaultVatRate)
            subtotal = t - vat
            estimated = true
        } else if (s != null) {
            if (vat == null) {
                vat = vatFromNet(s, defaultVatRate)
                estimated = true
            }
            total = s + vat
        }

        val items = findLines(rows)
        val net = subtotal
        val itemsSum = items.sumOf { it.amount }
        val itemsOk = items.isNotEmpty() && net != null &&
            Math.abs(itemsSum - net) <= maxOf(5L, net / 200)

        return ParsedInvoice(
            vendor = vendor,
            trn = trn,
            number = number,
            day = day,
            subtotal = subtotal,
            vat = vat,
            total = total,
            vatEstimated = estimated,
            lines = if (itemsOk) items else emptyList(),
        )
    }

    private fun toMinor(token: String): Long? = try {
        BigDecimal(token.replace(",", "")).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    } catch (e: Exception) {
        null
    }

    private data class Num(val text: String, val start: Int, val end: Int)

    /** Numeric tokens in a row, leaving out percentages ("5%") and long ID-like digit runs. */
    private fun numbersIn(row: String): List<Num> {
        val out = ArrayList<Num>()
        for (m in NUMBER.findAll(row)) {
            var text = m.value.trimEnd(',')
            if (text.isEmpty()) continue
            val end = m.range.first + text.length
            val after = row.substring(end).trimStart()
            if (after.startsWith("%")) continue
            val digits = text.count { it.isDigit() }
            if (!text.contains('.') && digits >= 9) continue
            // a date such as 12/03/2026 or 2026-03-12 is not an amount
            val before = if (m.range.first > 0) row[m.range.first - 1] else ' '
            val next = if (end < row.length) row[end] else ' '
            if (before == '/' || next == '/') continue
            val before2 = if (m.range.first > 1) row[m.range.first - 2] else ' '
            val next2 = if (end + 1 < row.length) row[end + 1] else ' '
            // 12.03.2026 and 14:35
            if ((before == '.' && before2.isDigit()) || (next == '.' && next2.isDigit())) continue
            if ((before == ':' && before2.isDigit()) || (next == ':' && next2.isDigit())) continue
            if ((before == '-' || next == '-') && !text.contains('.')) continue
            if (text.startsWith("0") && text.length > 1 && !text.startsWith("0.")) text = text.trimStart('0').ifEmpty { "0" }
            out.add(Num(text, m.range.first, end))
        }
        return out
    }

    private fun amountsIn(row: String): List<Long> = numbersIn(row).mapNotNull { toMinor(it.text) }

    private fun priceLikeAmounts(row: String): List<Long> =
        numbersIn(row).filter { Regex(""".*\.\d{2}""").matches(it.text) }.mapNotNull { toMinor(it.text) }

    private fun findTrn(rows: List<String>, ownTrn: String): String {
        val own = ownTrn.filter { it.isDigit() }
        for (row in rows) {
            val compact = DIGIT_GAP.replace(row, "")
            for (m in TRN.findAll(compact)) {
                if (m.value != own) return m.value
            }
        }
        return ""
    }

    private fun findNumber(rows: List<String>): String {
        for (row in rows) {
            for (m in INVOICE_NO.findAll(row)) {
                val candidate = m.groupValues[1].trimEnd('-', '/')
                if (candidate.isEmpty()) continue
                if (DATE_DMY.containsMatchIn(candidate) || DATE_YMD.containsMatchIn(candidate)) continue
                if (candidate.startsWith("100") && candidate.length == 15) continue
                return candidate
            }
        }
        return ""
    }

    private fun findDate(rows: List<String>, today: LocalDate): Long? {
        val preferred = rows.filter { it.lowercase().contains("date") && !it.lowercase().contains("due") }
        for (row in preferred + rows) {
            val d = dateIn(row, today)
            if (d != null) return d.toEpochDay()
        }
        return null
    }

    private fun dateIn(row: String, today: LocalDate): LocalDate? {
        DATE_YMD.find(row)?.let { m ->
            make(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), today)?.let { return it }
        }
        DATE_DMY.find(row)?.let { m ->
            var d = m.groupValues[1].toInt()
            var mo = m.groupValues[2].toInt()
            // the UAE writes day first; only swap when that cannot be right
            if (mo > 12 && d <= 12) {
                val tmp = d; d = mo; mo = tmp
            }
            make(year(m.groupValues[3]), mo, d, today)?.let { return it }
        }
        DATE_D_MON_Y.find(row)?.let { m ->
            val mo = MONTHS.indexOf(m.groupValues[2].lowercase()) + 1
            make(year(m.groupValues[3]), mo, m.groupValues[1].toInt(), today)?.let { return it }
        }
        DATE_MON_D_Y.find(row)?.let { m ->
            val mo = MONTHS.indexOf(m.groupValues[1].lowercase()) + 1
            make(m.groupValues[3].toInt(), mo, m.groupValues[2].toInt(), today)?.let { return it }
        }
        return null
    }

    private fun year(text: String): Int = if (text.length == 2) 2000 + text.toInt() else text.toInt()

    private fun make(y: Int, m: Int, d: Int, today: LocalDate): LocalDate? {
        if (y < 2000 || y > today.year + 1) return null
        return try {
            LocalDate.of(y, m, d)
        } catch (e: Exception) {
            null
        }
    }

    private fun findVendor(rows: List<String>): String {
        for (row in rows.take(8)) {
            val letters = row.count { it.isLetter() }
            val digits = row.count { it.isDigit() }
            if (letters < 3 || digits > letters) continue
            if (VENDOR_SKIP.containsMatchIn(row)) continue
            return row.replace(Regex("""\s{2,}"""), " ").trim().take(60)
        }
        return ""
    }

    /**
     * Item rows are only accepted when the arithmetic proves them: some quantity times some unit
     * price on the row equals an amount on the same row. Anything else is left for manual entry.
     */
    private fun findLines(rows: List<String>): List<ParsedLine> {
        val out = ArrayList<ParsedLine>()
        for (row in rows) {
            val l = row.lowercase()
            if (l.contains("total")) continue
            val nums = numbersIn(row)
            if (nums.size < 3) continue
            val values = nums.map { it.text.replace(",", "").toDoubleOrNull() ?: -1.0 }
            var found: ParsedLine? = null
            search@ for (k in nums.indices.reversed()) {
                val amount = values[k]
                if (amount <= 0.0) continue
                for (i in 0 until k) {
                    for (j in i + 1 until k) {
                        val a = values[i]
                        val b = values[j]
                        if (a <= 0.0 || b <= 0.0) continue
                        if (Math.abs(a * b - amount) > maxOf(0.011, amount * 0.002)) continue
                        val description = row.substring(0, nums[i].start)
                            .replace(Regex("""^\s*\d{1,3}[.)\s]+"""), "")
                            .trim().trimEnd(':', '-', 'x', 'X', '@').trim()
                        if (description.count { it.isLetter() } < 2) continue
                        // the quantity is the whole-number factor when only one of the two is whole
                        val aWhole = a == Math.floor(a)
                        val bWhole = b == Math.floor(b)
                        val qty = if (bWhole && !aWhole) b else a
                        val price = if (qty == a) b else a
                        found = ParsedLine(
                            description = description.take(80),
                            qty = qty,
                            unitPrice = Math.round(price * 100.0),
                            amount = Math.round(amount * 100.0),
                        )
                        break@search
                    }
                }
            }
            if (found != null) out.add(found)
        }
        return out
    }
}
