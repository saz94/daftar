package com.daftar.app.io

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.daftar.app.R
import com.daftar.app.data.Business
import com.daftar.app.data.Doc
import com.daftar.app.data.DocKind
import com.daftar.app.data.DocLine
import com.daftar.app.domain.Money
import com.daftar.app.domain.lineNet
import com.daftar.app.domain.trimNumber
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Draws an A4 invoice or quotation. Every horizontal position is measured from the "start" edge,
 * so the same code produces a left-to-right page in English and a mirrored page in Arabic.
 */
object PdfExporter {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN

    private const val INK = 0xFF141A2E.toInt()
    private const val MUTED = 0xFF5B6480.toInt()
    /** Same order as BusinessColors in ui/Theme.kt: the invoice takes the business's colour. */
    private val ACCENTS = intArrayOf(
        0xFF2340B8.toInt(), 0xFF17835A.toInt(), 0xFF8B2F5C.toInt(),
        0xFFB26A00.toInt(), 0xFF0F7C8C.toInt(), 0xFF3A4466.toInt(),
    )
    private var regularFace: Typeface = Typeface.DEFAULT
    private var boldFace: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private const val RULE = 0xFFDDE2EE.toInt()
    private const val BAND = 0xFFF2F4F9.toInt()

    private const val START = 0
    private const val END = 1

    private class Sheet(val pdf: PdfDocument, val rtl: Boolean) {
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = MARGIN
        val canvas: Canvas get() = page!!.canvas

        fun newPage() {
            page?.let { pdf.finishPage(it) }
            pageNo++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            y = MARGIN
        }

        fun finish() {
            page?.let { pdf.finishPage(it) }
            page = null
        }

        fun layout(text: String, width: Float, paint: TextPaint, align: Int): StaticLayout {
            val alignment = if (align == END) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
                .setAlignment(alignment)
                .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                .setIncludePad(false)
                .build()
        }

        /** Draws text in a box that begins [startX] from the start edge. Returns the height used. */
        fun text(text: String, startX: Float, width: Float, paint: TextPaint, top: Float, align: Int = START): Float {
            if (text.isEmpty()) return 0f
            val l = layout(text, width, paint, align)
            val left = if (rtl) PAGE_W - startX - width else startX
            canvas.save()
            canvas.translate(left, top)
            l.draw(canvas)
            canvas.restore()
            return l.height.toFloat()
        }

        fun rect(startX: Float, top: Float, width: Float, height: Float, paint: Paint) {
            val left = if (rtl) PAGE_W - startX - width else startX
            canvas.drawRect(left, top, left + width, top + height, paint)
        }

        fun image(bitmap: Bitmap, startX: Float, top: Float, width: Float, height: Float) {
            val left = if (rtl) PAGE_W - startX - width else startX
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + width, top + height), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
    }

