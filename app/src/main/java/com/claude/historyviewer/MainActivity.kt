package com.claude.historyviewer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.claude.historyviewer.ui.screens.ConversationDetailScreen
import com.claude.historyviewer.ui.screens.MainScreen
import com.claude.historyviewer.ui.theme.ClaudeHistoryViewerTheme
import com.claude.historyviewer.ui.viewmodel.ConversationDetailViewModel
import com.claude.historyviewer.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
            // Permission granted, continue with app
        } else {
            // Permission denied, show explanation or exit
        }
    }
    
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { 
        // Handle result if needed
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        setContent {
            ClaudeHistoryViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            val viewModel: MainViewModel = viewModel(
                                factory = MainViewModel.Factory(this@MainActivity)
                            )
                            
                            MainScreen(
                                onSessionClick = { sessionId ->
                                    navController.navigate("conversation/$sessionId")
                                },
                                viewModel = viewModel
                            )
                        }
                        
                        composable("conversation/{sessionId}") { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                            val viewModel: ConversationDetailViewModel = viewModel(
                                factory = ConversationDetailViewModel.Factory(this@MainActivity)
                            )
                            
                            ConversationDetailScreen(
                                sessionId = sessionId,
                                onBackClick = { navController.popBackStack() },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                manageStorageLauncher.launch(intent)
            }
        } else {
            // Android 10 and below
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            
            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                storagePermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }
}