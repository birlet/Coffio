package com.example.coffio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.BrewWithCoffee
import com.example.coffio.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val brews by viewModel.historyState.collectAsState()
    var brewToEdit by remember { mutableStateOf<BrewWithCoffee?>(null) }
    var brewToDelete by remember { mutableStateOf<Brew?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (brews.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No brews yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(brews) { brewWithCoffee ->
                    BrewItem(
                        brewWithCoffee = brewWithCoffee,
                        onEdit = { brewToEdit = brewWithCoffee },
                        onDelete = { brewToDelete = brewWithCoffee.brew }
                    )
                }
            }
        }
    }

    // Edit Dialog
    brewToEdit?.let { brewWithCoffee ->
        EditBrewDialog(
            brewWithCoffee = brewWithCoffee,
            onDismiss = { brewToEdit = null },
            onConfirm = { updatedBrew ->
                viewModel.updateBrew(updatedBrew)
                brewToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    brewToDelete?.let { brew ->
        AlertDialog(
            onDismissRequest = { brewToDelete = null },
            title = { Text("Delete Brew") },
            text = { Text("Are you sure you want to delete this brew entry?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBrew(brew)
                        brewToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { brewToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BrewItem(
    brewWithCoffee: BrewWithCoffee,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val brew = brewWithCoffee.brew
    val coffee = brewWithCoffee.coffee
    val sieve = brewWithCoffee.sieve
    val drink = brewWithCoffee.drink
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateString = dateFormatter.format(Date(brew.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drink?.name ?: coffee.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val subtitle = if (drink != null) {
                        "${coffee.name} • ${sieve.name}"
                    } else {
                        sieve.name
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailColumn("Grind", "${brew.grindSize}")
                DetailColumn("Yield", "${brew.actualYield}g")
                DetailColumn("Milk", "${brew.milkVolume}ml")
                DetailColumn("Temp", "${brew.temperature}°C")
            }
        }
    }
}

@Composable
fun EditBrewDialog(
    brewWithCoffee: BrewWithCoffee,
    onDismiss: () -> Unit,
    onConfirm: (Brew) -> Unit
) {
    val brew = brewWithCoffee.brew
    var temperature by remember { mutableStateOf(brew.temperature.toString()) }
    var grindSize by remember { mutableStateOf(brew.grindSize.toString()) }
    var actualYield by remember { mutableStateOf(brew.actualYield.toString()) }
    var brewTime by remember { mutableStateOf(brew.brewTime.toString()) }
    var coffeeWeight by remember { mutableStateOf(brew.coffeeWeight.toString()) }
    var milkVolume by remember { mutableStateOf(brew.milkVolume.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Brew") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperature (°C)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = grindSize,
                    onValueChange = { grindSize = it },
                    label = { Text("Grind Size") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = actualYield,
                    onValueChange = { actualYield = it },
                    label = { Text("Actual Yield (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = brewTime,
                    onValueChange = { brewTime = it },
                    label = { Text("Brew Time (s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = coffeeWeight,
                    onValueChange = { coffeeWeight = it },
                    label = { Text("Coffee Weight (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = milkVolume,
                    onValueChange = { milkVolume = it },
                    label = { Text("Milk Volume (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedBrew = brew.copy(
                        temperature = temperature.toDoubleOrNull() ?: brew.temperature,
                        grindSize = grindSize.toDoubleOrNull() ?: brew.grindSize,
                        actualYield = actualYield.toDoubleOrNull() ?: brew.actualYield,
                        brewTime = brewTime.toIntOrNull() ?: brew.brewTime,
                        coffeeWeight = coffeeWeight.toDoubleOrNull() ?: brew.coffeeWeight,
                        milkVolume = milkVolume.toDoubleOrNull() ?: brew.milkVolume
                    )
                    onConfirm(updatedBrew)
                }
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

@Composable
fun DetailColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
