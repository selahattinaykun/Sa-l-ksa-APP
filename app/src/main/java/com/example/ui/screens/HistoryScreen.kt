package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.MediViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MediViewModel) {
    val medications by viewModel.medications.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val trendLogs by viewModel.last30DaysLogs.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Geçmiş", "Trendler")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Geçmiş ve Trendler") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (selectedTabIndex == 0) {
                    // History Tab
                    if (allLogs.isEmpty()) {
                        Text("Henüz geçmiş kayıt bulunmamaktadır.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val groupedLogs = allLogs.groupBy { it.date }.toSortedMap(reverseOrder())
                            groupedLogs.forEach { (date, logs) ->
                                item {
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                items(logs) { log ->
                                    val med = medications.find { it.id == log.medicationId }
                                    val medName = med?.name ?: "Bilinmeyen İlaç"
                                    val statusText = if (log.isTaken) "Alındı" else "Alınmadı"

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                        border = if (log.isTaken) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (log.isTaken) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(medName, fontWeight = FontWeight.Bold)
                                            Text(statusText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Trends Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -29) // Last 30 days including today

                        val dateCounts = mutableMapOf<String, Int>()
                        for (i in 0 until 30) {
                            dateCounts[dateFormat.format(cal.time)] = 0
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }

                        trendLogs.filter { it.isTaken }.forEach { log ->
                            if (dateCounts.containsKey(log.date)) {
                                dateCounts[log.date] = (dateCounts[log.date] ?: 0) + 1
                            }
                        }

                        val values = dateCounts.values.toList()
                        val maxVal = values.maxOrNull()?.coerceAtLeast(1) ?: 1

                        Text("İlaç Alım Grafiği (Son 30 Gün)", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(24.dp))

                        val barColor = MaterialTheme.colorScheme.primary
                        val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

                        var animationPlayed by remember { mutableStateOf(false) }
                        val animationProgress by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (animationPlayed) 1f else 0f,
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        )
                        LaunchedEffect(Unit) {
                            animationPlayed = true
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val barWidth = canvasWidth / (values.size * 1.5f)
                            val spacing = barWidth * 0.5f

                            values.forEachIndexed { index, value ->
                                val x = index * (barWidth + spacing)
                                val barHeight = (value.toFloat() / maxVal) * canvasHeight * animationProgress
                                val y = canvasHeight - barHeight

                                // Draw background placeholder
                                drawRect(
                                    color = backgroundColor,
                                    topLeft = Offset(x, 0f),
                                    size = Size(barWidth, canvasHeight)
                                )

                                // Draw actual bar
                                drawRect(
                                    color = barColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Günlük Alınan Doz Sayısı", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(32.dp))

                        val totalTaken = values.sum()
                        val avgTaken = if (values.isNotEmpty()) totalTaken / values.size.toFloat() else 0f

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Özet", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Son 30 günde alınan toplam doz: $totalTaken", style = MaterialTheme.typography.bodyLarge)
                                Text("Günlük ortalama doz: ${String.format(Locale.US, "%.1f", avgTaken)}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
