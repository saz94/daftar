package com.daftar.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * All amounts are stored as whole minor units (fils for AED) in a Long, so sums never drift
 * the way floating point money does.
 */
object Money {
    /** "1,234.5" -> 123450. Accepts Arabic-Indic digits. Returns null when the text is not a number. */
    fun parse(text: String): Long? {
        val clean = normalizeDigits(text).replace(",", "").replace(" ", "").trim()
        if (clean.isEmpty()) return null
        return try {
            BigDecimal(clean).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    /** 123450 -> "1,234.50" (always Western digits so figures read the same in both languages). */
    fun format(minor: Long): String = String.format(Locale.ENGLISH, "%,.2f", minor / 100.0)

    /** Text to pre-fill an input with: 123450 -> "1234.50", 0 -> "". */
    fun input(minor: Long): String =
        if (minor == 0L) "" else String.format(Locale.ENGLISH, "%.2f", minor / 100.0)
}

fun parseDecimal(text: String): Double? =
    normalizeDigits(text).replace(",", "").trim().toDoubleOrNull()

/** 5.0 -> "5", 2.5 -> "2.5" */
fun trimNumber(value: Double): String {
    val rounded = Math.round(value * 1000.0) / 1000.0
    return if (rounded == Math.floor(rounded) && Math.abs(rounded) < 1e15) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}

fun normalizeDigits(s: String): String {
    val sb = StringBuilder(s.length)
    for (c in s) {
        when (c) {
            in '٠'..'٩' -> sb.append('0' + (c - '٠'))
            in '۰'..'۹' -> sb.append('0' + (c - '۰'))
            '٫' -> sb.append('.')
            '٬' -> sb.append(',')
            else -> sb.append(c)
        }
    }
    return sb.toString()
}

data class LineCalc(val qty: Double, val unitPrice: Long, val vatRate: Double)

data class Totals(val subtotal: Long, val discount: Long, val vat: Long) {
    /** Amount excluding VAT, after discount. */
    val net: Long get() = subtotal - discount
    val total: Long get() = net + vat
}

fun lineNet(qty: Double, unitPrice: Long): Long = Math.round(qty * unitPrice)

fun vatFromNet(net: Long, ratePercent: Double): Long = Math.round(net * ratePercent / 100.0)

/** VAT contained in a VAT-inclusive amount. */
fun vatFromGross(gross: Long, ratePercent: Double): Long =
    Math.round(gross * ratePercent / (100.0 + ratePercent))

/**
 * Document totals. The discount is a fixed amount taken off before VAT and is spread across the
 * lines in proportion, so mixed VAT rates stay correct.
 */
fun computeTotals(lines: List<LineCalc>, discount: Long): Totals {
    val nets = lines.map { lineNet(it.qty, it.unitPrice) }
    val subtotal = nets.sum()
    val d = discount.coerceIn(0L, maxOf(subtotal, 0L))
    val keep = if (subtotal > 0L) (subtotal - d).toDouble() / subtotal else 1.0
    var vat = 0L
    for (i in lines.indices) {
        vat += Math.round(nets[i] * keep * lines[i].vatRate / 100.0)
    }
    return Totals(subtotal, d, vat)
}

/** UAE corporate tax: 9% on taxable profit above AED 375,000 per year. Estimate only. */
const val CORPORATE_TAX_THRESHOLD_MINOR = 375_000_00L
const val CORPORATE_TAX_RATE = 9.0

fun corporateTaxEstimate(yearProfit: Long): Long {
    val taxable = yearProfit - CORPORATE_TAX_THRESHOLD_MINOR
    return if (taxable <= 0L) 0L else Math.round(taxable * CORPORATE_TAX_RATE / 100.0)
}

enum class PeriodType { MONTH, QUARTER, YEAR, ALL }

/** A reporting window relative to today: offset 0 is the current month/quarter/year, -1 the previous one. */
data class Period(val type: PeriodType = PeriodType.MONTH, val offset: Int = 0) {

    private fun start(today: LocalDate): LocalDate = when (type) {
        PeriodType.MONTH -> today.withDayOfMonth(1).plusMonths(offset.toLong())
        PeriodType.QUARTER -> LocalDate.of(today.year, ((today.monthValue - 1) / 3) * 3 + 1, 1)
            .plusMonths(3L * offset)
        PeriodType.YEAR -> LocalDate.of(today.year + offset, 1, 1)
        PeriodType.ALL -> LocalDate.of(1970, 1, 1)
    }

    /** Inclusive range of epoch days, or null for "all time". */
    fun range(today: LocalDate = LocalDate.now()): LongRange? {
        val s = start(today)
        val end = when (type) {
            PeriodType.MONTH -> s.plusMonths(1)
            PeriodType.QUARTER -> s.plusMonths(3)
            PeriodType.YEAR -> s.plusYears(1)
            PeriodType.ALL -> return null
        }.minusDays(1)
        return s.toEpochDay()..end.toEpochDay()
    }

    fun label(today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
        val s = start(today)
        return when (type) {
            PeriodType.MONTH -> s.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            PeriodType.QUARTER -> "Q" + ((s.monthValue - 1) / 3 + 1) + " " + s.year
            PeriodType.YEAR -> s.year.toString()
            PeriodType.ALL -> ""
        }
    }
}
