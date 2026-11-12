package com.claude.historyviewer.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.claude.historyviewer.data.model.ConversationEntry
import com.claude.historyviewer.data.model.ConversationSession

@Database(
    entities = [ConversationEntry::class, ConversationSession::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ConversationDatabase : RoomDatabase() {
    
    abstract fun conversationDao(): ConversationDao
    
    companion object {
        @Volatile
        private var INSTANCE: ConversationDatabase? = null
        
        fun getDatabase(context: Context): ConversationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConversationDatabase::class.java,
                    "conversation_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}