package com.claude.historyviewer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.claude.historyviewer.data.database.ConversationDatabase
import com.claude.historyviewer.data.model.ConversationEntry
import com.claude.historyviewer.data.model.MessageRole
import com.claude.historyviewer.data.model.MessageType
import com.claude.historyviewer.data.parser.ClaudeHistoryParser
import com.claude.historyviewer.data.repository.ConversationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConversationDetailUiState(
    val messages: List<ConversationEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFilters: Boolean = false,
    val activeFilters: Set<MessageRole> = setOf(),
    val availableMessageTypes: List<MessageRole> = MessageRole.values().toList()
)

class ConversationDetailViewModel(
    private val repository: ConversationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ConversationDetailUiState())
    val uiState: StateFlow<ConversationDetailUiState> = _uiState.asStateFlow()
    
    private val _allMessages = MutableStateFlow<List<ConversationEntry>>(emptyList())
    private val _activeFilters = MutableStateFlow<Set<MessageRole>>(setOf())
    
    init {
        // Combine all messages with active filters to produce filtered results
        viewModelScope.launch {
            combine(
                _allMessages,
                _activeFilters
            ) { messages, filters ->
                // Show only actual conversation messages (USER and ASSISTANT roles)
                // Filter out system messages, tool use, and tool results by default
                val conversationMessages = messages.filter { 
                    (it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT) &&
                    it.content.isNotBlank() // Only show messages with actual text content
                }
                
                if (filters.isEmpty()) {
                    conversationMessages
                } else {
                    conversationMessages.filter { it.role in filters }
                }
            }.collect { filteredMessages ->
                _uiState.update { 
                    it.copy(messages = filteredMessages)
                }
            }
        }
    }
    
    fun loadConversation(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                repository.getEntriesForSession(sessionId).collect { messages ->
                    _allMessages.value = messages
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
    
    fun toggleMessageTypeFilter() {
        _uiState.update { 
            it.copy(showFilters = !it.showFilters)
        }
    }
    
    fun toggleMessageTypeFilter(messageRole: MessageRole) {
        val currentFilters = _activeFilters.value.toMutableSet()
        if (messageRole in currentFilters) {
            currentFilters.remove(messageRole)
        } else {
            currentFilters.add(messageRole)
        }
        
        _activeFilters.value = currentFilters
        _uiState.update { 
            it.copy(activeFilters = currentFilters)
        }
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = ConversationDatabase.getDatabase(context)
            val parser = ClaudeHistoryParser()
            val repository = ConversationRepository(database.conversationDao(), parser)
            return ConversationDetailViewModel(repository) as T
        }
    }
}