    private fun paint(size: Float, color: Int = INK, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) boldFace else regularFace
    }

    private fun fill(color: Int) = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun date(day: Long): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

    fun export(context: Context, business: Business, doc: Doc, lines: List<DocLine>): File {
        val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        // the app's own typeface, so the document matches what is on screen
        try {
            regularFace = ResourcesCompat.getFont(context, R.font.plex_regular) ?: Typeface.DEFAULT
            boldFace = ResourcesCompat.getFont(context, R.font.plex_semibold) ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } catch (e: Exception) {
            // keep the system typeface
        }
        val accent = ACCENTS[Math.floorMod(business.colorIndex, ACCENTS.size)]
        val pdf = PdfDocument()
        val sheet = Sheet(pdf, rtl)
        sheet.newPage()

        val title = when {
            doc.kind == DocKind.QUOTATION -> context.getString(R.string.pdf_quotation)
            business.trn.isNotBlank() -> context.getString(R.string.pdf_tax_invoice)
            else -> context.getString(R.string.pdf_invoice)
        }
        val half = CONTENT_W / 2 - 10f

        // ---- header: seller on the start side, document title on the end side
        var leftY = MARGIN
        // optional logo above the business name, fitted into a 150 x 54 pt box
        val logo = business.logoPath?.let { path ->
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Throwable) {
                null
            }
        }
        if (logo != null && logo.width > 0 && logo.height > 0) {
            val scale = minOf(150f / logo.width, 54f / logo.height)
            val w = logo.width * scale
            val h = logo.height * scale
            sheet.image(logo, MARGIN, leftY, w, h)
            leftY += h + 10f
        }
        leftY += sheet.text(business.name, MARGIN, half, paint(18f, INK, true), leftY) + 4f
        val small = paint(9.5f, MUTED)
        if (business.address.isNotBlank()) leftY += sheet.text(business.address, MARGIN, half, small, leftY) + 2f
        val contact = listOf(business.phone, business.email).filter { it.isNotBlank() }.joinToString("  ·  ")
        if (contact.isNotBlank()) leftY += sheet.text(contact, MARGIN, half, small, leftY) + 2f
        if (business.trn.isNotBlank()) {
            leftY += sheet.text(context.getString(R.string.pdf_trn, business.trn), MARGIN, half, paint(9.5f, INK), leftY) + 2f
        }

        val endX = MARGIN + CONTENT_W - half
        var rightY = MARGIN
        rightY += sheet.text(title, endX, half, paint(20f, accent, true), rightY, END) + 6f
        rightY += sheet.text(doc.number, endX, half, paint(11f, INK, true), rightY, END) + 3f
        rightY += sheet.text(context.getString(R.string.pdf_date, date(doc.day)), endX, half, small, rightY, END) + 2f
        doc.dueDay?.let { due ->
            val label = if (doc.kind == DocKind.QUOTATION) R.string.pdf_valid_until else R.string.pdf_due
            rightY += sheet.text(context.getString(label, date(due)), endX, half, small, rightY, END) + 2f
        }

        sheet.y = maxOf(leftY, rightY) + 14f
        sheet.rect(MARGIN, sheet.y, CONTENT_W, 2f, fill(accent))
        sheet.y += 16f

        // ---- customer
        sheet.y += sheet.text(context.getString(R.string.pdf_bill_to), MARGIN, CONTENT_W, paint(8.5f, MUTED, true), sheet.y) + 4f
        sheet.y += sheet.text(doc.partyName.ifBlank { "—" }, MARGIN, CONTENT_W, paint(12f, INK, true), sheet.y) + 2f
        if (doc.partyAddress.isNotBlank()) sheet.y += sheet.text(doc.partyAddress, MARGIN, CONTENT_W, small, sheet.y) + 2f
        if (doc.partyTrn.isNotBlank()) {
            sheet.y += sheet.text(context.getString(R.string.pdf_trn, doc.partyTrn), MARGIN, CONTENT_W, small, sheet.y) + 2f
        }
        sheet.y += 16f

        // ---- items table. Column widths add up to CONTENT_W (515).
        val wDesc = 215f
        val wQty = 55f
        val wPrice = 85f
        val wVat = 55f
        val wAmount = 105f
        val xDesc = MARGIN
        val xQty = xDesc + wDesc
        val xPrice = xQty + wQty
        val xVat = xPrice + wPrice
        val xAmount = xVat + wVat
        val pad = 6f

        fun tableHeader() {
            val head = paint(8.5f, MUTED, true)
            sheet.rect(MARGIN, sheet.y, CONTENT_W, 22f, fill(BAND))
            val ty = sheet.y + 6f
            sheet.text(context.getString(R.string.pdf_col_description), xDesc + pad, wDesc - 2 * pad, head, ty)
            sheet.text(context.getString(R.string.pdf_col_qty), xQty, wQty - pad, head, ty, END)
            sheet.text(context.getString(R.string.pdf_col_price), xPrice, wPrice - pad, head, ty, END)
            sheet.text(context.getString(R.string.pdf_col_vat), xVat, wVat - pad, head, ty, END)
            sheet.text(context.getString(R.string.pdf_col_amount), xAmount, wAmount - pad, head, ty, END)
            sheet.y += 22f
        }
        tableHeader()

        val cell = paint(10f, INK)
        val rule = fill(RULE)
        for (line in lines) {
            val descHeight = sheet.layout(line.description, wDesc - 2 * pad, cell, START).height.toFloat()
            val rowHeight = maxOf(descHeight, 12f) + 14f
            if (sheet.y + rowHeight > PAGE_H - MARGIN - 30f) {
                sheet.newPage()
                tableHeader()
            }
            val ty = sheet.y + 7f
            sheet.text(line.description, xDesc + pad, wDesc - 2 * pad, cell, ty)
            sheet.text(trimNumber(line.qty), xQty, wQty - pad, cell, ty, END)
            sheet.text(Money.format(line.unitPrice), xPrice, wPrice - pad, cell, ty, END)
            sheet.text(trimNumber(line.vatRate) + "%", xVat, wVat - pad, cell, ty, END)
            sheet.text(Money.format(lineNet(line.qty, line.unitPrice)), xAmount, wAmount - pad, cell, ty, END)
            sheet.y += rowHeight
            sheet.rect(MARGIN, sheet.y, CONTENT_W, 0.7f, rule)
        }
        sheet.y += 14f

        // ---- totals, on the end side
        val boxW = 230f
        val boxX = MARGIN + CONTENT_W - boxW
        val rows = ArrayList<Pair<String, String>>()
        rows.add(context.getString(R.string.subtotal) to Money.format(doc.subtotal))
        if (doc.discount > 0) rows.add(context.getString(R.string.discount) to "\u200E-" + Money.format(doc.discount))
        rows.add(context.getString(R.string.vat) to Money.format(doc.vat))
        if (sheet.y + rows.size * 18f + 60f > PAGE_H - MARGIN) sheet.newPage()
        val label = paint(10f, MUTED)
        for ((name, value) in rows) {
            sheet.text(name, boxX, boxW / 2, label, sheet.y)
            sheet.text(value, boxX + boxW / 2, boxW / 2 - pad, cell, sheet.y, END)
            sheet.y += 18f
        }
        sheet.y += 2f
        sheet.rect(boxX, sheet.y, boxW, 30f, fill(accent))
        val white = paint(11f, 0xFFFFFFFF.toInt(), true)
        sheet.text(context.getString(R.string.pdf_total, business.currency), boxX + pad + 4f, boxW / 2, white, sheet.y + 9f)
        sheet.text(Money.format(doc.total), boxX + boxW / 2, boxW / 2 - pad - 4f, white, sheet.y + 9f, END)
        sheet.y += 46f

        // ---- notes
        if (doc.notes.isNotBlank()) {
            val notes = paint(9.5f, INK)
            val height = sheet.layout(doc.notes, CONTENT_W, notes, START).height
            if (sheet.y + height + 20f > PAGE_H - MARGIN) sheet.newPage()
            sheet.y += sheet.text(context.getString(R.string.notes), MARGIN, CONTENT_W, paint(8.5f, MUTED, true), sheet.y) + 4f
            sheet.y += sheet.text(doc.notes, MARGIN, CONTENT_W, notes, sheet.y)
        }

        sheet.finish()

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = doc.number.replace(Regex("[^A-Za-z0-9_\\-]"), "_").ifBlank { "document" }
        val file = File(dir, "$safeName.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    /** Turns the pages of an exported PDF into pictures for the on-screen preview. */
    fun render(file: File, scale: Int = 2): List<Bitmap> {
        val pages = ArrayList<Bitmap>()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            try {
                for (index in 0 until renderer.pageCount) {
                    val page = renderer.openPage(index)
                    val bitmap = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(0xFFFFFFFF.toInt())
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    pages.add(bitmap)
                }
            } finally {
                renderer.close()
            }
        }
        return pages
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_pdf)))
    }
}
