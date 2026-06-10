package com.neo.flashcard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.neo.flashcard.viewmodel.MainViewModel

@Composable
fun DoneScreen(viewModel: MainViewModel, navController: NavController) {
    val currentDeck by viewModel.currentDeck.collectAsState()
    val easyCount = viewModel.easyCount.value
    val hardCount = viewModel.hardCount.value

    Scaffold(
        topBar = {
            DeckTopBar(
                title = currentDeck?.name ?: "Deck",
                onBack = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFFC2571A))
                                ),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(56.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "Session Complete!", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text(
                        text = "You've reviewed all cards in\n${currentDeck?.name ?: "the deck"}.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DoneStat(icon = Icons.Default.CheckCircle, count = easyCount, label = "Easy", color = Color(0xFF3DBA6B))
                        Spacer(modifier = Modifier.width(60.dp))
                        DoneStat(icon = Icons.Default.Warning, count = hardCount, label = "Hard", color = Color(0xFFE05252))
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { 
                                viewModel.startStudy(currentDeck?.id ?: "")
                                navController.navigate("deck/${currentDeck?.id}") { popUpTo("deck/${currentDeck?.id}") { inclusive = true } }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Study Again", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text("Back to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun DoneStat(icon: ImageVector, count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp).background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
    }
}
