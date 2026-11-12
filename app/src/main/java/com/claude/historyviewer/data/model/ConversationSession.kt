package com.claude.historyviewer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "conversation_sessions")
data class ConversationSession(
    @PrimaryKey
    val sessionId: String,
    val title: String,
    val startTime: Instant,
    val lastActivity: Instant,
    val messageCount: Int,
    val totalTokens: Int,
    val projectPath: String,
    val version: String?
)

data class SessionSummary(
    val session: ConversationSession,
    val preview: String,
    val userMessages: Int,
    val assistantMessages: Int,
    val toolUseCount: Int
)