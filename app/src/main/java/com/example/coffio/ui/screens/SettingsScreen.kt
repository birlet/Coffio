package com.example.coffio.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coffio.ui.viewmodel.SettingsUiState
import com.example.coffio.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportDatabase(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importDatabase(it) }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SettingsUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            is SettingsUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState is SettingsUiState.Loading) {
                CircularProgressIndicator()
            }

            Button(
                onClick = { exportLauncher.launch("coffio_backup.csv") },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is SettingsUiState.Loading
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Database to CSV")
            }

            Button(
                onClick = { importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is SettingsUiState.Loading
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import Database from CSV")
            }

            Text(
                text = "Note: CSV files can be opened in Excel. Exporting will create a full backup of your coffees, sieves, and brews. Importing will merge the data into your current database.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
