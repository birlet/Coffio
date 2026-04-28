package com.example.coffio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coffio.ui.components.SelectionDropdown
import com.example.coffio.ui.i18n.LocalStrings
import com.example.coffio.ui.viewmodel.BrewingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewingScreen(
    drinkId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrewingViewModel = viewModel()
) {
    val coffees by viewModel.coffees.collectAsState()
    val sieves by viewModel.sieves.collectAsState()
    val strings = LocalStrings.current

    var showAddCoffeeDialog by remember { mutableStateOf(false) }
    var showAddSieveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(drinkId) {
        viewModel.initialize(drinkId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(viewModel.selectedDrink?.name ?: strings.brewMenu) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Coffee Selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectionDropdown(
                    label = strings.coffee,
                    options = coffees.map { it.name },
                    selectedOption = viewModel.selectedCoffee?.name ?: "",
                    onOptionSelected = { name ->
                        viewModel.selectedCoffee = coffees.find { it.name == name }
                        viewModel.updateCalculatedGrindSize()
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAddCoffeeDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = strings.addCoffee)
                }
            }

            // Sieve Selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectionDropdown(
                    label = strings.sieve,
                    options = sieves.map { it.name },
                    selectedOption = viewModel.selectedSieve?.name ?: "",
                    onOptionSelected = { name ->
                        viewModel.selectedSieve = sieves.find { it.name == name }
                        viewModel.updateCalculatedGrindSize()
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAddSieveDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = strings.addSieve)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Brewing Parameters
            Text(strings.brewingParameters, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrewInput(
                    label = strings.tempLabel,
                    value = viewModel.temperature,
                    onValueChange = { viewModel.temperature = it },
                    modifier = Modifier.weight(1f)
                )
                BrewInput(
                    label = strings.coffeeWeightLabel,
                    value = viewModel.coffeeWeight,
                    onValueChange = { viewModel.coffeeWeight = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrewInput(
                    label = strings.targetYieldLabel,
                    value = viewModel.targetYield,
                    onValueChange = {
                        viewModel.targetYield = it
                        viewModel.updateCalculatedGrindSize()
                    },
                    modifier = Modifier.weight(1f)
                )
                BrewInput(
                    label = strings.grindSizeLabel,
                    value = viewModel.grindSize,
                    onValueChange = { viewModel.grindSize = it },
                    modifier = Modifier.weight(1f)
                )
            }

            // Calculated Grind Size (read-only)
            viewModel.calculatedGrindSize?.let { calculated ->
                OutlinedTextField(
                    value = calculated,
                    onValueChange = {},
                    label = { Text(strings.calculatedGrindSizeLabel) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrewInput(
                    label = strings.tamperLabel,
                    value = viewModel.tamperPressure,
                    onValueChange = { viewModel.tamperPressure = it },
                    modifier = Modifier.weight(1f)
                )
                BrewInput(
                    label = strings.milkLabel,
                    value = viewModel.milkVolume,
                    onValueChange = { viewModel.milkVolume = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrewInput(
                    label = strings.desiredBrewTimeLabel,
                    value = viewModel.desiredBrewTime,
                    onValueChange = {
                        viewModel.desiredBrewTime = it
                        viewModel.updateCalculatedGrindSize()
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Post-Brewing
            Text(strings.result, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrewInput(
                    label = strings.brewTimeLabel,
                    value = viewModel.resultBrewTime,
                    onValueChange = { viewModel.resultBrewTime = it },
                    modifier = Modifier.weight(1f)
                )
                BrewInput(
                    label = strings.actualYieldLabel,
                    value = viewModel.actualYield,
                    onValueChange = { viewModel.actualYield = it },
                    placeholder = viewModel.targetYield,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveBrew(onNavigateBack) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = viewModel.selectedCoffee != null && viewModel.selectedSieve != null
            ) {
                Text(strings.done, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showAddCoffeeDialog) {
        AddItemDialog(
            title = strings.newCoffee,
            confirmLabel = strings.add,
            cancelLabel = strings.cancel,
            nameLabel = strings.name,
            onDismiss = { showAddCoffeeDialog = false },
            onConfirm = { name ->
                viewModel.addCoffee(name)
                showAddCoffeeDialog = false
            }
        )
    }

    if (showAddSieveDialog) {
        AddItemDialog(
            title = strings.newSieve,
            confirmLabel = strings.add,
            cancelLabel = strings.cancel,
            nameLabel = strings.name,
            onDismiss = { showAddSieveDialog = false },
            onConfirm = { name ->
                viewModel.addSieve(name)
                showAddSieveDialog = false
            }
        )
    }
}

@Composable
fun BrewInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.toDoubleOrNull() != null || input.endsWith(".")) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true
    )
}

@Composable
fun AddItemDialog(
    title: String,
    confirmLabel: String,
    cancelLabel: String,
    nameLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(nameLabel) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        }
    )
}
