package com.neo.flashcard.data

import androidx.room.*
import com.neo.flashcard.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM decks")
    fun getAllDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE folderId = :folderId")
    fun getDecksForFolder(folderId: String): Flow<List<Deck>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId")
    fun getCardsForDeck(deckId: String): Flow<List<Card>>
    
    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    fun getCardCount(deckId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND difficulty != 'normal'")
    fun getReviewedCardCount(deckId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck)

    @Update
    suspend fun updateDeck(deck: Deck)

    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card)

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)
    
    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteCardsForDeck(deckId: String)
}

@Database(entities = [Folder::class, Deck::class, Card::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flashcard_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
