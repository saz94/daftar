package com.daftar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daftar.app.R
import com.daftar.app.domain.Money
import com.daftar.app.domain.Period
import com.daftar.app.domain.PeriodType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val FieldShape = RoundedCornerShape(14.dp)
private const val DAY_MILLIS = 86_400_000L

/** Invisible mark that keeps "-1,234.50" from being reordered to "1,234.50-" in right-to-left text. */
const val LTR_MARK = "\u200E"

fun formatDay(day: Long): String =
    LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

/** Standard page: title bar, optional back arrow, optional bottom tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = fab,
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

/** Scrolling column for forms; keeps the focused field above the keyboard. */
@Composable
fun FormColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun BottomTabs(current: String, go: Go) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        val items = listOf(
            Triple("home", Icons.Outlined.Dashboard, R.string.tab_home),
            Triple("docs", Icons.Outlined.Description, R.string.tab_documents),
            Triple("money", Icons.Outlined.Payments, R.string.tab_money),
            Triple("more", Icons.Outlined.MoreHoriz, R.string.tab_more),
        )
        items.forEachIndexed { index, (route, icon, label) ->
            if (index == 2) {
                // the one thing you do most, creating something, sits in the middle of the bar
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(
                        onClick = { go.create() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(54.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_new), modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
            NavigationBarItem(
                selected = current == route,
                onClick = { if (current != route) go.tab(route) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
    )
}

/** A round badge with a name's initials, tinted so the same name always gets the same colour. */
@Composable
fun InitialsAvatar(name: String, size: Int = 40) {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val initials = when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1)
        else -> words[0].take(1) + words[1].take(1)
    }.uppercase(Locale.getDefault())
    val tint = CategoryColors[Math.floorMod(name.hashCode(), CategoryColors.size)]
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.labelLarge,
            color = lerp(tint, MaterialTheme.colorScheme.onSurface, 0.45f),
            maxLines = 1,
        )
    }
}

/** A list row inside a white card: title + subtitle at the start, value at the end. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val base = Modifier.fillMaxWidth()
    Row(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** Groups rows in one rounded card with hairlines between them. */
@Composable
fun CardList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(content = content)
    }
}

@Composable
fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Amount(
    minor: Long,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = true,
) {
    Text(
        Money.format(minor),
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.tabular(),
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = color,
        maxLines = 1,
    )
}

@Composable
fun StatusPill(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Pill-shaped switch between a few views. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Month / quarter / year switch with arrows to step backwards and forwards. */
@Composable
fun PeriodBar(period: Period, onChange: (Period) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Segmented(
            options = listOf(
                stringResource(R.string.period_month),
                stringResource(R.string.period_quarter),
                stringResource(R.string.period_year),
                stringResource(R.string.period_all),
            ),
            selected = period.type.ordinal,
            onSelect = { onChange(Period(PeriodType.values()[it], 0)) },
        )
        if (period.type != PeriodType.ALL) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onChange(period.copy(offset = period.offset - 1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous))
                }
                Text(
                    period.label(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                )
                IconButton(onClick = { onChange(period.copy(offset = period.offset + 1)) }, enabled = period.offset < 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next))
                }
            }
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    suffix: String? = null,
    isError: Boolean = false,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = isError,
        suffix = if (suffix != null) {
            { Text(suffix) }
        } else {
            null
        },
        supportingText = if (supporting != null) {
            { Text(supporting) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (keyboard == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
        ),
        shape = FieldShape,
    )
}

@Composable
fun MoneyField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    currency: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    isError: Boolean = false,
) {
    Field(label, value, onChange, modifier, keyboard = KeyboardType.Decimal, suffix = currency, isError = isError)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    day: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    optional: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = if (day != null) formatDay(day) else "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
        )
        // the text field itself is read-only, so a transparent layer on top opens the calendar
        Box(
            Modifier
                .matchParentSize()
                .clip(FieldShape)
                .clickable(onClickLabel = label, role = Role.Button) { open = true },
        )
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (day ?: today()) * DAY_MILLIS)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(it / DAY_MILLIS) }
                    open = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                if (optional && day != null) {
                    TextButton(onClick = {
                        onChange(null)
                        open = false
                    }) { Text(stringResource(R.string.clear)) }
                } else {
                    TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun SaveButton(onClick: () -> Unit, enabled: Boolean = true, label: String = stringResource(R.string.save)) {
    // a double tap must not save the same thing twice
    var lastTap by remember { mutableLongStateOf(0L) }
    Button(
        onClick = {
            val now = System.nanoTime() / 1_000_000L
            if (now - lastTap > 700L) {
                lastTap = now
                onClick()
            }
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

/** Label on the start side, figure on the end side. */
@Composable
fun FigureRow(label: String, minor: Long, bold: Boolean = false, color: Color = MaterialTheme.colorScheme.onSurface, prefix: String = "") {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            LTR_MARK + prefix + Money.format(minor),
            style = (if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge).tabular(),
            color = color,
        )
    }
}
