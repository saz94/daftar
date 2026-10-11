package com.daftar.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daftar.app.R
import com.daftar.app.domain.Money
import com.daftar.app.domain.Period
import com.daftar.app.domain.PeriodType
import com.daftar.app.domain.corporateTaxEstimate
import com.daftar.app.domain.parseDecimal
import com.daftar.app.domain.trimNumber
import com.daftar.app.domain.vatFromGross
import com.daftar.app.domain.vatFromNet

@Composable
fun TaxScreen(vm: AppViewModel, go: Go) {
    val business by vm.business.collectAsState()
    val docs by vm.docs.collectAsState()
    val txns by vm.txns.collectAsState()
    val b = business ?: return
    val money = LocalMoneyColors.current

    // VAT is usually filed quarterly, so that is where this screen opens
    var periodType by rememberSaveable { mutableIntStateOf(PeriodType.QUARTER.ordinal) }
    var periodOffset by rememberSaveable { mutableIntStateOf(0) }
    val period = Period(PeriodType.values()[periodType], periodOffset)
    val range = period.range()
    val summary = remember(docs, txns, range) { summarize(docs, txns, range) }
    val year = remember(docs, txns) { summarize(docs, txns, Period(PeriodType.YEAR, 0).range()) }

    var calcAmount by rememberSaveable { mutableStateOf("") }
    var calcRate by rememberSaveable { mutableStateOf(trimNumber(b.vatRate)) }
    var calcInclusive by rememberSaveable { mutableStateOf(false) }

    AppScreen(title = stringResource(R.string.tax_and_vat), onBack = { go.back() }) { padding ->
        FormColumn(padding) {
            PeriodBar(period, onChange = {
                periodType = it.type.ordinal
                periodOffset = it.offset
            })

            Panel(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(if (summary.vatDue >= 0L) R.string.vat_payable else R.string.vat_refundable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Money.format(Math.abs(summary.vatDue)) + " " + b.currency,
                    style = MaterialTheme.typography.headlineMedium.tabular(),
                    color = if (summary.vatDue > 0L) money.due else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                FigureRow(stringResource(R.string.vat_on_sales), summary.outputVat)
                FigureRow(stringResource(R.string.vat_on_purchases), summary.inputVat, prefix = "-")
            }

            Panel(Modifier.fillMaxWidth()) {
                FigureRow(stringResource(R.string.sales_excl_vat), summary.income)
                FigureRow(stringResource(R.string.purchases_excl_vat), summary.expenses)
                FigureRow(stringResource(R.string.net_profit), summary.profit, bold = true)
            }
            Text(
                stringResource(R.string.vat_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            SectionLabel(stringResource(R.string.vat_calculator))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyField(stringResource(R.string.amount), calcAmount, { calcAmount = it }, b.currency, Modifier.weight(2f))
                Field(stringResource(R.string.vat_rate), calcRate, { calcRate = it }, Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "%")
            }
            Segmented(
                options = listOf(stringResource(R.string.calc_add_vat), stringResource(R.string.calc_includes_vat)),
                selected = if (calcInclusive) 1 else 0,
                onSelect = { calcInclusive = it == 1 },
            )
            val amount = Money.parse(calcAmount)
            val rate = parseDecimal(calcRate) ?: 0.0
            if (amount != null) {
                val vat = if (calcInclusive) vatFromGross(amount, rate) else vatFromNet(amount, rate)
                val net = if (calcInclusive) amount - vat else amount
                Panel(Modifier.fillMaxWidth()) {
                    FigureRow(stringResource(R.string.amount_excl_vat), net)
                    FigureRow(stringResource(R.string.vat), vat)
                    FigureRow(stringResource(R.string.total_with_currency, b.currency), net + vat, bold = true)
                }
            }

            SectionLabel(stringResource(R.string.corporate_tax))
            Panel(Modifier.fillMaxWidth()) {
                FigureRow(stringResource(R.string.profit_this_year), year.profit)
                FigureRow(stringResource(R.string.corporate_tax_estimate), corporateTaxEstimate(year.profit), bold = true)
            }
            Text(
                stringResource(R.string.corporate_tax_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
