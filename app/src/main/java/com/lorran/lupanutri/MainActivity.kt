package com.lorran.lupanutri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lorran.lupanutri.ui.AppNav
import com.lorran.lupanutri.ui.theme.LupaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LupaApp
        setContent {
            LupaTheme { AppNav(app) }
        }
    }
}
