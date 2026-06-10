package com.neo.flashcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neo.flashcard.ui.screens.*
import com.neo.flashcard.ui.theme.NeoFlashcardTheme
import com.neo.flashcard.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme = viewModel.isDarkTheme.value
            NeoFlashcardTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    FlashcardApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun FlashcardApp(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(viewModel, navController)
        }
        composable("deck/{deckId}") { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId") ?: return@composable
            DeckScreen(viewModel, navController, deckId)
        }
        composable("done") {
            DoneScreen(viewModel, navController)
        }
    }
}
