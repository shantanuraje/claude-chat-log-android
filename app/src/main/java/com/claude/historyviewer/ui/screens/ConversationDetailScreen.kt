package com.claude.historyviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claude.historyviewer.ui.components.MessageCard
import com.claude.historyviewer.ui.viewmodel.ConversationDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    sessionId: String,
    onBackClick: () -> Unit,
    viewModel: ConversationDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(sessionId) {
        viewModel.loadConversation(sessionId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversation Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleMessageTypeFilter() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                    
                    IconButton(
                        onClick = { /* TODO: Export functionality */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Text(
                                text = "Error loading conversation",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            
                            Text(
                                text = uiState.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                
                uiState.messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "No messages",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Text(
                                text = "No messages found",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                
                else -> {
                    Column {
                        // Filter chips (if shown)
                        if (uiState.showFilters) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.availableMessageTypes) { messageType ->
                                    FilterChip(
                                        onClick = { viewModel.toggleMessageTypeFilter(messageType) },
                                        label = { Text(messageType.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        selected = messageType in uiState.activeFilters
                                    )
                                }
                            }
                        }
                        
                        // Messages
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.messages,
                                key = { it.uuid }
                            ) { message ->
                                MessageCard(entry = message)
                            }
                        }
                    }
                }
            }
        }
    }
}