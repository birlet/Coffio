package com.example.coffio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.ui.components.SelectionDropdown
import com.example.coffio.ui.i18n.LocalStrings
import com.example.coffio.ui.viewmodel.ChartsViewModel
import com.example.coffio.ui.viewmodel.ConsumptionData
import com.example.coffio.ui.viewmodel.RegressionLine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartsViewModel = viewModel()
) {
    val coffees by viewModel.coffees.collectAsState()
    val sieves by viewModel.sieves.collectAsState()
    val brewsBySieve by viewModel.brewsBySieve.collectAsState()
    val regressionLines by viewModel.regressionLines.collectAsState()
    val consumption by viewModel.consumptionState.collectAsState()
    val totalConsumption by viewModel.totalConsumptionState.collectAsState()
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.chartsTitle) },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SelectionDropdown(
                label = strings.selectCoffee,
                options = coffees.map { it.name },
                selectedOption = viewModel.selectedCoffee?.name ?: "",
                onOptionSelected = { name ->
                    coffees.find { it.name == name }?.let { viewModel.onCoffeeSelected(it) }
                }
            )

            // Consumption Chart (filtered by selected coffee)
            ConsumptionChart(
                consumption = consumption,
                title = strings.consumptionTitle,
                label = viewModel.selectedCoffee?.name ?: strings.allCoffees,
                showTotal = true
            )

            if (viewModel.selectedCoffee == null) {
                EmptyState(strings.selectCoffeeHint)
            } else if (brewsBySieve.isEmpty()) {
                EmptyState(strings.noBrewsForCoffee)
            } else {
                brewsBySieve.forEach { (sieveId, sieveBrews) ->
                    val sieveName = sieves.find { it.id == sieveId }?.name ?: strings.unknownSieve
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${strings.sievePrefix}$sieveName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = strings.grindVsTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            GrindTimeChart(
                                brews = sieveBrews,
                                regressionLine = regressionLines[sieveId],
                                xAxisLabel = strings.brewTimeAxis,
                                yAxisLabel = strings.grindAxis
                            )
                        }
                        
                        Legend()
                        
                        StatSummary(sieveBrews)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GrindTimeChart(
    brews: List<Brew>,
    regressionLine: RegressionLine? = null,
    xAxisLabel: String,
    yAxisLabel: String
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (brews.isEmpty()) return@Canvas

        val maxTime = (brews.maxOf { it.brewTime }.toFloat() / 5).toInt() * 5 + 5f
        val maxGrind = (brews.maxOf { it.grindSize }.toFloat() / 0.2f).toInt() * 0.2f + 0.2f
        
        val width = size.width
        val height = size.height
        val paddingLeft = 60.dp.toPx()
        val paddingBottom = 60.dp.toPx()
        val paddingRight = 20.dp.toPx()
        val paddingTop = 20.dp.toPx()
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        
        val paint = android.graphics.Paint().apply {
            this.color = labelColor
            textSize = 8.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }

        drawContext.canvas.nativeCanvas.apply {
            // Draw Grid and Axis Labels
            // Vertical lines every 5s
            var t = 0f
            while (t <= maxTime) {
                val x = paddingLeft + (t / maxTime) * chartWidth
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(x, paddingTop),
                    end = Offset(x, height - paddingBottom),
                    strokeWidth = 1f
                )
                // Label
                drawText(t.toInt().toString(), x, height - paddingBottom + 15.dp.toPx(), paint)
                t += 5f
            }
            
            // Horizontal lines every 0.2
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            var g = 0f
            while (g <= maxGrind) {
                val y = height - paddingBottom - (g / maxGrind) * chartHeight
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1f
                )
                // Label
                drawText("%.1f".format(g), paddingLeft - 10f, y + 4.dp.toPx(), paint)
                g += 0.2f
            }

            // Draw Axes
            drawLine(
                color = Color.Gray,
                start = Offset(paddingLeft, height - paddingBottom),
                end = Offset(width - paddingRight, height - paddingBottom),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Gray,
                start = Offset(paddingLeft, paddingTop),
                end = Offset(paddingLeft, height - paddingBottom),
                strokeWidth = 2f
            )

            // Titles
            paint.textSize = 10.sp.toPx()
            paint.textAlign = android.graphics.Paint.Align.CENTER
            // X-Axis Title
            drawText(xAxisLabel, paddingLeft + chartWidth / 2, height - 10.dp.toPx(), paint)
            
            // Y-Axis Title
            save()
            rotate(-90f, 15.dp.toPx(), paddingTop + chartHeight / 2)
            drawText(yAxisLabel, 15.dp.toPx(), paddingTop + chartHeight / 2, paint)
            restore()
        }

        // Draw Regression Line
        if (regressionLine != null) {
            val x1 = paddingLeft + (regressionLine.startTime / maxTime) * chartWidth
            val y1 = height - paddingBottom - (regressionLine.startGrind / maxGrind) * chartHeight
            val x2 = paddingLeft + (regressionLine.endTime / maxTime) * chartWidth
            val y2 = height - paddingBottom - (regressionLine.endGrind / maxGrind) * chartHeight
            drawLine(
                color = Color(0xFFFF9800),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 3.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(10.dp.toPx(), 6.dp.toPx())
                )
            )
        }

        // Draw Points
        brews.forEach { brew ->
            val x = paddingLeft + (brew.brewTime.toFloat() / maxTime) * chartWidth
            val y = height - paddingBottom - (brew.grindSize.toFloat() / maxGrind) * chartHeight
            
            val ratio = if (brew.coffeeWeight > 0) brew.actualYield / brew.coffeeWeight else 0.0
            val pointColor = when {
                ratio <= 1.0 -> Color.Blue
                ratio <= 2.0 -> lerpColor(Color.Blue, Color.Green, (ratio - 1.0).toFloat())
                ratio <= 3.0 -> lerpColor(Color.Green, Color.Red, (ratio - 2.0).toFloat())
                else -> Color.Red
            }
            
            drawCircle(
                color = pointColor,
                radius = 8.dp.toPx(),
                center = Offset(x, y)
            )
            // Outline for visibility
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(x, y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

@Composable
fun Legend() {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem("1:1", Color.Blue)
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem("1:2", Color.Green)
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem("1:3", Color.Red)
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(strings.modelLine, Color(0xFFFF9800))
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, shape = CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatSummary(brews: List<Brew>) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(strings.statistics, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(strings.avgTime, "%.0f s".format(brews.map { it.brewTime }.average()))
                StatItem(strings.count, "${brews.size}")
                StatItem(strings.avgRatio, "1:%.1f".format(brews.map { if (it.coffeeWeight > 0) it.actualYield / it.coffeeWeight else 0.0 }.average()))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ConsumptionChart(
    consumption: ConsumptionData,
    title: String,
    label: String,
    showTotal: Boolean = false
) {
    val strings = LocalStrings.current
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val values = buildList {
                add(strings.today to consumption.today)
                add(strings.thisWeek to consumption.thisWeek)
                add(strings.thisMonth to consumption.thisMonth)
                if (showTotal) add(strings.total to consumption.total)
            }
            val maxValue = values.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barCount = values.size
                    val totalWidth = size.width
                    val totalHeight = size.height
                    val barSpacing = 24.dp.toPx()
                    val labelAreaHeight = 40.dp.toPx()
                    val topPadding = 20.dp.toPx()
                    val chartHeight = totalHeight - labelAreaHeight - topPadding
                    val barWidth = (totalWidth - (barCount + 1) * barSpacing) / barCount

                    val paint = android.graphics.Paint().apply {
                        color = labelColor
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    values.forEachIndexed { index, (periodLabel, value) ->
                        val x = barSpacing + index * (barWidth + barSpacing)
                        val barHeight = ((value / maxValue) * chartHeight).toFloat()
                        val barTop = topPadding + chartHeight - barHeight

                        // Bar
                        drawRect(
                            color = barColor,
                            topLeft = Offset(x, barTop),
                            size = Size(barWidth, barHeight)
                        )

                        // Value label above bar
                        drawContext.canvas.nativeCanvas.drawText(
                            "%.1f ${strings.consumptionUnit}".format(value),
                            x + barWidth / 2,
                            barTop - 6.dp.toPx(),
                            paint
                        )

                        // Period label below bar
                        drawContext.canvas.nativeCanvas.drawText(
                            periodLabel,
                            x + barWidth / 2,
                            totalHeight - 8.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }
    }
}
