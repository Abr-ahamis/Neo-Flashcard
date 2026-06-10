package com.neo.flashcard.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neo.flashcard.data.AppDatabase
import com.neo.flashcard.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).flashcardDao()

    // Fully reactive streams from Room
    val folders = dao.getAllFolders()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val decks = dao.getAllDecks()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Deck Statistics (Real-time Progress)
    private val _deckStats = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val deckStats = _deckStats.asStateFlow()

    init {
        // Observe decks and update stats for each
        viewModelScope.launch {
            decks.collectLatest { deckList ->
                val statsMap = mutableMapOf<String, Pair<Int, Int>>()
                deckList.forEach { deck ->
                    launch {
                        combine(
                            dao.getCardCount(deck.id),
                            dao.getReviewedCardCount(deck.id)
                        ) { total, reviewed -> Pair(total, reviewed) }
                        .collect { stats ->
                            val current = _deckStats.value.toMutableMap()
                            current[deck.id] = stats
                            _deckStats.value = current
                        }
                    }
                }
            }
        }
    }

    // UI State
    val isDarkTheme = mutableStateOf(true)
    val currentDeckId = MutableStateFlow<String?>(null)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDeck = currentDeckId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else decks.map { list -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentCards = currentDeckId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList<Card>())
        else dao.getCardsForDeck(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study Logic State
    val studyQueue = mutableStateOf<List<Card>>(emptyList())
    val studyIndex = mutableStateOf(0)
    val easyCount = mutableStateOf(0)
    val hardCount = mutableStateOf(0)
    val isAnswerRevealed = mutableStateOf(false)

    // Simplified Import State (Stable Rollback)
    val isImporting = mutableStateOf(false)
    val importError = mutableStateOf<String?>(null)
    val importedCards = mutableStateOf<List<Card>>(emptyList())
    val importedFilename = mutableStateOf("")

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    fun addFolder(name: String, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val folder = Folder(name = name)
                dao.insertFolder(folder)
                withContext(Dispatchers.Main) { onComplete(folder.id) }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding folder", e)
            }
        }
    }

    fun addDeck(folderId: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.insertDeck(Deck(folderId = folderId, name = name))
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding deck", e)
            }
        }
    }

    fun addCard(deckId: String, question: String, answer: String, note: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.insertCard(Card(deckId = deckId, question = question, answer = answer, note = note))
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding card", e)
            }
        }
    }

    fun updateCard(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCard(card)
        }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCard(card)
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deck = decks.value.find { it.id == deckId }
            if (deck != null) {
                dao.deleteCardsForDeck(deckId)
                dao.deleteDeck(deck)
            }
        }
    }

    fun renameDeck(deckId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deck = decks.value.find { it.id == deckId }
            if (deck != null) {
                dao.updateDeck(deck.copy(name = newName))
            }
        }
    }

    fun startStudy(deckId: String) {
        currentDeckId.value = deckId
        viewModelScope.launch {
            val cards = dao.getCardsForDeck(deckId).first().shuffled()
            studyQueue.value = cards
            studyIndex.value = 0
            easyCount.value = 0
            hardCount.value = 0
            isAnswerRevealed.value = false
        }
    }

    fun nextCard() {
        if (studyIndex.value < studyQueue.value.size - 1) {
            studyIndex.value++
            isAnswerRevealed.value = false
        } else if (studyQueue.value.isNotEmpty()) {
            studyIndex.value = studyQueue.value.size 
        }
    }

    fun previousCard() {
        if (studyIndex.value > 0) {
            studyIndex.value--
            isAnswerRevealed.value = false
        }
    }

    fun revealAnswer() {
        isAnswerRevealed.value = true
    }

    fun rateCard(rating: String) {
        val currentCard = studyQueue.value.getOrNull(studyIndex.value) ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (rating) {
                    "easy" -> {
                        withContext(Dispatchers.Main) { easyCount.value++ }
                        dao.updateCard(currentCard.copy(difficulty = "easy"))
                        withContext(Dispatchers.Main) {
                            val newQueue = studyQueue.value.toMutableList()
                            newQueue.removeAt(studyIndex.value)
                            studyQueue.value = newQueue
                            isAnswerRevealed.value = false
                        }
                    }
                    "hard" -> {
                        withContext(Dispatchers.Main) { hardCount.value++ }
                        dao.updateCard(currentCard.copy(difficulty = "hard"))
                        withContext(Dispatchers.Main) {
                            val newQueue = studyQueue.value.toMutableList()
                            val card = newQueue.removeAt(studyIndex.value)
                            val targetIdx = (studyIndex.value + 2).coerceAtMost(newQueue.size)
                            newQueue.add(targetIdx, card)
                            studyQueue.value = newQueue
                            isAnswerRevealed.value = false
                        }
                    }
                    "review" -> {
                        dao.updateCard(currentCard.copy(difficulty = "normal"))
                        withContext(Dispatchers.Main) {
                            val newQueue = studyQueue.value.toMutableList()
                            val card = newQueue.removeAt(studyIndex.value)
                            val targetIdx = (studyIndex.value + 5).coerceAtMost(newQueue.size)
                            newQueue.add(targetIdx, card)
                            studyQueue.value = newQueue
                            isAnswerRevealed.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error rating card", e)
            }
        }
    }

    // Stable Rollback CSV Parsing
    fun processCsvFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { 
                    isImporting.value = true
                    importError.value = null
                    importedCards.value = emptyList()
                }
                
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri) 
                    ?: throw Exception("Could not open file")
                
                val reader = BufferedReader(InputStreamReader(inputStream))
                val text = reader.readText()
                inputStream.close()

                if (text.isBlank()) throw Exception("File is empty")

                val lines = text.split(Regex("\\r?\\n")).filter { it.trim().isNotEmpty() }
                if (lines.isEmpty()) throw Exception("No content in CSV")

                val cards = mutableListOf<Card>()
                
                for (line in lines) {
                    val parts = line.split(Regex("[,;\\t]"))
                    if (parts.size >= 2) {
                        cards.add(Card(
                            deckId = "", // assigned on import
                            question = parts[0].replace(Regex("^\"|\"$"), "").trim(),
                            answer = parts[1].replace(Regex("^\"|\"$"), "").trim(),
                            note = if (parts.size > 2) parts[2].replace(Regex("^\"|\"$"), "").trim() else ""
                        ))
                    }
                }

                if (cards.isEmpty()) throw Exception("No cards could be parsed from file. Ensure it has at least two columns.")

                val filename = getCleanFileName(uri)
                withContext(Dispatchers.Main) {
                    importedCards.value = cards
                    importedFilename.value = filename.replace(Regex("\\.[^.]+$"), "")
                    isImporting.value = false
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error parsing CSV", e)
                withContext(Dispatchers.Main) {
                    importError.value = e.message ?: "Unknown error"
                    isImporting.value = false
                }
            }
        }
    }

    private fun getCleanFileName(uri: Uri): String {
        val context = getApplication<Application>()
        var result: String? = null
        try {
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) result = c.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error getting filename", e)
        }
        
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "Imported Deck"
    }

    fun finalizeImport(folderId: String, folderName: String, deckName: String) {
        val cards = importedCards.value
        if (cards.isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isImporting.value = true }
                
                var finalFolderId = folderId
                val existingFolder = dao.getAllFolders().first().find { it.name == folderName }
                if (existingFolder != null) {
                    finalFolderId = existingFolder.id
                } else {
                    val newFolder = Folder(id = folderId, name = folderName)
                    dao.insertFolder(newFolder)
                    finalFolderId = newFolder.id
                }

                val deck = Deck(folderId = finalFolderId, name = deckName)
                dao.insertDeck(deck)
                
                cards.forEach { card ->
                    dao.insertCard(card.copy(deckId = deck.id))
                }
                
                withContext(Dispatchers.Main) {
                    importedCards.value = emptyList()
                    isImporting.value = false
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error finalizing import", e)
                withContext(Dispatchers.Main) {
                    importError.value = "Import failed: ${e.message}"
                    isImporting.value = false
                }
            }
        }
    }
}
