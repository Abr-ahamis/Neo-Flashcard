package com.neo.flashcard.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isCollapsed: Boolean = false
)

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val folderId: String,
    val name: String
)

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val question: String,
    val answer: String,
    val note: String = "",
    val difficulty: String = "normal" // "normal", "hard", "easy"
)

data class FolderWithDecks(
    val folder: Folder,
    val decks: List<Deck>
)

data class DeckWithCards(
    val deck: Deck,
    val cards: List<Card>
)
