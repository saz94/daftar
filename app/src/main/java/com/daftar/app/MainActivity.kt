package com.daftar.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.daftar.app.data.Prefs
import com.daftar.app.ui.AppRoot
import com.daftar.app.ui.AppViewModel
import com.daftar.app.ui.DaftarTheme

class MainActivity : AppCompatActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light / dark / follow-the-phone. AppCompat also restores the chosen language.
        Prefs.applyTheme(Prefs(this).themeMode)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // "route" opens a particular screen: used by the home-screen shortcuts widget, and when
        // taking screenshots. "demo" loads sample data and is only used for screenshots.
        val demo = intent?.getBooleanExtra("demo", false) ?: false
        val startRoute = if (savedInstanceState == null) intent?.getStringExtra("route") else null

        setContent {
            DaftarTheme {
                AppRoot(vm = vm, demo = demo, startRoute = startRoute)
            }
        }
    }

    // The app lock comes back on after the app has been out of sight for a while.
    override fun onStop() {
        super.onStop()
        vm.onAppHidden()
    }

    override fun onStart() {
        super.onStart()
        vm.onAppShown()
    }
}
