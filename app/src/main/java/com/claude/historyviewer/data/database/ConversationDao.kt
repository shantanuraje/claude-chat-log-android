package com.claude.historyviewer.data.database

import androidx.room.*
import com.claude.historyviewer.data.model.ConversationEntry
import com.claude.historyviewer.data.model.ConversationSession
import com.claude.historyviewer.data.model.SessionSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    @Query("SELECT * FROM conversation_sessions ORDER BY lastActivity DESC")
    fun getAllSessions(): Flow<List<ConversationSession>>
    
    @Query("SELECT * FROM conversation_entries WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEntriesForSession(sessionId: String): Flow<List<ConversationEntry>>
    
    @Query("""
        SELECT * FROM conversation_sessions 
        WHERE title LIKE :query OR sessionId LIKE :query
        ORDER BY lastActivity DESC
    """)
    fun searchSessions(query: String): Flow<List<ConversationSession>>
    
    @Query("""
        SELECT * FROM conversation_entries 
        WHERE content LIKE :query 
        ORDER BY timestamp DESC
    """)
    fun searchEntries(query: String): Flow<List<ConversationEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSession)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ConversationEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<ConversationEntry>)
    
    @Query("DELETE FROM conversation_sessions")
    suspend fun deleteAllSessions()
    
    @Query("DELETE FROM conversation_entries")
    suspend fun deleteAllEntries()
    
    @Query("""
        SELECT 
            s.*,
            COUNT(e.uuid) as messageCount,
            SUM(e.inputTokens + e.outputTokens + e.cacheCreationInputTokens + e.cacheReadInputTokens) as totalTokens
        FROM conversation_sessions s
        LEFT JOIN conversation_entries e ON s.sessionId = e.sessionId
        GROUP BY s.sessionId
        ORDER BY s.lastActivity DESC
    """)
    fun getSessionsWithStats(): Flow<List<ConversationSession>>
}