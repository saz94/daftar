package com.daftar.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daftar.app.R
import com.daftar.app.data.Loan
import com.daftar.app.data.LoanPayment
import com.daftar.app.data.Shareholder
import com.daftar.app.domain.Money
import com.daftar.app.domain.parseDecimal
import com.daftar.app.domain.trimNumber
import java.util.Locale

/**
 * Ownership percentage for each shareholder. A percentage typed in by hand wins; everyone else
 * splits whatever is left in proportion to the money they put in.
 */
fun ownership(shareholders: List<Shareholder>): Map<Long, Double> {
    val fixed = shareholders.filter { it.sharePercent != null }
    val auto = shareholders.filter { it.sharePercent == null }
    val remaining = (100.0 - fixed.sumOf { it.sharePercent ?: 0.0 }).coerceAtLeast(0.0)
    val autoTotal = auto.sumOf { it.invested }
    val result = HashMap<Long, Double>()
    for (s in fixed) result[s.id] = s.sharePercent ?: 0.0
    for (s in auto) {
        result[s.id] = when {
            autoTotal > 0L -> remaining * s.invested / autoTotal
            auto.isNotEmpty() -> remaining / auto.size
            else -> 0.0
        }
    }
    return result
}

private fun percent(value: Double): String = String.format(Locale.ENGLISH, "%.1f%%", value)

// ------------------------------------------------------------------ shareholders

