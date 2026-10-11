package com.daftar.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daftar.app.R
import com.daftar.app.data.Business
import com.daftar.app.data.Prefs
import com.daftar.app.domain.parseDecimal
import com.daftar.app.domain.trimNumber
import java.time.LocalDate

// ------------------------------------------------------------------ "More" tab

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        leading = { IconBadge(icon) },
        trailing = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}

@Composable
fun MoreScreen(vm: AppViewModel, go: Go) {
    val business by vm.business.collectAsState()
    val b = business ?: return

    AppScreen(title = stringResource(R.string.tab_more), bottomBar = { BottomTabs("more", go) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardList {
                MenuRow(Icons.Outlined.Inventory2, stringResource(R.string.products), stringResource(R.string.products_hint)) { go.to("products") }
                RowDivider()
                MenuRow(Icons.Outlined.Groups, stringResource(R.string.shareholders), stringResource(R.string.shareholders_hint)) { go.to("shareholders") }
                RowDivider()
                MenuRow(Icons.Outlined.AccountBalance, stringResource(R.string.loans), stringResource(R.string.loans_hint)) { go.to("loans") }
                RowDivider()
                MenuRow(Icons.Outlined.Percent, stringResource(R.string.tax_and_vat), stringResource(R.string.tax_hint)) { go.to("tax") }
            }
            CardList {
                MenuRow(Icons.Outlined.Storefront, stringResource(R.string.business_details), b.name) { go.to("business/${b.id}") }
                RowDivider()
                MenuRow(Icons.Outlined.Settings, stringResource(R.string.settings), stringResource(R.string.settings_hint)) { go.to("settings") }
            }
        }
    }
}

// ------------------------------------------------------------------ settings

@Composable
private fun LanguagePicker() {
    val tags = listOf("", "en", "ar")
    Segmented(
        // language names are shown in their own language so they can always be found
        options = listOf(stringResource(R.string.follow_phone), "English", "العربية"),
        selected = tags.indexOf(Prefs.currentLanguage()).coerceAtLeast(0),
        onSelect = { Prefs.setLanguage(tags[it]) },
    )
}

