package com.example.coffio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.example.coffio.data.local.datastore.AppPreferencesManager
import com.example.coffio.ui.i18n.AppLanguage
import com.example.coffio.ui.i18n.LocalStrings
import com.example.coffio.ui.i18n.englishStrings
import com.example.coffio.ui.i18n.germanStrings
import com.example.coffio.ui.navigation.NavGraph
import com.example.coffio.ui.theme.CoffioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appPreferencesManager = remember { AppPreferencesManager(this) }
            val language by appPreferencesManager.languageFlow.collectAsState(initial = AppLanguage.ENGLISH)
            val strings = if (language == AppLanguage.GERMAN) germanStrings else englishStrings

            CoffioTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
