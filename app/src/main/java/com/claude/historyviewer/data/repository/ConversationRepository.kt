package com.claude.historyviewer.data.repository

import com.claude.historyviewer.data.database.ConversationDao
import com.claude.historyviewer.data.model.ConversationEntry
import com.claude.historyviewer.data.model.ConversationSession
import com.claude.historyviewer.data.parser.ClaudeHistoryParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ConversationRepository(
    private val dao: ConversationDao,
    private val parser: ClaudeHistoryParser
) {
    
    fun getAllSessions(): Flow<List<ConversationSession>> = dao.getSessionsWithStats()
    
    fun getEntriesForSession(sessionId: String): Flow<List<ConversationEntry>> = 
        dao.getEntriesForSession(sessionId)
    
    fun searchSessions(query: String): Flow<List<ConversationSession>> = 
        dao.searchSessions("%$query%")
    
    fun searchEntries(query: String): Flow<List<ConversationEntry>> = 
        dao.searchEntries("%$query%")
    
    suspend fun refreshFromFiles(claudeProjectsPath: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val parsedSessions = parser.parseHistoryFiles(claudeProjectsPath)
                
                // Clear existing data
                dao.deleteAllSessions()
                dao.deleteAllEntries()
                
                // Insert new data
                var totalEntries = 0
                parsedSessions.forEach { parsedSession ->
                    dao.insertSession(parsedSession.session)
                    dao.insertEntries(parsedSession.entries)
                    totalEntries += parsedSession.entries.size
                }
                
                Result.success(totalEntries)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun importSpecificFile(filePath: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                // This would parse a specific JSONL file
                // Implementation similar to refreshFromFiles but for a single file
                Result.success(0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}