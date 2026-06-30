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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coffio.data.local.entities.Coffee
import com.example.coffio.data.local.entities.Drink
import com.example.coffio.data.local.entities.Sieve
import com.example.coffio.ui.components.SelectionDropdown
import com.example.coffio.ui.i18n.AppLanguage
import com.example.coffio.ui.i18n.LocalStrings
import com.example.coffio.ui.viewmodel.SettingsUiState
import com.example.coffio.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val drinks by viewModel.drinks.collectAsState()
    val sieves by viewModel.sieves.collectAsState()
    val coffees by viewModel.coffees.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncServer by viewModel.syncServer.collectAsState()
    var syncServerInput by remember(syncServer) { mutableStateOf(syncServer) }

    var showAddDrinkDialog by remember { mutableStateOf(false) }
    var drinkToEdit by remember { mutableStateOf<Drink?>(null) }
    var showResetDbDialog by remember { mutableStateOf(false) }
    var showClearServerDbDialog by remember { mutableStateOf(false) }

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
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDrinkDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = strings.newDrink)
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
                    strings.language,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentLanguage == AppLanguage.ENGLISH,
                        onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                        label = { Text(strings.languageEnglish) }
                    )
                    FilterChip(
                        selected = currentLanguage == AppLanguage.GERMAN,
                        onClick = { viewModel.setLanguage(AppLanguage.GERMAN) },
                        label = { Text(strings.languageGerman) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Text(
                    strings.syncSettings,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = syncServerInput,
                    onValueChange = {
                        syncServerInput = it
                        viewModel.setSyncServer(it)
                    },
                    label = { Text(strings.syncServerIpLabel) },
                    placeholder = { Text(strings.syncServerIpHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.syncEnabledLabel, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = { viewModel.setSyncEnabled(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.runSyncNow() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = syncEnabled && syncServerInput.isNotBlank() && uiState !is SettingsUiState.Loading
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.syncNow)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showClearServerDbDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = syncEnabled && syncServerInput.isNotBlank() && uiState !is SettingsUiState.Loading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.clearServerDb)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Text(
                    strings.drinkEditor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (drinks.isEmpty()) {
                item {
                    Text(
                        strings.noDrinksYet,
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
                        Text(strings.exportJson, style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { jsonImportLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState !is SettingsUiState.Loading
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.importJson, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    strings.databaseManagement,
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
                        Text(strings.exportCsv)
                    }

                    Button(
                        onClick = { importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is SettingsUiState.Loading
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.importCsv)
                    }

                    OutlinedButton(
                        onClick = { showResetDbDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is SettingsUiState.Loading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.resetDb)
                    }

                    Text(
                        text = strings.csvNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    if (showResetDbDialog) {
        AlertDialog(
            onDismissRequest = { showResetDbDialog = false },
            title = { Text(strings.resetDbTitle) },
            text = { Text(strings.resetDbWarning) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetDatabase()
                        showResetDbDialog = false
                    }
                ) {
                    Text(strings.confirmResetDb, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDbDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showClearServerDbDialog) {
        AlertDialog(
            onDismissRequest = { showClearServerDbDialog = false },
            title = { Text(strings.clearServerDbTitle) },
            text = { Text(strings.clearServerDbWarning) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearServerDb()
                        showClearServerDbDialog = false
                    }
                ) {
                    Text(strings.confirmClearServerDb, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearServerDbDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showAddDrinkDialog) {
        DrinkDialog(
            title = strings.newDrink,
            sieves = sieves,
            coffees = coffees,
            strings = strings,
            onDismiss = { showAddDrinkDialog = false },
            onConfirm = { name, sieveId, coffeeId, temp, weight, yield, grind, desiredTime, pressure, milk, visible ->
                viewModel.addDrink(
                    Drink(
                        name = name,
                        defaultSieveId = sieveId,
                        defaultCoffeeId = coffeeId,
                        defaultTemperature = temp,
                        defaultCoffeeWeight = weight,
                        defaultTargetYield = yield,
                        defaultGrindSize = grind,
                        defaultDesiredTime = desiredTime,
                        defaultTamperPressure = pressure,
                        defaultMilkVolume = milk,
                        isVisible = visible
                    )
                )
                showAddDrinkDialog = false
            }
        )
    }

    drinkToEdit?.let { drink ->
        DrinkDialog(
            title = strings.editDrink,
            initialDrink = drink,
            sieves = sieves,
            coffees = coffees,
            strings = strings,
            onDismiss = { drinkToEdit = null },
            onConfirm = { name, sieveId, coffeeId, temp, weight, yield, grind, desiredTime, pressure, milk, visible ->
                viewModel.updateDrink(
                    drink.copy(
                        name = name,
                        defaultSieveId = sieveId,
                        defaultCoffeeId = coffeeId,
                        defaultTemperature = temp,
                        defaultCoffeeWeight = weight,
                        defaultTargetYield = yield,
                        defaultGrindSize = grind,
                        defaultDesiredTime = desiredTime,
                        defaultTamperPressure = pressure,
                        defaultMilkVolume = milk,
                        isVisible = visible
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
                    "${LocalStrings.current.sievePrefix}$sieveName | Temp: ${drink.defaultTemperature}°C",
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
            }        }
    }
}

@Composable
fun DrinkDialog(
    title: String,
    sieves: List<Sieve>,
    coffees: List<Coffee>,
    strings: com.example.coffio.ui.i18n.AppStrings,
    initialDrink: Drink? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Long?, Double, Double, Double, Double, Double, Double, Double, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialDrink?.name ?: "") }
    var selectedSieveId by remember { mutableStateOf(initialDrink?.defaultSieveId) }
    var selectedCoffeeId by remember { mutableStateOf(initialDrink?.defaultCoffeeId) }
    var isVisible by remember { mutableStateOf(initialDrink?.isVisible ?: true) }
    var temp by remember { mutableStateOf(initialDrink?.defaultTemperature?.toString() ?: "93.0") }
    var weight by remember { mutableStateOf(initialDrink?.defaultCoffeeWeight?.toString() ?: "18.0") }
    var yield by remember { mutableStateOf(initialDrink?.defaultTargetYield?.toString() ?: "36.0") }
    var grind by remember { mutableStateOf(initialDrink?.defaultGrindSize?.toString() ?: "2.0") }
    var desiredTime by remember { mutableStateOf(initialDrink?.defaultDesiredTime?.toString() ?: "25.0") }
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.name) }, singleLine = true)
                
                SelectionDropdown(
                    label = strings.defaultSieve,
                    options = listOf(strings.none) + sieves.map { it.name },
                    selectedOption = sieves.find { it.id == selectedSieveId }?.name ?: strings.none,
                    onOptionSelected = { selectedName ->
                        selectedSieveId = if (selectedName == strings.none) null else sieves.find { it.name == selectedName }?.id
                    }
                )

                SelectionDropdown(
                    label = strings.defaultCoffee,
                    options = listOf(strings.none) + coffees.map { it.name },
                    selectedOption = coffees.find { it.id == selectedCoffeeId }?.name ?: strings.none,
                    onOptionSelected = { selectedName ->
                        selectedCoffeeId = if (selectedName == strings.none) null else coffees.find { it.name == selectedName }?.id
                    }
                )

                OutlinedTextField(value = temp, onValueChange = { temp = it }, label = { Text(strings.tempField) }, singleLine = true)
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text(strings.coffeeWeightField) }, singleLine = true)
                OutlinedTextField(value = yield, onValueChange = { yield = it }, label = { Text(strings.targetYieldField) }, singleLine = true)
                OutlinedTextField(value = grind, onValueChange = { grind = it }, label = { Text(strings.grindSizeField) }, singleLine = true)
                OutlinedTextField(value = desiredTime, onValueChange = { desiredTime = it }, label = { Text(strings.desiredTimeField) }, singleLine = true)
                OutlinedTextField(value = pressure, onValueChange = { pressure = it }, label = { Text(strings.tamperPressureField) }, singleLine = true)
                OutlinedTextField(value = milk, onValueChange = { milk = it }, label = { Text(strings.milkVolumeField) }, singleLine = true)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isVisible,
                        onCheckedChange = { isVisible = it }
                    )
                    Text(strings.visibleInHome, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        selectedSieveId,
                        selectedCoffeeId,
                        temp.toDoubleOrNull() ?: 93.0,
                        weight.toDoubleOrNull() ?: 18.0,
                        yield.toDoubleOrNull() ?: 36.0,
                        grind.toDoubleOrNull() ?: 2.0,
                        desiredTime.toDoubleOrNull() ?: 25.0,
                        pressure.toDoubleOrNull() ?: 15.0,
                        milk.toDoubleOrNull() ?: 0.0,
                        isVisible
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
