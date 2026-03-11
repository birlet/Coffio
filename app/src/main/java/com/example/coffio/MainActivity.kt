package com.example.coffio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.coffio.ui.navigation.NavGraph
import com.example.coffio.ui.theme.CoffioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoffioTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
