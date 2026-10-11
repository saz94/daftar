package com.daftar.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daftar.app.R
import com.daftar.app.data.Txn
import com.daftar.app.data.TxnType
import com.daftar.app.domain.Money
import com.daftar.app.domain.parseDecimal
import com.daftar.app.domain.trimNumber
import com.daftar.app.domain.vatFromGross
import com.daftar.app.domain.vatFromNet

@Composable
fun MoneyScreen(vm: AppViewModel, go: Go) {
    val business by vm.business.collectAsState()
    val txns by vm.txns.collectAsState()
    val b = business ?: return
    val money = LocalMoneyColors.current

    val tab = vm.moneyTab
    val list = remember(txns, tab) {
        when (tab) {
            1 -> txns.filter { it.type == TxnType.INCOME }
            2 -> txns.filter { it.type == TxnType.EXPENSE }
            else -> txns
        }
    }
    val newType = if (tab == 1) TxnType.INCOME else TxnType.EXPENSE
    val addLabel = stringResource(if (tab == 1) R.string.add_income else R.string.add_expense)

    AppScreen(
        title = stringResource(R.string.income_and_expenses),
        bottomBar = { BottomTabs("money", go) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Segmented(
                options = listOf(stringResource(R.string.all), stringResource(R.string.income), stringResource(R.string.expenses)),
                selected = tab,
                onSelect = { vm.moneyTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (list.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Payments,
                    title = stringResource(R.string.empty_money_title),
                    body = stringResource(R.string.empty_money_body),
                    actionLabel = addLabel,
                    onAction = { go.to("txn/$newType/0") },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.id }) { txn ->
                        val income = txn.type == TxnType.INCOME
                        Surface(
                            onClick = { go.to("txn/${txn.type}/${txn.id}") },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            ListRow(
                                title = txn.description.ifBlank { txn.category.ifBlank { stringResource(if (income) R.string.income else R.string.expense) } },
                                subtitle = listOf(txn.category, txn.party, formatDay(txn.day)).filter { it.isNotBlank() }.joinToString("  ·  "),
                                leading = {
                                    IconBadge(
                                        if (income) Icons.Outlined.SouthWest else Icons.Outlined.NorthEast,
                                        if (income) money.positive else money.negative,
                                    )
                                },
                                trailing = {
                                    Text(
                                        LTR_MARK + (if (income) "+" else "-") + Money.format(txn.net + txn.vat),
                                        style = MaterialTheme.typography.bodyLarge.tabular(),
                                        color = if (income) money.positive else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TxnEditorScreen(vm: AppViewModel, go: Go, type: String, id: Long) {
    val business by vm.business.collectAsState()
    val txns by vm.txns.collectAsState()
    val b = business ?: return
    val existing = remember(id, txns) { txns.firstOrNull { it.id == id } }
    val key = existing?.id

    var kind by rememberSaveable(key) { mutableStateOf(existing?.type ?: type) }
    var amount by rememberSaveable(key) { mutableStateOf(existing?.let { Money.input(it.net + it.vat) } ?: "") }
    var hasVat by rememberSaveable(key) { mutableStateOf(existing?.let { it.vat > 0L } ?: (b.vatRate > 0.0)) }
    var rate by rememberSaveable(key) { mutableStateOf(trimNumber(existing?.vatRate?.takeIf { it > 0.0 } ?: b.vatRate)) }
    var category by rememberSaveable(key) { mutableStateOf(existing?.category ?: "") }
    var description by rememberSaveable(key) { mutableStateOf(existing?.description ?: "") }
    var party by rememberSaveable(key) { mutableStateOf(existing?.party ?: "") }
    var day by rememberSaveable(key) { mutableStateOf(existing?.day ?: today()) }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val income = kind == TxnType.INCOME
    val gross = Money.parse(amount)
    val rateValue = if (hasVat) (parseDecimal(rate) ?: 0.0) else 0.0
    val vat = if (gross != null) vatFromGross(gross, rateValue) else 0L
    val valid = gross != null && gross > 0L
    val presets = stringArrayResource(if (income) R.array.income_categories else R.array.expense_categories)

    AppScreen(
        title = stringResource(
            if (existing == null) {
                if (income) R.string.add_income else R.string.add_expense
            } else {
                if (income) R.string.income else R.string.expense
            },
        ),
        onBack = { go.back() },
        actions = {
            if (existing != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        },
    ) { padding ->
        FormColumn(padding) {
            Segmented(
                options = listOf(stringResource(R.string.expense), stringResource(R.string.income)),
                selected = if (income) 1 else 0,
                onSelect = { kind = if (it == 1) TxnType.INCOME else TxnType.EXPENSE },
            )
            MoneyField(
                label = stringResource(R.string.amount_paid_or_received),
                value = amount,
                onChange = { amount = it },
                currency = b.currency,
                isError = showErrors && !valid,
            )
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.includes_vat), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.includes_vat_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = hasVat, onCheckedChange = { hasVat = it })
                }
                if (hasVat) {
                    Spacer(Modifier.height(12.dp))
                    Field(stringResource(R.string.vat_rate), rate, { rate = it }, keyboard = KeyboardType.Decimal, suffix = "%")
                    if (gross != null) {
                        Spacer(Modifier.height(8.dp))
                        FigureRow(stringResource(R.string.amount_excl_vat), gross - vat)
                        FigureRow(stringResource(R.string.vat), vat)
                    }
                }
            }

            SectionLabel(stringResource(R.string.category))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (preset in presets) {
                    FilterChip(
                        selected = category == preset,
                        onClick = { category = if (category == preset) "" else preset },
                        label = { Text(preset) },
                    )
                }
            }
            Field(stringResource(R.string.category_custom), category, { category = it })
            Field(stringResource(R.string.description_optional), description, { description = it })
            Field(stringResource(if (income) R.string.received_from else R.string.paid_to), party, { party = it })
            DateField(stringResource(R.string.date), day, { if (it != null) day = it })

            SaveButton(onClick = {
                if (gross == null || !valid) {
                    showErrors = true
                } else {
                    vm.saveTxn(
                        Txn(
                            id = existing?.id ?: 0L,
                            businessId = b.id,
                            type = kind,
                            category = category.trim(),
                            description = description.trim(),
                            party = party.trim(),
                            net = gross - vat,
                            vat = vat,
                            vatRate = rateValue,
                            day = day,
                        ),
                    )
                    go.back()
                }
            })
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_entry_title),
            body = stringResource(R.string.delete_cannot_undo),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                vm.deleteTxn(existing.id)
                go.back()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
