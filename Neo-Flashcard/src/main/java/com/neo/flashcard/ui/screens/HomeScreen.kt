package com.neo.flashcard.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.neo.flashcard.model.*
import com.neo.flashcard.ui.theme.*
import com.neo.flashcard.viewmodel.MainViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, navController: NavController) {
    val folders by viewModel.folders.collectAsState()
    val decks by viewModel.decks.collectAsState()
    val isImporting by viewModel.isImporting
    val importError by viewModel.importError
    val importedCards by viewModel.importedCards
    val deckStats by viewModel.deckStats.collectAsState()
    
    var showFabMenu by remember { mutableStateOf(false) }
    var showNewDeckDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    var deckToRename by remember { mutableStateOf<Deck?>(null) }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            try {
                uri?.let { viewModel.processCsvFile(it) }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error picking file", e)
            }
        }
    )

    LaunchedEffect(importError) {
        importError?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(importedCards) {
        if (importedCards.isNotEmpty()) { showImportDialog = true }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            topBar = {
                HomeTopBar(onToggleTheme = { /*viewModel.toggleTheme()*/ })
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                HomeHeader()
                
                StatsBar(
                    deckCount = decks.size,
                    folderCount = folders.size,
                    cardCount = deckStats.values.sumOf { it.first }
                )

                if (decks.isEmpty() && folders.isEmpty()) {
                    EmptyState { showFabMenu = true }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
                    ) {
                        val foldersWithDecks = folders.map { folder ->
                            FolderWithDecks(folder, decks.filter { it.folderId == folder.id })
                        }.filter { it.decks.isNotEmpty() }

                        items(foldersWithDecks, key = { it.folder.id }) { folderWithDecks ->
                            FolderSection(
                                folderWithDecks = folderWithDecks,
                                deckStats = deckStats,
                                onDeckClick = { deckId ->
                                    viewModel.startStudy(deckId)
                                    navController.navigate("deck/$deckId")
                                },
                                onRenameDeck = { deckToRename = it },
                                onDeleteDeck = { deckToDelete = it }
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
            AnimatedVisibility(
                visible = showFabMenu,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 72.dp)
                ) {
                    FabMenuItem("Import CSV", Color(0xFFF97316), Icons.Default.FileUpload) {
                        showFabMenu = false
                        csvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/vnd.ms-excel"))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FabMenuItem("New deck", Color(0xFF3DBA6B), Icons.Default.Layers) {
                        showFabMenu = false
                        showNewDeckDialog = true
                    }
                }
            }

            FloatingActionButton(
                onClick = { showFabMenu = !showFabMenu },
                containerColor = if (showFabMenu) Color(0xFFE05252) else MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(60.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Menu",
                    modifier = Modifier.rotate(if (showFabMenu) 45f else 0f).size(28.dp)
                )
            }
        }

        if (isImporting && !showImportDialog) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Processing...", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showNewDeckDialog) {
        NewDeckDialog(
            folders = folders,
            onDismiss = { showNewDeckDialog = false },
            onConfirm = { folderName, deckName ->
                val existingFolder = folders.find { it.name == folderName }
                if (existingFolder != null) {
                    viewModel.addDeck(existingFolder.id, deckName)
                    showNewDeckDialog = false
                } else {
                    viewModel.addFolder(folderName) { folderId ->
                        viewModel.addDeck(folderId, deckName)
                        showNewDeckDialog = false
                    }
                }
            }
        )
    }

    if (showImportDialog && importedCards.isNotEmpty()) {
        StableImportDialog(
            viewModel = viewModel,
            folders = folders,
            onDismiss = { showImportDialog = false },
            onImport = { folderId, folderName, deckName ->
                viewModel.finalizeImport(folderId, folderName, deckName)
                showImportDialog = false
            }
        )
    }
    
    if (deckToRename != null) {
        RenameDeckDialog(
            deck = deckToRename!!,
            onDismiss = { deckToRename = null },
            onConfirm = { newName ->
                viewModel.renameDeck(deckToRename!!.id, newName)
                deckToRename = null
            }
        )
    }
    
    if (deckToDelete != null) {
        DeleteDeckDialog(
            deck = deckToDelete!!,
            onDismiss = { deckToDelete = null },
            onConfirm = {
                viewModel.deleteDeck(deckToDelete!!.id)
                deckToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StableImportDialog(
    viewModel: MainViewModel,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onImport: (String, String, String) -> Unit
) {
    val cards by viewModel.importedCards
    val defaultName by viewModel.importedFilename
    val isImporting by viewModel.isImporting
    
    var deckName by remember { mutableStateOf("") }
    var folderMode by remember { mutableStateOf("existing") }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    var newFolderName by remember { mutableStateOf("") }

    LaunchedEffect(defaultName) { deckName = defaultName }

    AlertDialog(
        onDismissRequest = if (isImporting) ({}) else onDismiss,
        confirmButton = {
            if (isImporting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            } else {
                Button(
                    onClick = {
                        val fName = if (folderMode == "existing") selectedFolder?.name ?: "" else newFolderName
                        if (deckName.isNotBlank() && fName.isNotBlank()) {
                            val folderId = folders.find { it.name == fName }?.id ?: UUID.randomUUID().toString()
                            onImport(folderId, fName, deckName)
                        }
                    },
                    enabled = deckName.isNotBlank() && (if(folderMode=="existing") selectedFolder != null else newFolderName.isNotBlank()),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Import ${cards.size} Cards", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isImporting) {
                TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Bold) }
            }
        },
        title = { Text("Complete Import", fontWeight = FontWeight.Black, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column {
                    Text("DECK NAME", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deckName,
                        onValueChange = { deckName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }
                
                Column {
                    Text("SELECT FOLDER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabOption("Existing", folderMode == "existing") { folderMode = "existing" }
                        TabOption("New Folder", folderMode == "new") { folderMode = "new" }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (folderMode == "existing") {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            folders.forEach { folder ->
                                FolderPill(folder.name, selected = selectedFolder == folder) { selectedFolder = folder }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            placeholder = { Text("e.g. Science") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun FabMenuItem(label: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.clickable { onClick() }.padding(vertical = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = borderStroke(),
            shadowElevation = 6.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onToggleTheme: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { },
        actions = {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun HomeHeader() {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFFC2571A))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Neo Flashcard",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.5).sp
            )
        }
        Text(
            text = "Short reviews, lasting learning.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatsBar(deckCount: Int, folderCount: Int, cardCount: Int) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip(icon = Icons.Default.Layers, label = "decks", count = deckCount, color = MaterialTheme.colorScheme.primary)
        StatChip(icon = Icons.Default.Folder, label = "folders", count = folderCount, color = Color(0xFF2ABFBF))
        StatChip(icon = Icons.Default.Style, label = "cards", count = cardCount, color = Color(0xFFF0C040))
    }
}

@Composable
fun StatChip(icon: ImageVector, label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        border = borderStroke(1.5.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(text = count.toString(), color = color, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FolderSection(
    folderWithDecks: FolderWithDecks,
    deckStats: Map<String, Pair<Int, Int>>,
    onDeckClick: (String) -> Unit,
    onRenameDeck: (Deck) -> Unit,
    onDeleteDeck: (Deck) -> Unit
) {
    var collapsed by remember { mutableStateOf(folderWithDecks.folder.isCollapsed) }

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { collapsed = !collapsed }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderWithDecks.folder.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${folderWithDecks.decks.size} decks",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        if (!collapsed) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                folderWithDecks.decks.forEach { deck ->
                    val stats = deckStats[deck.id] ?: Pair(0, 0)
                    DeckCard(
                        deck = deck,
                        stats = stats,
                        onClick = { onDeckClick(deck.id) },
                        onRename = { onRenameDeck(deck) },
                        onDelete = { onDeleteDeck(deck) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeckCard(deck: Deck, stats: Pair<Int, Int>, onClick: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val progress = if (stats.first > 0) stats.second.toFloat() / stats.first else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = borderStroke(),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFFC2571A))
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = deck.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = if (progress >= 1f) Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (progress >= 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (progress >= 1f) "Completed" else "${stats.second} / ${stats.first} cards",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Study now", fontWeight = FontWeight.Medium) },
                            onClick = { showMenu = false; onClick() },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename", fontWeight = FontWeight.Medium) },
                            onClick = { showMenu = false; onRename() },
                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (progress >= 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun EmptyState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "No decks yet", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            text = "Create a folder and add a deck, or import a CSV file to start studying.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
            lineHeight = 20.sp
        )
        Button(
            onClick = onCreateClick,
            modifier = Modifier.padding(top = 28.dp).height(50.dp).fillMaxWidth(0.7f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Get started", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewDeckDialog(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var deckName by remember { mutableStateOf("") }
    var folderMode by remember { mutableStateOf("existing") }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    var newFolderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { 
                    val fName = if (folderMode == "existing") selectedFolder?.name ?: "" else newFolderName
                    if (deckName.isNotBlank() && fName.isNotBlank()) onConfirm(fName, deckName)
                },
                enabled = deckName.isNotBlank() && (if(folderMode=="existing") selectedFolder != null else newFolderName.isNotBlank()),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Create Deck", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Bold) }
        },
        title = { Text("Create New Deck", fontWeight = FontWeight.Black, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column {
                    Text("DECK NAME", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deckName,
                        onValueChange = { deckName = it },
                        placeholder = { Text("e.g. Spanish Vocabulary") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }
                
                Column {
                    Text("FOLDER / CATEGORY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabOption("Existing", folderMode == "existing") { folderMode = "existing" }
                        TabOption("New Folder", folderMode == "new") { folderMode = "new" }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (folderMode == "existing") {
                        if (folders.isEmpty()) {
                            Text("No folders yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                folders.forEach { folder ->
                                    FolderPill(folder.name, selected = selectedFolder == folder) { selectedFolder = folder }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            placeholder = { Text("e.g. Languages") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDeckDialog(deck: Deck, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(deck.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Deck", fontWeight = FontWeight.Black) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }, shape = RoundedCornerShape(14.dp)) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun DeleteDeckDialog(deck: Deck, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Deck", fontWeight = FontWeight.Black) },
        text = { Text("Are you sure you want to delete '${deck.name}'?\nThis will remove all associated cards permanently.", fontSize = 15.sp, lineHeight = 22.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Delete permanently", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep it", fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun RowScope.TabOption(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.weight(1f).clickable { onClick() },
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (active) 4.dp else 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FolderPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp = 1.dp) = 
    androidx.compose.foundation.BorderStroke(width, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