@Composable
fun SettingsScreen(vm: AppViewModel, go: Go) {
    var theme by remember { mutableStateOf(vm.prefs.themeMode) }
    var message by remember { mutableStateOf<Int?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            vm.exportBackup(uri) { ok -> message = if (ok) R.string.backup_saved else R.string.backup_failed }
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importBackup(uri) { ok -> message = if (ok) R.string.backup_restored else R.string.backup_unreadable }
        }
    }

    AppScreen(title = stringResource(R.string.settings), onBack = { go.back() }) { padding ->
        FormColumn(padding) {
            SectionLabel(stringResource(R.string.language))
            LanguagePicker()

            SectionLabel(stringResource(R.string.appearance))
            val themes = listOf(Prefs.THEME_SYSTEM, Prefs.THEME_LIGHT, Prefs.THEME_DARK)
            Segmented(
                options = listOf(stringResource(R.string.follow_phone), stringResource(R.string.theme_light), stringResource(R.string.theme_dark)),
                selected = themes.indexOf(theme).coerceAtLeast(0),
                onSelect = {
                    theme = themes[it]
                    vm.prefs.themeMode = themes[it]
                },
            )

            SectionLabel(stringResource(R.string.backup))
            CardList {
                ListRow(
                    title = stringResource(R.string.backup_save),
                    subtitle = stringResource(R.string.backup_save_hint),
                    onClick = { saveBackup.launch("daftar-backup-" + LocalDate.now() + ".json") },
                    leading = { IconBadge(Icons.Outlined.Save) },
                )
                RowDivider()
                ListRow(
                    title = stringResource(R.string.backup_restore),
                    subtitle = stringResource(R.string.backup_restore_hint),
                    onClick = { confirmRestore = true },
                    leading = { IconBadge(Icons.Outlined.Restore) },
                )
            }
            message?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
            }
            Text(
                stringResource(R.string.privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    }

    if (confirmRestore) {
        ConfirmDialog(
            title = stringResource(R.string.backup_restore),
            body = stringResource(R.string.backup_restore_warning),
            confirmLabel = stringResource(R.string.choose_file),
            onConfirm = {
                confirmRestore = false
                openBackup.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
            },
            onDismiss = { confirmRestore = false },
        )
    }
}

// ------------------------------------------------------------------ business details

@Composable
fun BusinessEditorScreen(
    vm: AppViewModel,
    id: Long,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null,
    onboarding: Boolean = false,
) {
    val businesses by vm.businesses.collectAsState()
    val all = businesses.orEmpty()
    val existing = remember(id, all) { all.firstOrNull { it.id == id } }
    val key = existing?.id

    var name by rememberSaveable(key) { mutableStateOf(existing?.name ?: "") }
    var trn by rememberSaveable(key) { mutableStateOf(existing?.trn ?: "") }
    var address by rememberSaveable(key) { mutableStateOf(existing?.address ?: "") }
    var phone by rememberSaveable(key) { mutableStateOf(existing?.phone ?: "") }
    var email by rememberSaveable(key) { mutableStateOf(existing?.email ?: "") }
    var currency by rememberSaveable(key) { mutableStateOf(existing?.currency ?: "AED") }
    var vatRate by rememberSaveable(key) { mutableStateOf(trimNumber(existing?.vatRate ?: 5.0)) }
    var invoicePrefix by rememberSaveable(key) { mutableStateOf(existing?.invoicePrefix ?: "INV-") }
    var quotePrefix by rememberSaveable(key) { mutableStateOf(existing?.quotePrefix ?: "QUO-") }
    var colorIndex by rememberSaveable(key) { mutableStateOf(existing?.colorIndex ?: (all.size % BusinessColors.size)) }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val trnDigits = trn.filter { it.isDigit() }
    val trnBad = trn.isNotBlank() && trnDigits.length != 15

    AppScreen(
        title = stringResource(
            when {
                onboarding -> R.string.app_name
                existing == null -> R.string.add_business
                else -> R.string.business_details
            },
        ),
        onBack = onBack,
        actions = {
            if (existing != null && all.size > 1) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        },
    ) { padding ->
        FormColumn(padding) {
            if (onboarding) {
                LanguagePicker()
                Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.welcome_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Field(
                stringResource(R.string.business_name),
                name,
                { name = it },
                isError = showErrors && name.isBlank(),
                supporting = if (showErrors && name.isBlank()) stringResource(R.string.required) else null,
            )
            Field(
                stringResource(R.string.trn_optional),
                trn,
                { trn = it },
                keyboard = KeyboardType.Number,
                isError = trnBad,
                supporting = stringResource(R.string.trn_hint),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(stringResource(R.string.currency), currency, { currency = it.uppercase().take(4) }, Modifier.weight(1f), keyboard = KeyboardType.Ascii)
                Field(stringResource(R.string.vat_rate), vatRate, { vatRate = it }, Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "%")
            }

            SectionLabel(stringResource(R.string.colour))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BusinessColors.forEachIndexed { index, color ->
                    val on = index == colorIndex
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (on) 3.dp else 0.dp, if (on) MaterialTheme.colorScheme.onBackground else Color.Transparent, CircleShape)
                            .selectable(selected = on, role = Role.RadioButton, onClick = { colorIndex = index }),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            SectionLabel(stringResource(R.string.shown_on_invoices))
            Field(stringResource(R.string.address_optional), address, { address = it })
            Field(stringResource(R.string.phone_optional), phone, { phone = it }, keyboard = KeyboardType.Phone)
            Field(stringResource(R.string.email_optional), email, { email = it }, keyboard = KeyboardType.Email)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(stringResource(R.string.invoice_prefix), invoicePrefix, { invoicePrefix = it }, Modifier.weight(1f), keyboard = KeyboardType.Ascii)
                Field(stringResource(R.string.quotation_prefix), quotePrefix, { quotePrefix = it }, Modifier.weight(1f), keyboard = KeyboardType.Ascii)
            }

            Spacer(Modifier.height(4.dp))
            SaveButton(
                label = stringResource(if (onboarding) R.string.get_started else R.string.save),
                onClick = {
                    if (name.isBlank()) {
                        showErrors = true
                    } else {
                        val base = existing ?: Business()
                        vm.saveBusiness(
                            base.copy(
                                name = name.trim(),
                                trn = trnDigits,
                                address = address.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                currency = currency.trim().ifBlank { "AED" },
                                vatRate = parseDecimal(vatRate) ?: 5.0,
                                invoicePrefix = invoicePrefix.trim(),
                                quotePrefix = quotePrefix.trim(),
                                colorIndex = colorIndex,
                            ),
                            onDone,
                        )
                    }
                },
            )
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_business_title, existing.name),
            body = stringResource(R.string.delete_business_body),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                vm.deleteBusiness(existing.id, onDone)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
