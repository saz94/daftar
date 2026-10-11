package com.daftar.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.daftar.app.R
import com.daftar.app.data.Business
import com.daftar.app.data.Doc
import com.daftar.app.data.DocKind
import com.daftar.app.data.DocStatus
import com.daftar.app.data.Txn
import com.daftar.app.data.TxnType
import com.daftar.app.domain.Money
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BusinessAvatar(business: Business, size: Int = 36) {
    val color = BusinessColors[Math.floorMod(business.colorIndex, BusinessColors.size)]
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            business.name.trim().take(1).uppercase(Locale.getDefault()),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, go: Go) {
    val business by vm.business.collectAsState()
    val businesses by vm.businesses.collectAsState()
    val docs by vm.docs.collectAsState()
    val txns by vm.txns.collectAsState()
    val loans by vm.loans.collectAsState()
    val payments by vm.loanPayments.collectAsState()
    val shareholders by vm.shareholders.collectAsState()
    val b = business ?: return

    var switcherOpen by remember { mutableStateOf(false) }
    var scanOpen by remember { mutableStateOf(false) }
    val scan = rememberScanActions(vm) { go.toNow("doc/${DocKind.PURCHASE}/0") }
    val money = LocalMoneyColors.current

    val range = vm.period.range()
    val summary = remember(docs, txns, range) { summarize(docs, txns, range) }
    val receivable = remember(docs) {
        docs.filter { it.kind == DocKind.INVOICE && it.status == DocStatus.UNPAID }.sumOf { it.total }
    }
    val loanBalance = remember(loans, payments) { loans.sumOf { it.principal } - payments.sumOf { it.amount } }
    val capital = remember(shareholders) { shareholders.sumOf { it.invested } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClickLabel = stringResource(R.string.switch_business)) { switcherOpen = true }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BusinessAvatar(b)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            b.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = { BottomTabs("home", go) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeriodBar(vm.period, onChange = { vm.period = it })

            // ---- headline: profit for the period
            // solid green on paper in light mode; a quieter deep green panel in dark mode
            val dark = isSystemInDarkTheme()
            val heroBg = if (dark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
            val heroFg = if (dark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
            Surface(shape = MaterialTheme.shapes.large, color = heroBg) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        stringResource(R.string.net_profit),
                        style = MaterialTheme.typography.labelLarge,
                        color = heroFg.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            Money.format(summary.profit),
                            style = MaterialTheme.typography.displaySmall.tabular(),
                            color = heroFg,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            b.currency,
                            style = MaterialTheme.typography.titleSmall,
                            color = heroFg.copy(alpha = 0.75f),
                            modifier = Modifier.padding(bottom = 7.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        HeroFigure(stringResource(R.string.income), summary.income, heroFg, Modifier.weight(1f))
                        HeroFigure(stringResource(R.string.expenses), summary.expenses, heroFg, Modifier.weight(1f))
                    }
                }
            }

            // ---- quick actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction(Icons.AutoMirrored.Outlined.ReceiptLong, stringResource(R.string.action_invoice), Modifier.weight(1f)) {
                    go.to("doc/${DocKind.INVOICE}/0")
                }
                QuickAction(Icons.Outlined.RequestQuote, stringResource(R.string.action_quote), Modifier.weight(1f)) {
                    go.to("doc/${DocKind.QUOTATION}/0")
                }
                QuickAction(Icons.Outlined.CameraAlt, stringResource(R.string.action_scan), Modifier.weight(1f)) {
                    scanOpen = true
                }
                QuickAction(Icons.Outlined.ShoppingBag, stringResource(R.string.action_expense), Modifier.weight(1f)) {
                    go.to("txn/${TxnType.EXPENSE}/0")
                }
            }

            // ---- six months at a glance
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.last_six_months), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Legend(money.positive, stringResource(R.string.income))
                    Spacer(Modifier.width(12.dp))
                    Legend(money.negative, stringResource(R.string.expenses))
                }
                Spacer(Modifier.height(16.dp))
                MonthlyBars(docs, txns)
            }

            // ---- what is owed, in both directions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.Outlined.Percent,
                    label = stringResource(if (summary.vatDue >= 0) R.string.vat_payable else R.string.vat_refundable),
                    minor = Math.abs(summary.vatDue),
                    tint = money.due,
                    modifier = Modifier.weight(1f),
                    onClick = { go.to("tax") },
                )
                StatTile(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.unpaid_invoices),
                    minor = receivable,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        vm.docsTab = 0
                        go.tab("docs")
                    },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.Outlined.AccountBalance,
                    label = stringResource(R.string.loans_outstanding),
                    minor = loanBalance,
                    tint = money.negative,
                    modifier = Modifier.weight(1f),
                    onClick = { go.to("loans") },
                )
                StatTile(
                    icon = Icons.Outlined.Groups,
                    label = stringResource(R.string.capital_invested),
                    minor = capital,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { go.to("shareholders") },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (switcherOpen) {
        ModalBottomSheet(onDismissRequest = { switcherOpen = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                Text(
                    stringResource(R.string.your_businesses),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                for (item in businesses.orEmpty()) {
                    ListRow(
                        title = item.name,
                        subtitle = if (item.trn.isNotBlank()) stringResource(R.string.trn_value, item.trn) else null,
                        onClick = {
                            vm.selectBusiness(item.id)
                            switcherOpen = false
                        },
                        leading = { BusinessAvatar(item, 40) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.id == b.id) {
                                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.selected), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    switcherOpen = false
                                    go.to("business/${item.id}")
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                    )
                }
                ListRow(
                    title = stringResource(R.string.add_business),
                    onClick = {
                        switcherOpen = false
                        go.to("business/0")
                    },
                    leading = { IconBadge(Icons.Outlined.Add) },
                )
            }
        }
    }

    if (scanOpen) {
        ScanSheet(
            onDismiss = { scanOpen = false },
            onCamera = {
                scanOpen = false
                scan.camera()
            },
            onGallery = {
                scanOpen = false
                scan.gallery()
            },
            onManual = {
                scanOpen = false
                vm.pendingScan = null
                go.to("doc/${DocKind.PURCHASE}/0")
            },
        )
    }
}

@Composable
private fun HeroFigure(label: String, minor: Long, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.75f))
        Text(
            Money.format(minor),
            style = MaterialTheme.typography.titleMedium.tabular(),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, minor: Long, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp)) {
            IconBadge(icon, tint)
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Money.format(minor), style = MaterialTheme.typography.titleMedium.tabular(), maxLines = 1)
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Income and expense bars for the last six months. Built from layout boxes so it mirrors in Arabic. */
@Composable
private fun MonthlyBars(docs: List<Doc>, txns: List<Txn>) {
    val money = LocalMoneyColors.current
    val months = remember(docs, txns) {
        val first = LocalDate.now().withDayOfMonth(1)
        (5 downTo 0).map { back ->
            val start = first.minusMonths(back.toLong())
            val end = start.plusMonths(1).minusDays(1)
            start to summarize(docs, txns, start.toEpochDay()..end.toEpochDay())
        }
    }
    val peak = months.maxOf { maxOf(it.second.income, it.second.expenses) }.coerceAtLeast(1L)
    val formatter = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) }
    Row(Modifier.fillMaxWidth().height(132.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((start, s) in months) {
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Bar(s.income.toFloat() / peak, money.positive)
                    Bar(s.expenses.toFloat() / peak, money.negative)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    start.format(formatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        Modifier
            .width(10.dp)
            .fillMaxHeight(if (f > 0f) maxOf(f, 0.03f) else 0.015f)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(if (f > 0f) color else MaterialTheme.colorScheme.outlineVariant),
    )
}

// ---- importing an invoice from a picture

class ScanActions(val camera: () -> Unit, val gallery: () -> Unit)

/**
 * Registers the camera and gallery launchers at screen level, so the result still arrives after
 * the bottom sheet that started them has closed.
 */
@Composable
fun rememberScanActions(vm: AppViewModel, onReady: () -> Unit): ScanActions {
    val context = LocalContext.current
    val ready by rememberUpdatedState(onReady)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingPath
        if (saved && path != null) vm.scan(File(path)) { ready() }
    }
    val pickPicture = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importAndScan(uri) { ready() }
    }
    return remember(takePicture, pickPicture) {
        ScanActions(
            camera = {
                try {
                    val file = vm.newScanFile()
                    pendingPath = file.absolutePath
                    takePicture.launch(FileProvider.getUriForFile(context, context.packageName + ".files", file))
                } catch (e: Exception) {
                    // no camera app on this device; the gallery and manual entry still work
                }
            },
            gallery = {
                pickPicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSheet(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit, onManual: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                stringResource(R.string.add_purchase),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Text(
                stringResource(R.string.scan_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )
            ListRow(
                title = stringResource(R.string.scan_camera),
                subtitle = stringResource(R.string.scan_camera_hint),
                onClick = onCamera,
                leading = { IconBadge(Icons.Outlined.CameraAlt) },
            )
            ListRow(
                title = stringResource(R.string.scan_gallery),
                subtitle = stringResource(R.string.scan_gallery_hint),
                onClick = onGallery,
                leading = { IconBadge(Icons.Outlined.PhotoLibrary) },
            )
            ListRow(
                title = stringResource(R.string.enter_manually),
                subtitle = stringResource(R.string.enter_manually_hint),
                onClick = onManual,
                leading = { IconBadge(Icons.Outlined.Description) },
            )
        }
    }
}
