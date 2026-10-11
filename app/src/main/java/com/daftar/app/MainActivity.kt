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
        setContent {
            DaftarTheme {
                AppRoot()
            }
        }
    }
}
