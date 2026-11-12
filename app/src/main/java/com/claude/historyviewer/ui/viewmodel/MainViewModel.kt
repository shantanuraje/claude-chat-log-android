package com.claude.historyviewer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.claude.historyviewer.data.database.ConversationDatabase
import com.claude.historyviewer.data.model.ConversationSession
import com.claude.historyviewer.data.parser.ClaudeHistoryParser
import com.claude.historyviewer.data.repository.ConversationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val sessions: List<ConversationSession> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSearch: Boolean = false,
    val searchQuery: String = "",
    val refreshMessage: String? = null
)

class MainViewModel(
    private val repository: ConversationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    
    init {
        loadSessions()
    }
    
    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                combine(
                    repository.getAllSessions(),
                    _searchQuery
                ) { sessions, query ->
                    if (query.isEmpty()) {
                        sessions
                    } else {
                        sessions.filter { session ->
                            session.title.contains(query, ignoreCase = true) ||
                            session.projectPath.contains(query, ignoreCase = true)
                        }
                    }
                }.collect { filteredSessions ->
                    _uiState.update { 
                        it.copy(
                            sessions = filteredSessions,
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
    
    fun refreshData(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val claudePath = "/storage/emulated/0/.claude/projects"
                val result = repository.refreshFromFiles(claudePath)
                
                if (result.isSuccess) {
                    val entriesCount = result.getOrNull() ?: 0
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            refreshMessage = "Loaded $entriesCount entries"
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to refresh data"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to refresh data"
                    )
                }
            }
        }
    }
    
    fun toggleSearch() {
        _uiState.update { 
            it.copy(showSearch = !it.showSearch)
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update { it.copy(searchQuery = "") }
    }
    
    fun clearRefreshMessage() {
        _uiState.update { it.copy(refreshMessage = null) }
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = ConversationDatabase.getDatabase(context)
            val parser = ClaudeHistoryParser()
            val repository = ConversationRepository(database.conversationDao(), parser)
            return MainViewModel(repository) as T
        }
    }
}