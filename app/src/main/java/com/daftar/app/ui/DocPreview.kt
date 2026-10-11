package com.daftar.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftar.app.R
import com.daftar.app.data.Doc
import com.daftar.app.data.DocKind
import com.daftar.app.data.DocStatus
import com.daftar.app.io.PdfExporter
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shows an invoice or quotation exactly as the customer will receive it (the real PDF pages),
 * with the things you do next: share it, mark it paid, or edit it.
 */
@Composable
fun DocPreviewScreen(vm: AppViewModel, go: Go, id: Long) {
    val business by vm.business.collectAsState()
    val docs by vm.docs.collectAsState()
    val b = business ?: return
    val context = LocalContext.current
    val money = LocalMoneyColors.current

    // the copy in the live list changes when the document is edited or its status changes
    val listed = docs.firstOrNull { it.id == id }
    var doc by remember { mutableStateOf<Doc?>(null) }
    var file by remember { mutableStateOf<File?>(null) }
    var pages by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(listed, b) {
        val stored = vm.loadDoc(id)
        if (stored == null) {
            go.backNow()
            return@LaunchedEffect
        }
        doc = stored.first
        val pdf: File? = withContext(Dispatchers.IO) {
            try {
                PdfExporter.export(context, b, stored.first, stored.second)
            } catch (e: Throwable) {
                null
            }
        }
        if (pdf != null) {
            val images: List<ImageBitmap> = withContext(Dispatchers.IO) {
                try {
                    PdfExporter.render(pdf).map { it.asImageBitmap() }
                } catch (e: Throwable) {
                    emptyList()
                }
            }
            file = pdf
            pages = images
        }
    }

    val current = doc
    val today = today()
    AppScreen(
        title = current?.number ?: "",
        onBack = { go.back() },
        actions = {
            if (current != null) {
                IconButton(onClick = { go.to("doc/${current.kind}/${current.id}") }) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (pages.isEmpty()) {
                    Box(Modifier.fillMaxWidth().aspectRatio(595f / 842f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }
                pages.forEachIndexed { index, page ->
                    // paper stays white in dark mode too: it is a picture of the real document
                    Surface(shape = RoundedCornerShape(6.dp), color = Color.White, shadowElevation = 8.dp) {
                        Box {
                            Image(
                                bitmap = page,
                                contentDescription = stringResource(R.string.document_preview),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (index == 0 && current != null) {
                                val overdue = current.status == DocStatus.UNPAID && current.dueDay != null && current.dueDay < today
                                val color = when {
                                    overdue -> money.negative
                                    current.status == DocStatus.PAID || current.status == DocStatus.ACCEPTED -> Color(0xFF17835A)
                                    current.status == DocStatus.DECLINED -> Color(0xFFC8372D)
                                    else -> Color(0xFF8A6200)
                                }
                                Stamp(
                                    text = if (overdue) stringResource(R.string.status_overdue) else statusLabel(current.status),
                                    color = if (overdue) Color(0xFFC8372D) else color,
                                    modifier = Modifier.align(BiasAlignment(-0.62f, 0.42f)),
                                )
                            }
                        }
                    }
                }
            }

            if (current != null) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val unpaidInvoice = current.kind == DocKind.INVOICE && current.status == DocStatus.UNPAID
                        val openQuote = current.kind == DocKind.QUOTATION && current.status != DocStatus.ACCEPTED
                        val nextLabel: Int? = when {
                            unpaidInvoice -> R.string.mark_paid
                            current.kind == DocKind.INVOICE -> R.string.mark_unpaid
                            openQuote -> R.string.convert_to_invoice
                            else -> null
                        }
                        if (nextLabel != null) {
                            OutlinedButton(
                                onClick = {
                                    if (openQuote) {
                                        if (!busy) {
                                            busy = true
                                            vm.convertToInvoice(id) { invoiceId -> go.replace("preview/$invoiceId") }
                                        }
                                    } else {
                                        vm.setDocStatus(id, if (unpaidInvoice) DocStatus.PAID else DocStatus.UNPAID)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(stringResource(nextLabel), maxLines = 1)
                            }
                        }
                        Button(
                            onClick = {
                                val pdf = file
                                if (pdf != null) {
                                    try {
                                        PdfExporter.share(context, pdf)
                                    } catch (e: Exception) {
                                        // nothing installed that can receive a PDF
                                    }
                                }
                            },
                            enabled = file != null,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null)
                            Text(stringResource(R.string.share_pdf), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

/** A rubber stamp, slightly crooked, the way it lands on a paper invoice. Shown in the app only, never in the PDF. */
@Composable
private fun Stamp(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(Locale.getDefault()),
        color = color.copy(alpha = 0.82f),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        maxLines = 1,
        modifier = modifier
            .rotate(-11f)
            .border(2.5.dp, color.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}
