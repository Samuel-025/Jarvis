package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.core.di.ServiceLocator
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    private val serviceLocator by lazy {
        ServiceLocator.getInstance(applicationContext)
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(serviceLocator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JarvisTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
