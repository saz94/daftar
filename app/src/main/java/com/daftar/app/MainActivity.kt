package com.daftar.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.daftar.app.data.Prefs
import com.daftar.app.ui.AppRoot
import com.daftar.app.ui.DaftarTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Light / dark / follow-the-phone. AppCompat also restores the chosen language.
        Prefs.applyTheme(Prefs(this).themeMode)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Used when taking screenshots: start with sample data, on a particular screen.
        val demo = intent?.getBooleanExtra("demo", false) ?: false
        val startRoute = if (savedInstanceState == null) intent?.getStringExtra("route") else null

        setContent {
            DaftarTheme {
                AppRoot(demo = demo, startRoute = startRoute)
            }
        }
    }
}
