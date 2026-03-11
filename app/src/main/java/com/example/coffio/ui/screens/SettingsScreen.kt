package com.example.coffio.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coffio.data.local.entities.Drink
import com.example.coffio.data.local.entities.Sieve
import com.example.coffio.ui.components.SelectionDropdown
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
    val drinks by viewModel.drinks.collectAsState()
    val sieves by viewModel.sieves.collectAsState()

    var showAddDrinkDialog by remember { mutableStateOf(false) }
    var drinkToEdit by remember { mutableStateOf<Drink?>(null) }

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

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportDrinks(it) }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importDrinks(it) }
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDrinkDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Drink")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Drink Editor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (drinks.isEmpty()) {
                item {
                    Text(
                        "No drinks added yet. Click + to add your first drink.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(drinks) { drink ->
                DrinkItem(
                    drink = drink,
                    sieveName = sieves.find { it.id == drink.defaultSieveId }?.name ?: "None",
                    onEdit = { drinkToEdit = drink },
                    onDelete = { viewModel.deleteDrink(drink) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { jsonExportLauncher.launch("drinks_backup.json") },
                        modifier = Modifier.weight(1f),
                        enabled = uiState !is SettingsUiState.Loading
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Export JSON", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { jsonImportLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState !is SettingsUiState.Loading
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Import JSON", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Database Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { exportLauncher.launch("coffio_backup.csv") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is SettingsUiState.Loading
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Full DB (CSV)")
                    }

                    Button(
                        onClick = { importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is SettingsUiState.Loading
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import Full DB (CSV)")
                    }

                    Text(
                        text = "Note: CSV files are for full backups (Coffees, Sieves, Brews). JSON files are specifically for your Drink definitions and their settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    if (showAddDrinkDialog) {
        DrinkDialog(
            title = "New Drink",
            sieves = sieves,
            onDismiss = { showAddDrinkDialog = false },
            onConfirm = { name, sieveId, temp, weight, yield, grind, pressure, milk ->
                viewModel.addDrink(
                    Drink(
                        name = name,
                        defaultSieveId = sieveId,
                        defaultTemperature = temp,
                        defaultCoffeeWeight = weight,
                        defaultTargetYield = yield,
                        defaultGrindSize = grind,
                        defaultTamperPressure = pressure,
                        defaultMilkVolume = milk
                    )
                )
                showAddDrinkDialog = false
            }
        )
    }

    drinkToEdit?.let { drink ->
        DrinkDialog(
            title = "Edit Drink",
            initialDrink = drink,
            sieves = sieves,
            onDismiss = { drinkToEdit = null },
            onConfirm = { name, sieveId, temp, weight, yield, grind, pressure, milk ->
                viewModel.updateDrink(
                    drink.copy(
                        name = name,
                        defaultSieveId = sieveId,
                        defaultTemperature = temp,
                        defaultCoffeeWeight = weight,
                        defaultTargetYield = yield,
                        defaultGrindSize = grind,
                        defaultTamperPressure = pressure,
                        defaultMilkVolume = milk
                    )
                )
                drinkToEdit = null
            }
        )
    }
}

@Composable
fun DrinkItem(
    drink: Drink,
    sieveName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(drink.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Sieve: $sieveName | Temp: ${drink.defaultTemperature}°C",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun DrinkDialog(
    title: String,
    sieves: List<Sieve>,
    initialDrink: Drink? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Double, Double, Double, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(initialDrink?.name ?: "") }
    var selectedSieveId by remember { mutableStateOf(initialDrink?.defaultSieveId) }
    var temp by remember { mutableStateOf(initialDrink?.defaultTemperature?.toString() ?: "93.0") }
    var weight by remember { mutableStateOf(initialDrink?.defaultCoffeeWeight?.toString() ?: "18.0") }
    var yield by remember { mutableStateOf(initialDrink?.defaultTargetYield?.toString() ?: "36.0") }
    var grind by remember { mutableStateOf(initialDrink?.defaultGrindSize?.toString() ?: "2.0") }
    var pressure by remember { mutableStateOf(initialDrink?.defaultTamperPressure?.toString() ?: "15.0") }
    var milk by remember { mutableStateOf(initialDrink?.defaultMilkVolume?.toString() ?: "0.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                
                SelectionDropdown(
                    label = "Default Sieve",
                    options = listOf("None") + sieves.map { it.name },
                    selectedOption = sieves.find { it.id == selectedSieveId }?.name ?: "None",
                    onOptionSelected = { name ->
                        selectedSieveId = if (name == "None") null else sieves.find { it.name == name }?.id
                    }
                )

                OutlinedTextField(value = temp, onValueChange = { temp = it }, label = { Text("Temp (°C)") }, singleLine = true)
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Coffee Weight (g)") }, singleLine = true)
                OutlinedTextField(value = yield, onValueChange = { yield = it }, label = { Text("Target Yield (g)") }, singleLine = true)
                OutlinedTextField(value = grind, onValueChange = { grind = it }, label = { Text("Grind Size") }, singleLine = true)
                OutlinedTextField(value = pressure, onValueChange = { pressure = it }, label = { Text("Tamper Pressure (kg)") }, singleLine = true)
                OutlinedTextField(value = milk, onValueChange = { milk = it }, label = { Text("Milk Volume (ml)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        selectedSieveId,
                        temp.toDoubleOrNull() ?: 93.0,
                        weight.toDoubleOrNull() ?: 18.0,
                        yield.toDoubleOrNull() ?: 36.0,
                        grind.toDoubleOrNull() ?: 2.0,
                        pressure.toDoubleOrNull() ?: 15.0,
                        milk.toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
