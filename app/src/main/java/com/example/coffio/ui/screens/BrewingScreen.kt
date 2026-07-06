package com.example.coffio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.BrewSource
import com.example.coffio.ui.components.SelectionDropdown
import com.example.coffio.ui.i18n.LocalStrings
import com.example.coffio.ui.viewmodel.BrewingViewModel
import java.util.Locale

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
    val lastBrew = viewModel.lastBrew
    val recentBrews = viewModel.recentBrews

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
                        viewModel.onCoffeeSelected(coffees.find { it.name == name })
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
                        viewModel.onSieveSelected(sieves.find { it.name == name })
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAddSieveDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = strings.addSieve)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Brewing Parameters — collapsible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleBrewParamsExpanded() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.brewingParameters, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (viewModel.brewParamsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = viewModel.brewParamsExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                }
            }

            // Last Brew — collapsible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleLastBrewExpanded() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.lastBrews, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (viewModel.lastBrewExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = viewModel.lastBrewExpanded) {
                if (lastBrew != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WheelPicker(
                            label = "",
                            value = lastBrew.syncKey,
                            items = recentBrews.map { it.syncKey },
                            onValueChange = viewModel::selectLastBrew,
                            itemLabel = { syncKey ->
                                recentBrews.firstOrNull { it.syncKey == syncKey }?.let { brew ->
                                    formatLastBrewWheelLabel(brew)
                                } ?: syncKey
                            },
                            itemHeightDp = 32,
                            visibleItems = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = strings.noLastBrew,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Post-Brewing
            Text(strings.result, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val grindOptions = remember { (0..500).map { String.format(Locale.US, "%.1f", it / 10.0) } }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WheelPicker(
                        label = strings.grindSizeLabel,
                        value = viewModel.calculatedGrindSize
                            ?: viewModel.grindSize.ifEmpty { "0" },
                        items = grindOptions,
                        onValueChange = { viewModel.grindSize = it },
                        highlightValue = viewModel.calculatedGrindSize,
                        modifier = Modifier.weight(1f)
                    )
                    WheelPicker(
                        label = strings.brewTimeLabel,
                        value = viewModel.resultBrewTime.ifEmpty { 
                            viewModel.desiredBrewTime.ifEmpty { "0" }
                        },
                        items = (0..120).map { it.toString() },
                        onValueChange = { viewModel.resultBrewTime = it },
                        modifier = Modifier.weight(1f)
                    )
                    WheelPicker(
                        label = strings.actualYieldLabel,
                        value = viewModel.actualYield.ifEmpty {
                            viewModel.targetYield.ifEmpty { "0" }
                        },
                        items = (0..200).map { it.toString() },
                        onValueChange = { viewModel.actualYield = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.size(12.dp)
                ) {}
                Text(
                    text = strings.legendCalculatedGrindSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            var dataOnly by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = dataOnly,
                    onCheckedChange = { dataOnly = it }
                )
                Text(
                    text = strings.saveDataOnly,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Button(
                onClick = {
                    viewModel.saveBrew(
                        onSuccess = { if (!dataOnly) onNavigateBack() },
                        dataOnly = dataOnly
                    )
                },
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
private fun LastBrewMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
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
    value: String,
    items: List<String>,
    onValueChange: (String) -> Unit,
    highlightValue: String? = null,
    itemLabel: (String) -> String = { it },
    itemColor: (String) -> Color = { Color.Unspecified },
    itemHeightDp: Int = 40,
    visibleItems: Int = 3,
    modifier: Modifier = Modifier
) {
    val itemHeight = itemHeightDp.dp
    val visibleCount = visibleItems
    val halfVisible = visibleCount / 2

    val initialIndex = remember(items, value) {
        val idx = items.indexOf(value)
        if (idx != -1) idx else {
            val dValue = value.toDoubleOrNull()
            if (dValue != null) {
                items.indexOfFirst { it.toDoubleOrNull() == dValue }.coerceAtLeast(0)
            } else 0
        }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Scroll to new value when it changes externally
    LaunchedEffect(value) {
        val targetIndex = items.indexOf(value)
        val resolvedIndex = if (targetIndex != -1) targetIndex else {
            val dValue = value.toDoubleOrNull()
            if (dValue != null) items.indexOfFirst { it.toDoubleOrNull() == dValue } else -1
        }
        if (resolvedIndex != -1) {
            if (resolvedIndex != listState.firstVisibleItemIndex) {
                listState.animateScrollToItem(resolvedIndex)
            }
            // Sync resolved value back so ViewModel always reflects what's displayed
            onValueChange(items[resolvedIndex])
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val idx = listState.firstVisibleItemIndex.coerceIn(items.indices)
            if (items[idx] != value) {
                onValueChange(items[idx])
            }
        }
    }

    val centeredDataIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(items.indices) }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
            maxLines = 1,
            textAlign = TextAlign.Center
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
                items(halfVisible) {
                    Box(modifier = Modifier.height(itemHeight))
                }
                items(items.size) { index ->
                    val isCenter = index == centeredDataIndex
                    val isHighlighted = highlightValue != null && items[index] == highlightValue
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .alpha(if (isCenter) 1f else 0.3f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = itemLabel(items[index]),
                            style = if (isCenter) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                            color = if (isHighlighted) Color(0xFF4CAF50)
                            else itemColor(items[index]),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                items(halfVisible) {
                    Box(modifier = Modifier.height(itemHeight))
                }
            }

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

private fun brewColor(source: BrewSource): Color {
    return when (source) {
        BrewSource.LOCAL -> Color(0xFFE8F5E9)
        BrewSource.REMOTE -> Color(0xFFE3F2FD)
        BrewSource.IMPORTED -> Color(0xFFE0E0E0)
    }
}

private fun brewSourceLabel(source: BrewSource, serverLabel: String, importedLabel: String, ownLabel: String): String {
    return when (source) {
        BrewSource.LOCAL -> ownLabel
        BrewSource.REMOTE -> serverLabel
        BrewSource.IMPORTED -> importedLabel
    }
}

private fun formatLastBrewWheelLabel(brew: Brew): String {
    return buildString {
        append(String.format(Locale.US, "%.1f", brew.grindSize))
        append(" • ")
        append(brew.brewTime)
        append("s • ")
        append(String.format(Locale.US, "%.1f", brew.actualYield))
        append("g")
    }
}
