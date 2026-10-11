package com.daftar.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daftar.app.R
import com.daftar.app.data.Product
import com.daftar.app.domain.Money
import com.daftar.app.domain.parseDecimal
import com.daftar.app.domain.trimNumber

@Composable
fun ProductsScreen(vm: AppViewModel, go: Go) {
    val business by vm.business.collectAsState()
    val products by vm.products.collectAsState()
    val b = business ?: return
    var query by rememberSaveable { mutableStateOf("") }
    val list = remember(products, query) {
        if (query.isBlank()) {
            products
        } else {
            products.filter { it.name.contains(query.trim(), ignoreCase = true) || it.sku.contains(query.trim(), ignoreCase = true) }
        }
    }

    AppScreen(
        title = stringResource(R.string.products),
        onBack = { go.back() },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { go.to("product/0") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_product)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (products.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = stringResource(R.string.empty_products_title),
                    body = stringResource(R.string.empty_products_body),
                    actionLabel = stringResource(R.string.add_product),
                    onAction = { go.to("product/0") },
                )
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_products)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.id }) { product ->
                        Surface(
                            onClick = { go.to("product/${product.id}") },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            ListRow(
                                title = product.name,
                                subtitle = listOf(
                                    product.sku,
                                    stringResource(R.string.in_stock, trimNumber(product.stock), product.unit).trim(),
                                ).filter { it.isNotBlank() }.joinToString("  ·  "),
                                trailing = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Amount(product.price)
                                        Text(b.currency, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
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
fun ProductEditorScreen(vm: AppViewModel, go: Go, id: Long) {
    val business by vm.business.collectAsState()
    val products by vm.products.collectAsState()
    val b = business ?: return
    val existing = remember(id, products) { products.firstOrNull { it.id == id } }
    val key = existing?.id

    var name by rememberSaveable(key) { mutableStateOf(existing?.name ?: "") }
    var sku by rememberSaveable(key) { mutableStateOf(existing?.sku ?: "") }
    var unit by rememberSaveable(key) { mutableStateOf(existing?.unit ?: "") }
    var price by rememberSaveable(key) { mutableStateOf(Money.input(existing?.price ?: 0L)) }
    var cost by rememberSaveable(key) { mutableStateOf(Money.input(existing?.cost ?: 0L)) }
    var vat by rememberSaveable(key) { mutableStateOf(trimNumber(existing?.vatRate ?: b.vatRate)) }
    var stock by rememberSaveable(key) { mutableStateOf(existing?.let { trimNumber(it.stock) } ?: "") }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val priceValue = Money.parse(price)
    val costValue = Money.parse(cost) ?: 0L
    val margin = if (priceValue != null && priceValue > 0L) priceValue - costValue else null

    AppScreen(
        title = stringResource(if (existing == null) R.string.add_product else R.string.product),
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
                stringResource(R.string.product_name),
                name,
                { name = it },
                isError = showErrors && name.isBlank(),
                supporting = if (showErrors && name.isBlank()) stringResource(R.string.required) else null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(stringResource(R.string.sku_optional), sku, { sku = it }, Modifier.weight(1f), keyboard = KeyboardType.Ascii)
                Field(stringResource(R.string.unit_optional), unit, { unit = it }, Modifier.weight(1f))
            }
            MoneyField(stringResource(R.string.selling_price), price, { price = it }, b.currency, isError = showErrors && priceValue == null)
            MoneyField(stringResource(R.string.cost_price), cost, { cost = it }, b.currency)
            if (margin != null && costValue > 0L) {
                Panel(Modifier.fillMaxWidth()) {
                    FigureRow(
                        stringResource(R.string.profit_per_unit),
                        margin,
                        color = if (margin >= 0L) LocalMoneyColors.current.positive else LocalMoneyColors.current.negative,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(stringResource(R.string.vat_rate), vat, { vat = it }, Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "%")
                Field(stringResource(R.string.stock_quantity), stock, { stock = it }, Modifier.weight(1f), keyboard = KeyboardType.Decimal)
            }
            SaveButton(onClick = {
                if (name.isBlank() || priceValue == null) {
                    showErrors = true
                } else {
                    vm.saveProduct(
                        Product(
                            id = existing?.id ?: 0L,
                            businessId = b.id,
                            name = name.trim(),
                            sku = sku.trim(),
                            unit = unit.trim(),
                            price = priceValue,
                            cost = costValue,
                            vatRate = parseDecimal(vat) ?: b.vatRate,
                            stock = parseDecimal(stock) ?: 0.0,
                        ),
                    )
                    go.back()
                }
            })
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_product_title),
            body = stringResource(R.string.delete_product_body),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                vm.deleteProduct(existing.id)
                go.back()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
