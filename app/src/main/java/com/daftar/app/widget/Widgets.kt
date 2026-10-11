package com.daftar.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.daftar.app.MainActivity
import com.daftar.app.R
import com.daftar.app.data.AppDatabase
import com.daftar.app.data.DocKind
import com.daftar.app.data.DocStatus
import com.daftar.app.data.Prefs
import com.daftar.app.domain.Money
import com.daftar.app.domain.Period
import com.daftar.app.domain.PeriodType
import com.daftar.app.ui.summarize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val White = ColorProvider(Color.White)
private val Soft = ColorProvider(Color(0xBFFFFFFF))
private val Yellow = ColorProvider(Color(0xFFF6DC7A))

/** Opens the app, optionally straight on a screen (see the "route" handling in ui/Nav.kt). */
private fun openApp(context: Context, route: String? = null): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (route != null) {
            putExtra("route", route)
            // makes each shortcut a distinct intent, so the launcher does not reuse the wrong one
            action = "com.daftar.app.OPEN." + route
        }
    }

/** Redraws every placed widget. Called by the app whenever the books change. */
suspend fun refreshWidgets(context: Context) {
    SnapshotWidget().updateAll(context)
    ActionsWidget().updateAll(context)
}

/** Asks the launcher to place a widget, so nobody has to hunt for the long-press menu. */
fun requestPinWidget(context: Context, snapshot: Boolean): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    val receiver = if (snapshot) SnapshotWidgetReceiver::class.java else ActionsWidgetReceiver::class.java
    return manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
}

fun canPinWidgets(context: Context): Boolean =
    try {
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    } catch (e: Exception) {
        false
    }

// ------------------------------------------------------------------ this month at a glance

private class Snapshot(
    val business: String,
    val period: String,
    val currency: String,
    val profit: Long,
    val income: Long,
    val expenses: Long,
    val unpaid: Long,
    /** With the app lock on, the home screen must not show figures to whoever picks up the phone. */
    val hidden: Boolean,
)

private suspend fun loadSnapshot(context: Context): Snapshot? = withContext(Dispatchers.IO) {
    val prefs = Prefs(context)
    val dao = AppDatabase.get(context).dao()
    val all = dao.allBusinesses()
    val business = all.firstOrNull { it.id == prefs.currentBusinessId.value } ?: all.firstOrNull() ?: return@withContext null
    val period = Period(PeriodType.MONTH, 0)
    val docs = dao.docsOf(business.id)
    val summary = summarize(docs, dao.txnsOf(business.id), period.range())
    Snapshot(
        business = business.name,
        period = period.label(),
        currency = business.currency,
        profit = summary.profit,
        income = summary.income,
        expenses = summary.expenses,
        unpaid = docs.filter { it.kind == DocKind.INVOICE && it.status == DocStatus.UNPAID }.sumOf { it.total },
        hidden = prefs.hasPin,
    )
}

class SnapshotWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = try {
            loadSnapshot(context)
        } catch (e: Exception) {
            null
        }
        provideContent { SnapshotContent(context, data) }
    }
}

class SnapshotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SnapshotWidget()
}

private fun style(size: Int, color: ColorProvider = White, weight: FontWeight = FontWeight.Normal) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = weight)

@Composable
private fun SnapshotContent(context: Context, data: Snapshot?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(16.dp)
            .clickable(actionStartActivity(openApp(context))),
    ) {
        if (data == null) {
            Text(context.getString(R.string.app_name), style = style(16, weight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))
            Text(context.getString(R.string.widget_empty), style = style(13, Soft))
            return@Column
        }
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(data.business, style = style(14, weight = FontWeight.Medium), maxLines = 1, modifier = GlanceModifier.defaultWeight())
            Text(data.period, style = style(12, Soft), maxLines = 1)
        }
        Spacer(GlanceModifier.height(8.dp))
        if (data.hidden) {
            Text(context.getString(R.string.widget_locked_title), style = style(18, weight = FontWeight.Bold), maxLines = 1)
            Spacer(GlanceModifier.height(4.dp))
            Text(context.getString(R.string.widget_locked_body), style = style(13, Soft), maxLines = 2)
            return@Column
        }
        Text(context.getString(R.string.net_profit), style = style(12, Soft), maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Money.format(data.profit), style = style(26, weight = FontWeight.Bold), maxLines = 1)
            Spacer(GlanceModifier.width(6.dp))
            Text(data.currency, style = style(12, Soft), maxLines = 1)
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth()) {
            Figure(context.getString(R.string.income), data.income, White, GlanceModifier.defaultWeight())
            Figure(context.getString(R.string.expenses), data.expenses, Yellow, GlanceModifier.defaultWeight())
            Figure(context.getString(R.string.status_unpaid), data.unpaid, White, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun Figure(label: String, minor: Long, color: ColorProvider, modifier: GlanceModifier) {
    Column(modifier) {
        Text(label, style = style(11, Soft), maxLines = 1)
        Text(Money.format(minor), style = style(13, color, FontWeight.Medium), maxLines = 1)
    }
}

// ------------------------------------------------------------------ shortcuts

class ActionsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { ActionsContent(context) }
    }
}

class ActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActionsWidget()
}

@Composable
private fun ActionsContent(context: Context) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Shortcut(context, R.drawable.ic_widget_invoice, R.string.widget_invoice, "doc/INVOICE/0", GlanceModifier.defaultWeight())
        Shortcut(context, R.drawable.ic_widget_scan, R.string.widget_scan, "scan", GlanceModifier.defaultWeight())
        Shortcut(context, R.drawable.ic_widget_expense, R.string.widget_expense, "txn/EXPENSE/0", GlanceModifier.defaultWeight())
        Shortcut(context, R.drawable.ic_widget_income, R.string.widget_income, "txn/INCOME/0", GlanceModifier.defaultWeight())
    }
}

@Composable
private fun Shortcut(context: Context, icon: Int, label: Int, route: String, modifier: GlanceModifier) {
    Column(
        modifier = modifier.padding(vertical = 8.dp).clickable(actionStartActivity(openApp(context, route))),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(26.dp))
        Spacer(GlanceModifier.height(4.dp))
        Text(context.getString(label), style = style(11), maxLines = 1)
    }
}
