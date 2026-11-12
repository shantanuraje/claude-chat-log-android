package com.claude.historyviewer.data.parser

import com.claude.historyviewer.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.datetime.Instant
import java.io.File
import java.io.BufferedReader
import java.io.FileReader

class ClaudeHistoryParser {
    
    private val gson = Gson()
    
    fun parseHistoryFiles(claudeProjectsPath: String): List<ParsedSession> {
        val projectsDir = File(claudeProjectsPath)
        val sessions = mutableListOf<ParsedSession>()
        
        if (!projectsDir.exists()) {
            return sessions
        }
        
        // Parse each project directory
        projectsDir.listFiles()?.forEach { projectDir ->
            if (projectDir.isDirectory) {
                projectDir.listFiles { file -> file.extension == "jsonl" }?.forEach { jsonlFile ->
                    val session = parseJsonlFile(jsonlFile, projectDir.name)
                    if (session != null) {
                        sessions.add(session)
                    }
                }
            }
        }
        
        return sessions
    }
    
    private fun parseJsonlFile(file: File, projectPath: String): ParsedSession? {
        val entries = mutableListOf<ConversationEntry>()
        var sessionId: String? = null
        var startTime: Instant? = null
        var lastActivity: Instant? = null
        var version: String? = null
        
        try {
            BufferedReader(FileReader(file)).use { reader ->
                reader.lineSequence().forEach { line ->
                    if (line.trim().isNotEmpty()) {
                        val jsonObject = gson.fromJson(line, JsonObject::class.java)
                        val entry = parseJsonEntry(jsonObject)
                        
                        if (entry != null) {
                            entries.add(entry)
                            
                            // Extract session metadata
                            if (sessionId == null) {
                                sessionId = entry.sessionId
                                version = entry.version
                            }
                            
                            val currentStartTime = startTime
                            if (currentStartTime == null || entry.timestamp < currentStartTime) {
                                startTime = entry.timestamp
                            }
                            
                            val currentLastActivity = lastActivity
                            if (currentLastActivity == null || entry.timestamp > currentLastActivity) {
                                lastActivity = entry.timestamp
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        
        val finalSessionId = sessionId
        val finalStartTime = startTime
        val finalLastActivity = lastActivity
        
        return if (finalSessionId != null && finalStartTime != null && finalLastActivity != null) {
            val title = generateSessionTitle(entries)
            val totalTokens = entries.sumOf { 
                it.inputTokens + it.outputTokens + it.cacheCreationInputTokens + it.cacheReadInputTokens 
            }
            
            val session = ConversationSession(
                sessionId = finalSessionId,
                title = title,
                startTime = finalStartTime,
                lastActivity = finalLastActivity,
                messageCount = entries.size,
                totalTokens = totalTokens,
                projectPath = projectPath,
                version = version
            )
            
            ParsedSession(session, entries)
        } else null
    }
    
    private fun parseJsonEntry(jsonObject: JsonObject): ConversationEntry? {
        try {
            val uuid = jsonObject.get("uuid")?.asString ?: return null
            val parentUuid = jsonObject.get("parentUuid")?.asString
            val sessionId = jsonObject.get("sessionId")?.asString ?: return null
            val timestamp = Instant.parse(jsonObject.get("timestamp")?.asString ?: return null)
            val type = jsonObject.get("type")?.asString ?: return null
            val isSidechain = jsonObject.get("isSidechain")?.asBoolean ?: false
            val userType = jsonObject.get("userType")?.asString ?: "external"
            val cwd = jsonObject.get("cwd")?.asString
            val gitBranch = jsonObject.get("gitBranch")?.asString
            val version = jsonObject.get("version")?.asString
            val requestId = jsonObject.get("requestId")?.asString
            
            // Parse message content
            val messageObject = jsonObject.get("message")?.asJsonObject ?: return null
            val role = messageObject.get("role")?.asString ?: return null
            val content = extractMessageContent(messageObject)
            val model = messageObject.get("model")?.asString
            
            // Parse usage tokens
            val usageObject = messageObject.get("usage")?.asJsonObject
            val inputTokens = usageObject?.get("input_tokens")?.asInt ?: 0
            val outputTokens = usageObject?.get("output_tokens")?.asInt ?: 0
            val cacheCreationInputTokens = usageObject?.get("cache_creation_input_tokens")?.asInt ?: 0
            val cacheReadInputTokens = usageObject?.get("cache_read_input_tokens")?.asInt ?: 0
            
            return ConversationEntry(
                uuid = uuid,
                parentUuid = parentUuid,
                sessionId = sessionId,
                type = parseMessageType(type),
                role = parseMessageRole(role),
                content = content,
                timestamp = timestamp,
                model = model,
                requestId = requestId,
                isSidechain = isSidechain,
                userType = userType,
                cwd = cwd,
                gitBranch = gitBranch,
                version = version,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                cacheCreationInputTokens = cacheCreationInputTokens,
                cacheReadInputTokens = cacheReadInputTokens
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun extractMessageContent(messageObject: JsonObject): String {
        val content = messageObject.get("content")
        
        return when {
            content.isJsonArray -> {
                val contentArray = content.asJsonArray
                val textParts = mutableListOf<String>()
                
                contentArray.forEach { element ->
                    when {
                        element.isJsonObject -> {
                            val obj = element.asJsonObject
                            when (obj.get("type")?.asString) {
                                "text" -> obj.get("text")?.asString?.let { textParts.add(it) }
                                // Skip tool_use and tool_result - they're not part of the conversation text
                            }
                        }
                        element.isJsonPrimitive -> textParts.add(element.asString)
                    }
                }
                
                textParts.joinToString("\n").trim()
            }
            content.isJsonPrimitive -> content.asString
            else -> content.toString()
        }
    }
    
    private fun parseMessageType(type: String): MessageType {
        return when (type.lowercase()) {
            "user" -> MessageType.USER
            "assistant" -> MessageType.ASSISTANT
            "tool_use" -> MessageType.TOOL_USE
            "tool_result" -> MessageType.TOOL_RESULT
            "system" -> MessageType.SYSTEM
            else -> MessageType.USER
        }
    }
    
    private fun parseMessageRole(role: String): MessageRole {
        return when (role.lowercase()) {
            "user" -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            "system" -> MessageRole.SYSTEM
            else -> MessageRole.USER
        }
    }
    
    private fun generateSessionTitle(entries: List<ConversationEntry>): String {
        // Find the first user message content to use as title
        val firstUserMessage = entries.firstOrNull { it.role == MessageRole.USER }
        return if (firstUserMessage != null) {
            val content = firstUserMessage.content.take(50).trim()
            if (content.length > 47) "$content..." else content
        } else {
            "Conversation ${entries.firstOrNull()?.sessionId?.take(8) ?: "Unknown"}"
        }
    }
}

data class ParsedSession(
    val session: ConversationSession,
    val entries: List<ConversationEntry>
)