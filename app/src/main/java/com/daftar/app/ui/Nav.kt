package com.daftar.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
class Go(private val nav: NavHostController) {
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

    /** Replace the current screen (used after turning a quotation into an invoice). */
    fun replace(route: String) {
        backNow()
        nav.navigate(route)
    }
}

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val businesses by vm.businesses.collectAsState()
    val list = businesses
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            list == null -> Unit
            list.isEmpty() -> BusinessEditorScreen(vm, id = 0L, onDone = {}, onboarding = true)
            else -> AppNav(vm)
        }
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

@Composable
private fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) }
    val idArg = listOf(navArgument("id") { type = NavType.LongType })

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
        composable("business/{id}", arguments = idArg) { entry ->
            BusinessEditorScreen(vm, id = entry.arguments?.getLong("id") ?: 0L, onDone = { go.back() }, onBack = { go.back() })
        }
    }
}
