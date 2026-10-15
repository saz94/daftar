package com.daftar.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daftar.app.R
import com.daftar.app.data.DocKind
import com.daftar.app.data.TxnType

/** The few navigation moves the screens need. */
class Go(private val nav: NavHostController, private val onCreate: () -> Unit) {
    /** False while a screen is still fading in or out, so a second quick tap is ignored. */
    private fun settled(): Boolean =
        nav.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

    fun to(route: String) {
        if (settled()) nav.navigate(route) { launchSingleTop = true }
    }

    /** Opens a screen from a background result (a finished scan), where no tap is involved. */
    fun toNow(route: String) {
        nav.navigate(route) { launchSingleTop = true }
    }

    fun back() {
        if (settled()) backNow()
    }

    /** Leaves the screen even if it has not finished appearing. Never empties the stack. */
    fun backNow() {
        if (nav.previousBackStackEntry != null) nav.popBackStack()
    }

    /** Switch between the bottom tabs without stacking them. */
    fun tab(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** Replace the current screen (after saving a new invoice, show its preview instead). */
    fun replace(route: String) {
        backNow()
        nav.navigate(route)
    }

    /** Opens the "create something" sheet behind the centre + button. */
    fun create() = onCreate()
}

@Composable
fun AppRoot(vm: AppViewModel = viewModel(), demo: Boolean = false, startRoute: String? = null) {
    val businesses by vm.businesses.collectAsState()
    val list = businesses
    LaunchedEffect(demo, list == null) {
        if (demo && list != null && list.isEmpty()) vm.seedDemo()
    }
    GlowBackground {
        when {
            list == null -> Unit
            list.isEmpty() -> if (!demo) BusinessEditorScreen(vm, id = 0L, onDone = {}, onboarding = true)
            else -> AppNav(vm, startRoute)
        }
    }
    // The lock sits above everything. The app underneath stays alive, so a photo being taken or a
    // half-written invoice is still there after unlocking.
    if (vm.locked || vm.lockPreview) LockScreen(vm)

    // While a PIN is set, keep the app's contents out of screenshots and the recent-apps strip.
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(vm.lockEnabled, window) {
        if (vm.lockEnabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {}
    }

    if (vm.scanning) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(18.dp))
                    Text(stringResource(R.string.reading_invoice))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNav(vm: AppViewModel, startRoute: String?) {
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) { vm.createOpen = true } }
    val idArg = listOf(navArgument("id") { type = NavType.LongType })
    val scan = rememberScanActions(vm) { go.toNow("doc/${DocKind.PURCHASE}/0") }
    val context = LocalContext.current

    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { fadeIn(tween(160)) },
        exitTransition = { fadeOut(tween(110)) },
        popEnterTransition = { fadeIn(tween(160)) },
        popExitTransition = { fadeOut(tween(110)) },
    ) {
        composable("home") { HomeScreen(vm, go) }
        composable("docs") { DocsScreen(vm, go) }
        composable("money") { MoneyScreen(vm, go) }
        composable("more") { MoreScreen(vm, go) }

        composable("doc/{kind}/{id}", arguments = idArg) { entry ->
            DocEditorScreen(
                vm = vm,
                go = go,
                kind = entry.arguments?.getString("kind") ?: DocKind.INVOICE,
                id = entry.arguments?.getLong("id") ?: 0L,
            )
        }
        composable("preview/{id}", arguments = idArg) { entry ->
            DocPreviewScreen(vm, go, entry.arguments?.getLong("id") ?: 0L)
        }
        composable("txn/{type}/{id}", arguments = idArg) { entry ->
            TxnEditorScreen(
                vm = vm,
                go = go,
                type = entry.arguments?.getString("type") ?: TxnType.EXPENSE,
                id = entry.arguments?.getLong("id") ?: 0L,
            )
        }
        composable("products") { ProductsScreen(vm, go) }
        composable("product/{id}", arguments = idArg) { entry ->
            ProductEditorScreen(vm, go, entry.arguments?.getLong("id") ?: 0L)
        }
        composable("shareholders") { ShareholdersScreen(vm, go) }
        composable("shareholder/{id}", arguments = idArg) { entry ->
            ShareholderEditorScreen(vm, go, entry.arguments?.getLong("id") ?: 0L)
        }
        composable("loans") { LoansScreen(vm, go) }
        composable("loan/{id}", arguments = idArg) { entry ->
            LoanEditorScreen(vm, go, entry.arguments?.getLong("id") ?: 0L)
        }
        composable("tax") { TaxScreen(vm, go) }
        composable("settings") { SettingsScreen(vm, go) }
        composable("setpin") { PinSetupScreen(vm, go) }
        composable("business/{id}", arguments = idArg) { entry ->
            BusinessEditorScreen(vm, id = entry.arguments?.getLong("id") ?: 0L, onDone = { go.back() }, onBack = { go.back() })
        }
    }

    // Screenshot support: open a given screen straight after launch.
    LaunchedEffect(startRoute) {
        if (!startRoute.isNullOrBlank() && startRoute != "home") {
            when (startRoute) {
                "create" -> vm.createOpen = true
                "scan" -> vm.scanOpen = true
                "lock" -> vm.lockPreview = true
                "pinwidget" -> com.daftar.app.widget.requestPinWidget(context, snapshot = true)
                "pinactions" -> com.daftar.app.widget.requestPinWidget(context, snapshot = false)
                "docs", "money", "more" -> go.tab(startRoute)
                else -> go.toNow(startRoute)
            }
        }
    }

    if (vm.createOpen) {
        ModalBottomSheet(
            onDismissRequest = { vm.createOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.create_new),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp),
                )
                val money = LocalMoneyColors.current
                val open: (String) -> Unit = { route ->
                    vm.createOpen = false
                    go.toNow(route)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CreateTile(Icons.AutoMirrored.Outlined.ReceiptLong, stringResource(R.string.new_invoice), stringResource(R.string.create_invoice_hint), MaterialTheme.colorScheme.primary, Modifier.weight(1f)) {
                        open("doc/${DocKind.INVOICE}/0")
                    }
                    CreateTile(Icons.Outlined.RequestQuote, stringResource(R.string.new_quotation), stringResource(R.string.create_quote_hint), MaterialTheme.colorScheme.primary, Modifier.weight(1f)) {
                        open("doc/${DocKind.QUOTATION}/0")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CreateTile(Icons.Outlined.CameraAlt, stringResource(R.string.action_scan), stringResource(R.string.create_scan_hint), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) {
                        vm.createOpen = false
                        vm.scanOpen = true
                    }
                    CreateTile(Icons.Outlined.ShoppingBag, stringResource(R.string.add_purchase), stringResource(R.string.create_purchase_hint), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) {
                        vm.pendingScan = null
                        open("doc/${DocKind.PURCHASE}/0")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CreateTile(Icons.Outlined.NorthEast, stringResource(R.string.add_expense), stringResource(R.string.create_expense_hint), money.negative, Modifier.weight(1f)) {
                        open("txn/${TxnType.EXPENSE}/0")
                    }
                    CreateTile(Icons.Outlined.SouthWest, stringResource(R.string.add_income), stringResource(R.string.create_income_hint), money.positive, Modifier.weight(1f)) {
                        open("txn/${TxnType.INCOME}/0")
                    }
                }
            }
        }
    }

    if (vm.scanOpen) {
        ScanSheet(
            onDismiss = { vm.scanOpen = false },
            onCamera = {
                vm.scanOpen = false
                scan.camera()
            },
            onGallery = {
                vm.scanOpen = false
                scan.gallery()
            },
            onManual = {
                vm.scanOpen = false
                vm.pendingScan = null
                go.toNow("doc/${DocKind.PURCHASE}/0")
            },
        )
    }
}

@Composable
private fun CreateTile(icon: ImageVector, title: String, hint: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            IconBadge(icon, tint)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
            )
        }
    }
}