@Composable
fun ShareholdersScreen(vm: AppViewModel, go: Go) {
    val business by vm.business.collectAsState()
    val shareholders by vm.shareholders.collectAsState()
    val b = business ?: return
    val shares = remember(shareholders) { ownership(shareholders) }
    val total = shareholders.sumOf { it.invested }

    AppScreen(
        title = stringResource(R.string.shareholders),
        onBack = { go.back() },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { go.to("shareholder/0") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_shareholder)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        if (shareholders.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Groups,
                title = stringResource(R.string.empty_shareholders_title),
                body = stringResource(R.string.empty_shareholders_body),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(R.string.add_shareholder),
                onAction = { go.to("shareholder/0") },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "summary") {
                    Panel(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.capital_invested),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            Money.format(total) + " " + b.currency,
                            style = MaterialTheme.typography.headlineSmall.tabular(),
                        )
                        Spacer(Modifier.height(14.dp))
                        // one bar, split by ownership
                        Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
                            shareholders.forEachIndexed { index, s ->
                                val share = (shares[s.id] ?: 0.0).toFloat()
                                if (share > 0f) {
                                    Box(
                                        Modifier
                                            .weight(share)
                                            .fillMaxHeight()
                                            .background(BusinessColors[index % BusinessColors.size]),
                                    )
                                }
                            }
                            val unassigned = (100.0 - shares.values.sum()).toFloat()
                            if (unassigned > 0.05f) {
                                Box(Modifier.weight(unassigned).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
                            }
                        }
                    }
                }
                items(shareholders, key = { it.id }) { s ->
                    val index = shareholders.indexOf(s)
                    Surface(
                        onClick = { go.to("shareholder/${s.id}") },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        ListRow(
                            title = s.name,
                            subtitle = stringResource(R.string.owns_percent, percent(shares[s.id] ?: 0.0)),
                            leading = {
                                Box(Modifier.size(12.dp).clip(CircleShape).background(BusinessColors[index.coerceAtLeast(0) % BusinessColors.size]))
                            },
                            trailing = { Amount(s.invested) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShareholderEditorScreen(vm: AppViewModel, go: Go, id: Long) {
    val business by vm.business.collectAsState()
    val shareholders by vm.shareholders.collectAsState()
    val b = business ?: return
    val existing = remember(id, shareholders) { shareholders.firstOrNull { it.id == id } }
    val key = existing?.id

    var name by rememberSaveable(key) { mutableStateOf(existing?.name ?: "") }
    var phone by rememberSaveable(key) { mutableStateOf(existing?.phone ?: "") }
    var invested by rememberSaveable(key) { mutableStateOf(Money.input(existing?.invested ?: 0L)) }
    var share by rememberSaveable(key) { mutableStateOf(existing?.sharePercent?.let { trimNumber(it) } ?: "") }
    var joined by rememberSaveable(key) { mutableStateOf(existing?.joinedDay ?: today()) }
    var note by rememberSaveable(key) { mutableStateOf(existing?.note ?: "") }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val shareValue = if (share.isBlank()) null else parseDecimal(share)
    val shareBad = share.isNotBlank() && (shareValue == null || shareValue < 0.0 || shareValue > 100.0)

    AppScreen(
        title = stringResource(if (existing == null) R.string.add_shareholder else R.string.shareholder),
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
            Field(
                stringResource(R.string.full_name),
                name,
                { name = it },
                isError = showErrors && name.isBlank(),
                supporting = if (showErrors && name.isBlank()) stringResource(R.string.required) else null,
            )
            MoneyField(stringResource(R.string.amount_invested), invested, { invested = it }, b.currency)
            Field(
                stringResource(R.string.ownership_percent),
                share,
                { share = it },
                keyboard = KeyboardType.Decimal,
                suffix = "%",
                isError = shareBad,
                supporting = stringResource(R.string.ownership_hint),
            )
            DateField(stringResource(R.string.date_joined), joined, { if (it != null) joined = it })
            Field(stringResource(R.string.phone_optional), phone, { phone = it }, keyboard = KeyboardType.Phone)
            Field(stringResource(R.string.notes), note, { note = it }, singleLine = false)
            SaveButton(onClick = {
                if (name.isBlank() || shareBad) {
                    showErrors = true
                } else {
                    vm.saveShareholder(
                        Shareholder(
                            id = existing?.id ?: 0L,
                            businessId = b.id,
                            name = name.trim(),
                            phone = phone.trim(),
                            invested = Money.parse(invested) ?: 0L,
                            sharePercent = shareValue,
                            joinedDay = joined,
                            note = note.trim(),
                        ),
                    )
                    go.back()
                }
            })
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_shareholder_title),
            body = stringResource(R.string.delete_cannot_undo),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                vm.deleteShareholder(existing.id)
                go.back()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

// ------------------------------------------------------------------ loans

@Composable
private fun ProgressTrack(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(BrandGradient))
    }
}

@Composable
fun LoansScreen(vm: AppViewModel, go: Go) {
    val business by vm.business.collectAsState()
    val loans by vm.loans.collectAsState()
    val payments by vm.loanPayments.collectAsState()
    val b = business ?: return
    val money = LocalMoneyColors.current
    val paidByLoan = remember(payments) { payments.groupBy { it.loanId }.mapValues { entry -> entry.value.sumOf { it.amount } } }
    val outstanding = loans.sumOf { it.principal } - payments.sumOf { it.amount }

    AppScreen(
        title = stringResource(R.string.loans),
        onBack = { go.back() },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { go.to("loan/0") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_loan)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        if (loans.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.AccountBalance,
                title = stringResource(R.string.empty_loans_title),
                body = stringResource(R.string.empty_loans_body),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(R.string.add_loan),
                onAction = { go.to("loan/0") },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "summary") {
                    Panel(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.loans_outstanding),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(Money.format(outstanding) + " " + b.currency, style = MaterialTheme.typography.headlineSmall.tabular())
                    }
                }
                items(loans, key = { it.id }) { loan ->
                    val paid = paidByLoan[loan.id] ?: 0L
                    val left = loan.principal - paid
                    val overdue = left > 0L && loan.dueDay != null && loan.dueDay < today()
                    Surface(
                        onClick = { go.to("loan/${loan.id}") },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    loan.lender,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(12.dp))
                                Amount(left)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.paid_of, Money.format(paid), Money.format(loan.principal)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                when {
                                    left <= 0L -> StatusPill(stringResource(R.string.status_settled), money.positive)
                                    overdue -> StatusPill(stringResource(R.string.status_overdue), money.negative)
                                    loan.dueDay != null -> Text(
                                        stringResource(R.string.due_on, formatDay(loan.dueDay)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            ProgressTrack(
                                fraction = if (loan.principal > 0L) paid.toFloat() / loan.principal else 0f,
                                color = money.positive,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoanEditorScreen(vm: AppViewModel, go: Go, id: Long) {
    val business by vm.business.collectAsState()
    val loans by vm.loans.collectAsState()
    val allPayments by vm.loanPayments.collectAsState()
    val b = business ?: return
    val existing = remember(id, loans) { loans.firstOrNull { it.id == id } }
    val key = existing?.id
    val payments = remember(allPayments, id) { allPayments.filter { it.loanId == id } }
    val money = LocalMoneyColors.current

    var lender by rememberSaveable(key) { mutableStateOf(existing?.lender ?: "") }
    var principal by rememberSaveable(key) { mutableStateOf(Money.input(existing?.principal ?: 0L)) }
    var rate by rememberSaveable(key) { mutableStateOf(existing?.interestRate?.takeIf { it > 0.0 }?.let { trimNumber(it) } ?: "") }
    var start by rememberSaveable(key) { mutableStateOf(existing?.startDay ?: today()) }
    var due by rememberSaveable(key) { mutableStateOf(existing?.dueDay) }
    var note by rememberSaveable(key) { mutableStateOf(existing?.note ?: "") }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addingPayment by remember { mutableStateOf(false) }

    val principalValue = Money.parse(principal)
    val valid = lender.isNotBlank() && principalValue != null && principalValue > 0L
    val paid = payments.sumOf { it.amount }

    AppScreen(
        title = stringResource(if (existing == null) R.string.add_loan else R.string.loan),
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
            if (existing != null) {
                Panel(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.still_owed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Money.format(existing.principal - paid) + " " + b.currency,
                        style = MaterialTheme.typography.headlineSmall.tabular(),
                    )
                    Spacer(Modifier.height(10.dp))
                    ProgressTrack(if (existing.principal > 0L) paid.toFloat() / existing.principal else 0f, money.positive)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.paid_of, Money.format(paid), Money.format(existing.principal)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SectionLabel(stringResource(R.string.repayments))
                if (payments.isNotEmpty()) {
                    CardList {
                        payments.forEachIndexed { index, payment ->
                            if (index > 0) RowDivider()
                            ListRow(
                                title = Money.format(payment.amount) + " " + b.currency,
                                subtitle = listOf(formatDay(payment.day), payment.note).filter { it.isNotBlank() }.joinToString("  ·  "),
                                trailing = {
                                    IconButton(onClick = { vm.deleteLoanPayment(payment.id) }) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = stringResource(R.string.remove),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
                OutlinedButton(onClick = { addingPayment = true }, modifier = Modifier.fillMaxWidth(), shape = FieldShape) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.record_repayment))
                }
                SectionLabel(stringResource(R.string.loan_details))
            }

            Field(
                stringResource(R.string.lender),
                lender,
                { lender = it },
                isError = showErrors && lender.isBlank(),
                supporting = if (showErrors && lender.isBlank()) stringResource(R.string.required) else null,
            )
            MoneyField(
                stringResource(R.string.amount_borrowed),
                principal,
                { principal = it },
                b.currency,
                isError = showErrors && (principalValue == null || principalValue <= 0L),
            )
            Field(stringResource(R.string.interest_rate_optional), rate, { rate = it }, keyboard = KeyboardType.Decimal, suffix = "%")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(stringResource(R.string.start_date), start, { if (it != null) start = it }, Modifier.weight(1f))
                DateField(stringResource(R.string.due_date), due, { due = it }, Modifier.weight(1f), optional = true)
            }
            Field(stringResource(R.string.notes), note, { note = it }, singleLine = false)
            SaveButton(onClick = {
                if (!valid || principalValue == null) {
                    showErrors = true
                } else {
                    vm.saveLoan(
                        Loan(
                            id = existing?.id ?: 0L,
                            businessId = b.id,
                            lender = lender.trim(),
                            principal = principalValue,
                            interestRate = parseDecimal(rate) ?: 0.0,
                            startDay = start,
                            dueDay = due,
                            note = note.trim(),
                        ),
                    )
                    go.back()
                }
            })
        }
    }

    if (addingPayment && existing != null) {
        PaymentDialog(
            currency = b.currency,
            onDismiss = { addingPayment = false },
            onSave = { amount, day, text ->
                vm.addLoanPayment(LoanPayment(businessId = b.id, loanId = existing.id, amount = amount, day = day, note = text))
                addingPayment = false
            },
        )
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_loan_title),
            body = stringResource(R.string.delete_loan_body),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                vm.deleteLoan(existing.id)
                go.back()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun PaymentDialog(currency: String, onDismiss: () -> Unit, onSave: (Long, Long, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableLongStateOf(today()) }
    var note by remember { mutableStateOf("") }
    val value = Money.parse(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.record_repayment)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyField(stringResource(R.string.amount), amount, { amount = it }, currency)
                DateField(stringResource(R.string.date), day, { if (it != null) day = it })
                Field(stringResource(R.string.note_optional), note, { note = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = value != null && value > 0L,
                onClick = { if (value != null) onSave(value, day, note.trim()) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
