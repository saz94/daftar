package com.daftar.app

import com.daftar.app.domain.InvoiceParser
import com.daftar.app.domain.LineCalc
import com.daftar.app.domain.Money
import com.daftar.app.domain.OcrLine
import com.daftar.app.domain.Period
import com.daftar.app.domain.PeriodType
import com.daftar.app.domain.computeTotals
import com.daftar.app.domain.corporateTaxEstimate
import com.daftar.app.domain.groupRows
import com.daftar.app.domain.trimNumber
import com.daftar.app.domain.vatFromGross
import com.daftar.app.domain.vatFromNet
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    private val today = LocalDate.of(2026, 10, 11)

    @Test fun moneyParsesAndFormats() {
        assertEquals(123450L, Money.parse("1,234.5"))
        assertEquals(123456L, Money.parse("1234.555"))
        assertEquals(500L, Money.parse("٥"))
        assertEquals(1250L, Money.parse("١٢٫٥"))
        assertEquals(null, Money.parse("abc"))
        assertEquals(null, Money.parse(""))
        assertEquals("1,234.50", Money.format(123450L))
        assertEquals("-0.05", Money.format(-5L))
        assertEquals("1234.50", Money.input(123450L))
        assertEquals("", Money.input(0L))
        assertEquals("5", trimNumber(5.0))
        assertEquals("2.5", trimNumber(2.5))
    }

    @Test fun vatAtFivePercent() {
        assertEquals(500L, vatFromNet(10000L, 5.0))
        assertEquals(500L, vatFromGross(10500L, 5.0))
        assertEquals(476L, vatFromGross(10000L, 5.0))
        assertEquals(0L, vatFromNet(10000L, 0.0))
    }

    @Test fun totalsWithMixedRatesAndDiscount() {
        val t = computeTotals(
            listOf(LineCalc(2.0, 5000L, 5.0), LineCalc(1.0, 10000L, 0.0), LineCalc(1.5, 2000L, 5.0)),
            discount = 0L,
        )
        assertEquals(23000L, t.subtotal)
        assertEquals(650L, t.vat)
        assertEquals(23650L, t.total)

        val d = computeTotals(listOf(LineCalc(1.0, 10000L, 5.0)), discount = 1000L)
        assertEquals(9000L, d.net)
        assertEquals(450L, d.vat)
        assertEquals(9450L, d.total)

        val capped = computeTotals(listOf(LineCalc(1.0, 10000L, 5.0)), discount = 99999L)
        assertEquals(0L, capped.total)
        assertEquals(0L, computeTotals(emptyList(), 0L).total)
    }

    @Test fun corporateTaxOnlyAboveThreshold() {
        assertEquals(0L, corporateTaxEstimate(375_000_00L))
        assertEquals(0L, corporateTaxEstimate(-5_000_00L))
        assertEquals(9_000_00L, corporateTaxEstimate(475_000_00L))
    }

    @Test fun periods() {
        val month = Period(PeriodType.MONTH, 0).range(today)!!
        assertEquals(LocalDate.of(2026, 10, 1).toEpochDay(), month.first)
        assertEquals(LocalDate.of(2026, 10, 31).toEpochDay(), month.last)
        val prevQuarter = Period(PeriodType.QUARTER, -1).range(today)!!
        assertEquals(LocalDate.of(2026, 7, 1).toEpochDay(), prevQuarter.first)
        assertEquals(LocalDate.of(2026, 9, 30).toEpochDay(), prevQuarter.last)
        val feb = Period(PeriodType.MONTH, -8).range(today)!!
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), feb.last)
        val year = Period(PeriodType.YEAR, -1).range(today)!!
        assertEquals(LocalDate.of(2025, 12, 31).toEpochDay(), year.last)
        assertEquals(null, Period(PeriodType.ALL, 0).range(today))
        assertEquals("Q3 2026", Period(PeriodType.QUARTER, -1).label(today))
    }

    @Test fun rowsAreStitchedAcrossThePage() {
        val rows = groupRows(
            listOf(
                OcrLine("105.00", 800, 502, 900, 530),
                OcrLine("Al Noor Trading LLC", 100, 40, 600, 80),
                OcrLine("Grand Total", 100, 500, 300, 530),
                OcrLine("VAT 5%", 100, 450, 220, 478),
                OcrLine("5.00", 820, 452, 900, 480),
            ),
        )
        assertEquals(listOf("Al Noor Trading LLC", "VAT 5%   5.00", "Grand Total   105.00"), rows)
    }

    @Test fun parsesAFullTaxInvoice() {
        val p = InvoiceParser.parse(
            listOf(
                "TAX INVOICE",
                "Al Noor Trading LLC",
                "Shop 4, Al Quoz 3, Dubai   Tel: 04 338 1234",
                "TRN: 100 2345 6789 0003",
                "Invoice No: INV-2026-0142     Date: 03/09/2026",
                "Bill To: Suhail Apparel   TRN 100111222333444",
                "Description   Qty   Unit Price   VAT   Amount",
                "1  Cotton fabric roll   4   250.00   5%   1,000.00",
                "2  Thread spool (box)   10   12.50   5%   125.00",
                "Subtotal   1,125.00",
                "VAT 5%   56.25",
                "Grand Total AED   1,181.25",
            ),
            ownTrn = "100111222333444",
            today = today,
        )
        assertEquals("Al Noor Trading LLC", p.vendor)
        assertEquals("100234567890003", p.trn)
        assertEquals("INV-2026-0142", p.number)
        assertEquals(LocalDate.of(2026, 9, 3).toEpochDay(), p.day)
        assertEquals(112500L, p.subtotal)
        assertEquals(5625L, p.vat)
        assertEquals(118125L, p.total)
        assertEquals(false, p.vatEstimated)
        assertEquals(2, p.lines.size)
        assertEquals("Cotton fabric roll", p.lines[0].description)
        assertEquals(4.0, p.lines[0].qty, 0.0001)
        assertEquals(25000L, p.lines[0].unitPrice)
        assertEquals(10.0, p.lines[1].qty, 0.0001)
        assertEquals(1250L, p.lines[1].unitPrice)
    }

    @Test fun parsesATillReceiptWithOnlyATotal() {
        val p = InvoiceParser.parse(
            listOf(
                "CITY STATIONERY",
                "TRN 100987654321009",
                "Receipt # 88213",
                "12 Oct 2025  14:35",
                "A4 paper   2 x 18.00   36.00",
                "Stapler   27.00",
                "TOTAL   63.00",
                "Cash   100.00",
                "Change   37.00",
            ),
            today = today,
        )
        assertEquals("CITY STATIONERY", p.vendor)
        assertEquals("100987654321009", p.trn)
        assertEquals("88213", p.number)
        assertEquals(LocalDate.of(2025, 10, 12).toEpochDay(), p.day)
        assertEquals(6300L, p.total)
        assertEquals(300L, p.vat)
        assertEquals(6000L, p.subtotal)
        assertTrue(p.vatEstimated)
        // items do not add up to the net amount, so they are left for manual entry
        assertEquals(0, p.lines.size)
    }

    @Test fun parsesInclusiveTotalsAndOtherDateStyles() {
        val p = InvoiceParser.parse(
            listOf(
                "Desert Print House FZE",
                "Tax Invoice Number 7781",
                "Invoice Date: 2026-01-15",
                "Due Date: 2026-02-14",
                "Total Excl. VAT   2,000.00",
                "Total VAT   100.00",
                "Total Incl. VAT   2,100.00",
            ),
            today = today,
        )
        assertEquals("Desert Print House FZE", p.vendor)
        assertEquals("7781", p.number)
        assertEquals(LocalDate.of(2026, 1, 15).toEpochDay(), p.day)
        assertEquals(200000L, p.subtotal)
        assertEquals(10000L, p.vat)
        assertEquals(210000L, p.total)

        val q = InvoiceParser.parse(listOf("Some Shop", "March 5, 2026", "Amount Due 52.50"), today = today)
        assertEquals(LocalDate.of(2026, 3, 5).toEpochDay(), q.day)
        assertEquals(5250L, q.total)

        val dots = InvoiceParser.parse(listOf("Some Shop", "Date 25.12.2025", "Net 10.00"), today = today)
        assertEquals(LocalDate.of(2025, 12, 25).toEpochDay(), dots.day)
        assertEquals(1000L, dots.total)

        val empty = InvoiceParser.parse(emptyList(), today = today)
        assertEquals(false, empty.foundAnything)
    }
}
