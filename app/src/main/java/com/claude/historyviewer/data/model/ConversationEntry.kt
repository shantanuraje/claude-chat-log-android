package com.claude.historyviewer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "conversation_entries")
data class ConversationEntry(
    @PrimaryKey
    val uuid: String,
    val parentUuid: String?,
    val sessionId: String,
    val type: MessageType,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant,
    val model: String?,
    val requestId: String?,
    val isSidechain: Boolean = false,
    val userType: String = "external",
    val cwd: String?,
    val gitBranch: String?,
    val version: String?,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val cacheCreationInputTokens: Int = 0,
    val cacheReadInputTokens: Int = 0
)

enum class MessageType {
    USER,
    ASSISTANT,
    TOOL_USE,
    TOOL_RESULT,
    SYSTEM
}

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class MessageContent(
    val type: String,
    val text: String? = null,
    val toolUseId: String? = null,
    val name: String? = null,
    val input: Map<String, Any>? = null
)

data class ToolUseResult(
    val oldTodos: List<TodoItem>? = null,
    val newTodos: List<TodoItem>? = null,
    val stdout: String? = null,
    val stderr: String? = null,
    val interrupted: Boolean = false,
    val isImage: Boolean = false
)

data class TodoItem(
    val id: String,
    val content: String,
    val status: String,
    val priority: String
)