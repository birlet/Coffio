package com.example.coffio.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
                    label = strings.coffeeWeightLabel,
                    value = viewModel.coffeeWeight,
                    onValueChange = { viewModel.coffeeWeight = it },
                    modifier = Modifier.weight(1f)
                )
                BrewInput(
                    label = strings.targetYieldLabel,
                    value = viewModel.targetYield,
                    onValueChange = {
                        viewModel.targetYield = it
                        viewModel.updateCalculatedGrindSize()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrewInput(
                    label = strings.tempLabel,
                    value = viewModel.temperature,
                    onValueChange = { viewModel.temperature = it },
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

            // Calculated Grind Size (read-only) — light green highlight
            viewModel.calculatedGrindSize?.let { calculated ->
                OutlinedTextField(
                    value = calculated,
                    onValueChange = {},
                    label = { Text(strings.calculatedGrindSizeLabel) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color(0xFFD4EDDA),
                        focusedContainerColor = androidx.compose.ui.graphics.Color(0xFFD4EDDA),
                        disabledContainerColor = androidx.compose.ui.graphics.Color(0xFFD4EDDA),
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                        focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Post-Brewing
            Text(strings.result, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Grind Size Slider
            val grindValue = viewModel.grindSize.toFloatOrNull() ?: 0f
            Column {
                Text(
                    text = "${strings.grindSizeLabel}: ${"%.1f".format(grindValue)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = grindValue,
                    onValueChange = { viewModel.grindSize = "%.1f".format(it) },
                    valueRange = 0f..50f,
                    steps = ((50f - 0f) / 0.1f).toInt() - 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                WheelPicker(
                    label = strings.brewTimeLabel,
                    value = viewModel.resultBrewTime.toIntOrNull()
                        ?: viewModel.desiredBrewTime.toIntOrNull() ?: 25,
                    range = 0..120,
                    step = 1,
                    onValueChange = { viewModel.resultBrewTime = it.toString() },
                    modifier = Modifier.weight(1f)
                )
                WheelPicker(
                    label = strings.actualYieldLabel,
                    value = viewModel.actualYield.toIntOrNull()
                        ?: viewModel.targetYield.toIntOrNull() ?: 36,
                    range = 0..200,
                    step = 1,
                    onValueChange = { viewModel.actualYield = it.toString() },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    label: String,
    value: Int,
    range: IntRange,
    step: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(range, step) { range.step(step).toList() }
    val initialIndex = remember(value, items) { items.indexOf(value).coerceAtLeast(0) }
    val itemHeight = 40.dp
    val visibleCount = 3
    val halfVisible = visibleCount / 2

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // The centered data index is firstVisibleItemIndex (which is offset by the padding items)
    // Since we prepend `halfVisible` spacer items, the real data index at center is:
    // firstVisibleItemIndex + halfVisible - halfVisible = firstVisibleItemIndex
    // But firstVisibleItemIndex includes the spacer items, so the actual data index
    // at center = firstVisibleItemIndex (0-based in the full list including spacers)
    // minus halfVisible spacer items = data index.
    // With halfVisible=1 spacer at top: center data index = firstVisibleItemIndex
    // (the spacer is index 0 in lazy list, data starts at index 1)
    // Actually the centered *lazy list* index = firstVisibleItemIndex + halfVisible
    // The data index = (centered lazy list index) - halfVisible (spacer count)
    // = firstVisibleItemIndex
    val centeredDataIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(items.indices) }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val idx = listState.firstVisibleItemIndex.coerceIn(items.indices)
            onValueChange(items[idx])
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .height(itemHeight * visibleCount)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top spacer so the first real item can be centered
                items(halfVisible) {
                    Box(modifier = Modifier.height(itemHeight))
                }
                items(items.size) { index ->
                    val isCenter = index == centeredDataIndex
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .alpha(if (isCenter) 1f else 0.3f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = items[index].toString(),
                            style = if (isCenter) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // Bottom spacer so the last real item can be centered
                items(halfVisible) {
                    Box(modifier = Modifier.height(itemHeight))
                }
            }

            // Selection highlight
            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
