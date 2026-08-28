package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.AppViewModelFactory
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.StockAiTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModelFactory: AppViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModelFactory = AppViewModelFactory(application)

        setContent {
            StockAiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(factory = viewModelFactory)
                }
            }
        }
    }
}
