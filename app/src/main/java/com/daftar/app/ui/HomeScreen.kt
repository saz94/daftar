package com.daftar.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.daftar.app.R
import com.daftar.app.data.Business
import com.daftar.app.data.Doc
import com.daftar.app.data.DocKind
import com.daftar.app.data.DocStatus
import com.daftar.app.data.Txn
import com.daftar.app.data.TxnType
import com.daftar.app.data.net
import com.daftar.app.domain.Money
import com.daftar.app.domain.Period
import com.daftar.app.domain.PeriodType
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The business's logo if it has one, otherwise its initial on its colour. */
@Composable
fun BusinessAvatar(business: Business, size: Int = 36) {
    val shape = RoundedCornerShape((size / 3.2f).dp)
    val logo = business.logoPath
    if (logo != null) {
        AsyncImage(
            model = File(logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size.dp)
                .clip(shape)
                .background(Color.White)
                .padding(2.dp),
        )
    } else {
        val color = BusinessColors[Math.floorMod(business.colorIndex, BusinessColors.size)]
        Box(
            Modifier
                .size(size.dp)
                .clip(shape)
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
}

/** [warm] = money going out or late (pink-orange card); otherwise the lime-teal card. */
private class Attention(val icon: ImageVector, val tint: Color, val title: String, val detail: String, val amount: Long, val warm: Boolean = true, val onClick: () -> Unit)

private class Activity(val day: Long, val id: Long, val title: String, val detail: String, val amount: Long, val moneyIn: Boolean?, val route: String)

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
    val money = LocalMoneyColors.current
    val today = today()

    val period = vm.period
    val range = period.range()
    val summary = remember(docs, txns, range) { summarize(docs, txns, range) }
    val previous = remember(docs, txns, period) {
        if (period.type == PeriodType.ALL) null else summarize(docs, txns, period.copy(offset = period.offset - 1).range())
    }
    val months = remember(docs, txns) {
        val first = LocalDate.now().withDayOfMonth(1)
        (5 downTo 0).map { back ->
            val start = first.minusMonths(back.toLong())
            start to summarize(docs, txns, start.toEpochDay()..start.plusMonths(1).minusDays(1).toEpochDay())
        }
    }

    val unpaid = remember(docs) { docs.filter { it.kind == DocKind.INVOICE && it.status == DocStatus.UNPAID } }
    val overdue = unpaid.filter { it.dueDay != null && it.dueDay < today }
    val bills = remember(docs) { docs.filter { it.kind == DocKind.PURCHASE && it.status == DocStatus.UNPAID } }
    val loanBalance = loans.sumOf { it.principal } - payments.sumOf { it.amount }
    val capital = shareholders.sumOf { it.invested }
    val quarter = Period(PeriodType.QUARTER, 0)
    val quarterVat = remember(docs, txns) { summarize(docs, txns, quarter.range()).vatDue }

    val attention = ArrayList<Attention>()
    if (overdue.isNotEmpty()) {
        attention.add(
            Attention(Icons.Outlined.WarningAmber, money.negative, stringResource(R.string.attention_overdue, overdue.size.toString()), stringResource(R.string.attention_overdue_hint), overdue.sumOf { it.total }) {
                vm.docsTab = 0
                vm.docsFilter = 3
                go.tab("docs")
            },
        )
    }
    if (bills.isNotEmpty()) {
        attention.add(
            Attention(Icons.Outlined.ShoppingBag, money.due, stringResource(R.string.attention_bills, bills.size.toString()), stringResource(R.string.attention_bills_hint), bills.sumOf { it.total }) {
                vm.docsTab = 2
                vm.docsFilter = 1
                go.tab("docs")
            },
        )
    }
    if (quarterVat > 0L) {
        attention.add(
            Attention(Icons.Outlined.Percent, money.due, stringResource(R.string.attention_vat, quarter.label()), stringResource(R.string.attention_vat_hint), quarterVat, warm = false) {
                go.to("tax")
            },
        )
    }
    val paidByLoan = payments.groupBy { it.loanId }.mapValues { entry -> entry.value.sumOf { it.amount } }
    for (loan in loans) {
        val left = loan.principal - (paidByLoan[loan.id] ?: 0L)
        val due = loan.dueDay
        if (left > 0L && due != null && due <= today + 30) {
            attention.add(
                Attention(Icons.Outlined.AccountBalance, if (due < today) money.negative else money.due, stringResource(R.string.attention_loan, loan.lender), stringResource(R.string.due_on, formatDay(due)), left) {
                    go.to("loan/${loan.id}")
                },
            )
        }
    }

    val purchasesLabel = stringResource(R.string.purchases)
    val otherLabel = stringResource(R.string.other)
    val breakdown = remember(docs, txns, range, purchasesLabel, otherLabel) {
        val totals = LinkedHashMap<String, Long>()
        for (t in txns) {
            if (t.type != TxnType.EXPENSE || (range != null && t.day !in range)) continue
            val key = t.category.ifBlank { otherLabel }
            totals[key] = (totals[key] ?: 0L) + t.net
        }
        val bought = docs.filter { it.kind == DocKind.PURCHASE && (range == null || it.day in range) }.sumOf { it.net }
        if (bought > 0L) totals[purchasesLabel] = (totals[purchasesLabel] ?: 0L) + bought
        val sorted = totals.entries.filter { it.value > 0L }.sortedByDescending { it.value }.map { it.key to it.value }
        if (sorted.size <= 5) sorted else sorted.take(4) + (otherLabel to sorted.drop(4).sumOf { it.second })
    }

    val invoiceLabel = stringResource(R.string.invoice)
    val quotationLabel = stringResource(R.string.quotation)
    val purchaseLabel = stringResource(R.string.purchase_invoice)
    val incomeLabel = stringResource(R.string.income)
    val expenseLabel = stringResource(R.string.expense)
    val noName = stringResource(R.string.no_name)
    val recent = remember(docs, txns, invoiceLabel, noName) {
        val all = ArrayList<Activity>()
        for (d in docs.take(8)) {
            val label = when (d.kind) {
                DocKind.INVOICE -> invoiceLabel
                DocKind.QUOTATION -> quotationLabel
                else -> purchaseLabel
            }
            val moneyIn = when (d.kind) {
                DocKind.INVOICE -> true
                DocKind.PURCHASE -> false
                else -> null
            }
            val route = if (d.kind == DocKind.PURCHASE) "doc/${d.kind}/${d.id}" else "preview/${d.id}"
            all.add(Activity(d.day, d.id, d.partyName.ifBlank { noName }, label + "  " + d.number, d.total, moneyIn, route))
        }
        for (t in txns.take(8)) {
            val income = t.type == TxnType.INCOME
            all.add(
                Activity(
                    t.day, t.id, t.description.ifBlank { t.category.ifBlank { if (income) incomeLabel else expenseLabel } },
                    t.category.ifBlank { if (income) incomeLabel else expenseLabel }, t.net + t.vat, income, "txn/${t.type}/${t.id}",
                ),
            )
        }
        all.sortedWith(compareByDescending<Activity> { it.day }.thenByDescending { it.id }).take(6)
    }

    TabFrame("home", go) { bottomBar ->
    Scaffold(
        bottomBar = bottomBar,
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
                        BusinessAvatar(b, 38)
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
                actions = {
                    IconButton(onClick = { go.to("settings") }) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) { padding ->
        val layout = LocalWindowLayout.current
        // the glance: period, orb, net profit, income and expenses
        val hero: @Composable ColumnScope.(horizontal: Boolean) -> Unit = { horizontal ->
            PeriodBar(period, onChange = { vm.period = it }, showStepper = false)

            HeroCard(
                horizontal = horizontal,
                period = period,
                onPeriod = { vm.period = it },
                currency = b.currency,
                summary = summary,
                previous = previous,
                previousLabel = if (period.type == PeriodType.ALL) "" else period.copy(offset = period.offset - 1).label(),
            )

        }
        // everything below the glance
        val details: @Composable ColumnScope.() -> Unit = {
            SectionLabel(stringResource(R.string.by_month))
            Panel(Modifier.fillMaxWidth()) {
                MonthBars(months)
            }

            if (attention.isNotEmpty()) {
                SectionLabel(stringResource(R.string.needs_attention))
                attention.forEach { item ->
                    GradientCard(
                        title = item.title,
                        detail = item.detail,
                        amount = Money.format(item.amount),
                        icon = item.icon,
                        warm = item.warm,
                        onClick = item.onClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (breakdown.isNotEmpty()) {
                SectionLabel(stringResource(R.string.where_money_went))
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Ring(breakdown.map { it.second }, Modifier.size(116.dp))
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val total = breakdown.sumOf { it.second }.coerceAtLeast(1L)
                            breakdown.forEachIndexed { index, (name, value) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(9.dp).clip(CircleShape).background(CategoryColors[index % CategoryColors.size]))
                                    Spacer(Modifier.width(8.dp))
                                    Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(
                                        (value * 100.0 / total).roundToInt().toString() + "%",
                                        style = MaterialTheme.typography.labelLarge.tabular(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SectionLabel(stringResource(R.string.where_you_stand))
            CardList {
                ListRow(
                    title = stringResource(R.string.unpaid_invoices),
                    subtitle = stringResource(R.string.owed_to_you),
                    onClick = {
                        vm.docsTab = 0
                        vm.docsFilter = 1
                        go.tab("docs")
                    },
                    leading = { IconBadge(Icons.Outlined.Schedule, MaterialTheme.colorScheme.primary) },
                    trailing = { AmountWithChevron(unpaid.sumOf { it.total }) },
                )
                RowDivider()
                ListRow(
                    title = stringResource(R.string.loans_outstanding),
                    subtitle = stringResource(R.string.you_owe),
                    onClick = { go.to("loans") },
                    leading = { IconBadge(Icons.Outlined.AccountBalance, money.negative) },
                    trailing = { AmountWithChevron(loanBalance) },
                )
                RowDivider()
                ListRow(
                    title = stringResource(R.string.capital_invested),
                    subtitle = stringResource(R.string.by_shareholders),
                    onClick = { go.to("shareholders") },
                    leading = { IconBadge(Icons.Outlined.Groups, money.positive) },
                    trailing = { AmountWithChevron(capital) },
                )
            }

            if (recent.isNotEmpty()) {
                SectionLabel(stringResource(R.string.recent_activity))
                CardList {
                    recent.forEachIndexed { index, item ->
                        if (index > 0) RowDivider()
                        ListRow(
                            title = item.title,
                            subtitle = item.detail + "  " + formatDay(item.day),
                            onClick = { go.to(item.route) },
                            leading = { InitialsAvatar(item.title) },
                            trailing = {
                                Text(
                                    LTR_MARK + (if (item.moneyIn == true) "+" else if (item.moneyIn == false) "-" else "") + Money.format(item.amount),
                                    style = MaterialTheme.typography.bodyLarge.tabular(),
                                    color = if (item.moneyIn == true) money.positive else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
            }
        }
        HomeBody(padding, layout, hero, details)
    }
    }

    if (switcherOpen) {
        ModalBottomSheet(onDismissRequest = { switcherOpen = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                Text(
                    stringResource(R.string.your_businesses),
                    style = MaterialTheme.typography.titleLarge,
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
                        leading = { BusinessAvatar(item, 42) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.id == b.id) {
                                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.selected), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    switcherOpen = false
                                    go.toNow("business/${item.id}")
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
                        go.toNow("business/0")
                    },
                    leading = { IconBadge(Icons.Outlined.Add) },
                )
            }
        }
    }
}

/**
 * Cover: one column, the glance first. Inner: the glance in a pane on the start side and the
 * details beside it, each scrolling on its own. Tabletop: the glance above the hinge, details below.
 */
@Composable
private fun HomeBody(
    padding: PaddingValues,
    layout: WindowLayout,
    hero: @Composable ColumnScope.(horizontal: Boolean) -> Unit,
    details: @Composable ColumnScope.() -> Unit,
) {
    @Composable
    fun scroller(content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
            Spacer(Modifier.height(12.dp))
        }
    }
    Box(Modifier.fillMaxSize().padding(padding)) {
        TabletopSplit(
            top = { scroller { hero(true) } },
            bottom = { scroller { details() } },
            whole = {
                if (layout.expanded) {
                    TwoPane(
                        first = { scroller { hero(false) } },
                        second = { scroller { details() } },
                        fraction = 0.42f,
                        minFirst = 340.dp,
                        maxFirst = 460.dp,
                    )
                } else {
                    scroller {
                        hero(false)
                        details()
                    }
                }
            },
        )
    }
}

@Composable
private fun AmountWithChevron(minor: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Amount(minor)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 2.dp).size(20.dp),
        )
    }
}

// ------------------------------------------------------------------ headline

/**
 * The reference's "82%" moment: a glowing gradient circle holding the profit margin (how much of
 * every dirham earned was kept), with the period to step through above it and the figures below.
 */
@Composable
private fun HeroCard(
    period: Period,
    onPeriod: (Period) -> Unit,
    currency: String,
    summary: Summary,
    previous: Summary?,
    previousLabel: String,
    /** Orb beside the figures instead of above them: for the short pane above a tabletop hinge. */
    horizontal: Boolean = false,
) {
    val money = LocalMoneyColors.current
    val margin = if (summary.income > 0L) (summary.profit * 100.0 / summary.income) else null
    val loss = summary.profit < 0L
    val orb: @Composable (Dp) -> Unit = { size ->
        GlowOrb(size = size, warm = loss) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 18.dp)) {
                Text(
                    if (margin == null) "–" else LTR_MARK + margin.roundToInt().coerceIn(-999, 999).toString() + "%",
                    style = (if (size < 170.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall).copy(fontWeight = FontWeight.SemiBold),
                    color = OnGradient,
                    maxLines = 1,
                )
                Text(
                    stringResource(R.string.profit_margin),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnGradient.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
    if (horizontal) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            orb(140.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (period.type == PeriodType.ALL) stringResource(R.string.period_all) else period.label(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.net_profit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(LTR_MARK + Money.format(summary.profit) + " " + currency, style = MaterialTheme.typography.headlineMedium.tabular(), maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroFigure(stringResource(R.string.income), summary.income, warm = false, modifier = Modifier.weight(1f))
                    HeroFigure(stringResource(R.string.expenses), summary.expenses, warm = true, modifier = Modifier.weight(1f))
                }
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // calendar-style header: < October 2026 >
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (period.type != PeriodType.ALL) {
                IconButton(onClick = { onPeriod(period.copy(offset = period.offset - 1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous))
                }
            }
            Text(
                if (period.type == PeriodType.ALL) stringResource(R.string.period_all) else period.label(),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            )
            if (period.type != PeriodType.ALL) {
                IconButton(onClick = { onPeriod(period.copy(offset = period.offset + 1)) }, enabled = period.offset < 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next))
                }
            }
        }

        orb(184.dp)

        Text(
            stringResource(R.string.net_profit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                LTR_MARK + Money.format(summary.profit),
                style = MaterialTheme.typography.displayLarge.tabular(),
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                currency,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        if (previous != null && previous.profit > 0L && summary.profit > 0L) {
            val change = (summary.profit - previous.profit) * 100.0 / abs(previous.profit)
            val up = change >= 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (up) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                    contentDescription = null,
                    tint = if (up) money.positive else money.negative,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(
                        if (up) R.string.more_than_previous else R.string.less_than_previous,
                        abs(change).roundToInt().coerceAtMost(999).toString() + "%",
                        previousLabel,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroFigure(stringResource(R.string.income), summary.income, warm = false, modifier = Modifier.weight(1f))
            HeroFigure(stringResource(R.string.expenses), summary.expenses, warm = true, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroFigure(label: String, minor: Long, warm: Boolean, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (warm) WarmGradient else BrandGradient))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Text(Money.format(minor), style = MaterialTheme.typography.titleMedium.tabular(), maxLines = 1)
    }
}

/**
 * Six months as pairs of rounded gradient bars, like the reference's "Repeating habits by day":
 * lime-teal for income, pink-orange for expenses. Bars grow in once, on arrival.
 */
@Composable
private fun MonthBars(months: List<Pair<LocalDate, Summary>>) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(if (started) 1f else 0f, tween(durationMillis = 800), label = "bars")
    val peak = months.maxOf { maxOf(it.second.income, it.second.expenses) }.coerceAtLeast(1L).toFloat()
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val formatter = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) }
    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((start, s) in months) {
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    for ((value, brush) in listOf(s.income to BrandGradient, s.expenses to WarmGradient)) {
                        Box(Modifier.width(12.dp).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(track), contentAlignment = Alignment.BottomCenter) {
                            val f = (value.coerceAtLeast(0L) / peak) * progress
                            if (f > 0f) {
                                Box(
                                    Modifier.fillMaxWidth().fillMaxHeight(maxOf(f, 0.04f)).clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                if (brush == BrandGradient) listOf(Teal, Lime) else listOf(Pink, Orange),
                                            ),
                                        ),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(start.format(formatter), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** Ring split by share of spending. Gaps between slices keep them distinct without outlines. */
@Composable
private fun Ring(values: List<Long>, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val thickness = 16.dp.toPx()
        val inset = thickness / 2
        val arcSize = Size(size.width - thickness, size.height - thickness)
        val total = values.sum().toFloat()
        if (total <= 0f) {
            drawArc(track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(thickness))
            return@Canvas
        }
        val gap = if (values.size > 1) 3f else 0f
        var start = -90f
        values.forEachIndexed { index, value ->
            val sweep = 360f * value / total
            if (sweep > gap) {
                drawArc(
                    CategoryColors[index % CategoryColors.size],
                    startAngle = start + gap / 2,
                    sweepAngle = sweep - gap,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(thickness),
                )
            }
            start += sweep
        }
    }
}
