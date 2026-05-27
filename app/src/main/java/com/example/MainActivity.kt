package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.MusicRepository
import com.example.ui.MusicPlayerScreen
import com.example.ui.MusicPlayerViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room Database & DAOs
        val database = AppDatabase.getDatabase(applicationContext)
        val trackDao = database.trackDao()
        val repository = MusicRepository(applicationContext, trackDao)

        // Initialize viewmodel using factory
        val viewModel: MusicPlayerViewModel by viewModels {
            MusicPlayerViewModel.Factory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Root Container
                    MusicPlayerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
