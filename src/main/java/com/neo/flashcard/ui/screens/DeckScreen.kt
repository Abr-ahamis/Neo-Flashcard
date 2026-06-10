package com.neo.flashcard.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.neo.flashcard.model.*
import com.neo.flashcard.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(viewModel: MainViewModel, navController: NavController, deckId: String) {
    val currentDeck by viewModel.currentDeck.collectAsState()
    val cards by viewModel.currentCards.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            DeckTopBar(
                title = currentDeck?.name ?: "Deck",
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Study", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.Layers, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cards", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.Style, null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                StudyTab(viewModel, navController)
            } else {
                CardsTab(viewModel, cards)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckTopBar(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 12.dp).size(44.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun StudyTab(viewModel: MainViewModel, navController: NavController) {
    val queue = viewModel.studyQueue.value
    val index = viewModel.studyIndex.value
    val isRevealed = viewModel.isAnswerRevealed.value

    if (queue.isEmpty() && viewModel.decks.value.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards in this deck yet.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    if (index >= queue.size && queue.isNotEmpty()) {
        LaunchedEffect(Unit) {
            navController.navigate("done")
        }
        return
    }

    val currentCard = queue.getOrNull(index) ?: return

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${index + 1} / ${queue.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${((index.toFloat() / queue.size) * 100).toInt()}% complete",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = (index.toFloat() / queue.size),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(4.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        Box(modifier = Modifier.weight(1f)) {
            FlashCardInteractionSystem(
                card = currentCard,
                isRevealed = isRevealed,
                onFlip = { if (!isRevealed) viewModel.revealAnswer() else viewModel.rateCard("review") },
                onPrev = { viewModel.previousCard() },
                onNext = { viewModel.nextCard() },
                onRate = { viewModel.rateCard(it) }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FlashCardInteractionSystem(
    card: Card,
    isRevealed: Boolean,
    onFlip: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRate: (String) -> Unit
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isRevealed) 180f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { 
                    this.rotationY = rotationY
                    cameraDistance = 15f * density
                }
        ) {
            if (rotationY <= 90f) {
                CardSide(
                    title = "QUESTION",
                    content = card.question,
                    icon = Icons.Default.HelpOutline,
                    onRate = onRate,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CardSide(
                    title = "ANSWER",
                    content = card.answer,
                    icon = Icons.Default.CheckCircleOutline,
                    isAnswer = true,
                    onRate = onRate,
                    modifier = Modifier.fillMaxSize().graphicsLayer { this.rotationY = 180f }
                )
            }
        }

        // Tap Regions for Navigation
        Row(modifier = Modifier.fillMaxSize()) {
            // Left region -> Prev
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.25f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPrev() }
            )
            // Center region -> Flip
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.5f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFlip() }
            )
            // Right region -> Next
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.25f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNext() }
            )
        }
    }
}

@Composable
fun CardSide(
    title: String,
    content: String,
    icon: ImageVector,
    isAnswer: Boolean = false,
    onRate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header: Clearly separated
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 20.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp)
                }
            }

            // Scrollable Content Area: Prevents overlapping and handles large text
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = content,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Rating Buttons: Always visible on both sides
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RatingButton(label = "Hard", color = Color(0xFFE05252), onClick = { onRate("hard") })
                RatingButton(label = "Review", color = Color(0xFFF97316), onClick = { onRate("review") })
                RatingButton(label = "Easy", color = Color(0xFF3DBA6B), onClick = { onRate("easy") })
            }
        }
    }
}

@Composable
fun RowScope.RatingButton(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, color = color, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsTab(viewModel: MainViewModel, cards: List<Card>) {
    var filter by remember { mutableStateOf("all") }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var editCard by remember { mutableStateOf<Card?>(null) }

    val filteredCards = if (filter == "hard") cards.filter { it.difficulty == "hard" } else cards

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = filter == "all",
                    onClick = { filter = "all" },
                    label = { Text("All Cards", fontWeight = FontWeight.Bold) },
                    shape = CircleShape
                )
                FilterChip(
                    selected = filter == "hard",
                    onClick = { filter = "hard" },
                    label = { Text("Hard Only", fontWeight = FontWeight.Bold) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE05252).copy(alpha = 0.15f), selectedLabelColor = Color(0xFFE05252))
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                if (filteredCards.isEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No cards found.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                itemsIndexed(filteredCards, key = { _, card -> card.id }) { _, card ->
                    CardListItem(
                        card = card,
                        onEdit = { editCard = card },
                        onDelete = { viewModel.deleteCard(card) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddCardDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            elevation = FloatingActionButtonDefaults.elevation(6.dp)
        ) {
            Icon(Icons.Default.Add, "Add Card", modifier = Modifier.size(28.dp))
        }
    }

    if (showAddCardDialog || editCard != null) {
        AddEditCardDialog(
            card = editCard,
            onDismiss = { showAddCardDialog = false; editCard = null },
            onConfirm = { q, a, n ->
                if (editCard != null) {
                    viewModel.updateCard(editCard!!.copy(question = q, answer = a, note = n))
                } else {
                    viewModel.addCard(viewModel.currentDeckId.value!!, q, a, n)
                }
                showAddCardDialog = false
                editCard = null
            }
        )
    }
}

@Composable
fun CardListItem(card: Card, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).background(if(card.difficulty == "hard") Color(0xFFE05252) else MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = card.question, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = card.answer, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFE05252).copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardDialog(card: Card? = null, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var question by remember { mutableStateOf(card?.question ?: "") }
    var answer by remember { mutableStateOf(card?.answer ?: "") }
    var note by remember { mutableStateOf(card?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (card != null) "Edit Flashcard" else "Add New Flashcard", fontWeight = FontWeight.Black, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("QUESTION", fontSize = 11.sp, fontWeight = FontWeight.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("ANSWER", fontSize = 11.sp, fontWeight = FontWeight.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("NOTES (OPTIONAL)", fontSize = 11.sp, fontWeight = FontWeight.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (question.isNotBlank() && answer.isNotBlank()) onConfirm(question, answer, note) },
                enabled = question.isNotBlank() && answer.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Card", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Bold) }
        }
    )
}
