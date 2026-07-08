package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Medication
import com.example.data.MedicationLog
import com.example.viewmodel.MediViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MediViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val trendLogs by viewModel.last30DaysLogs.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var filterStatus by remember { mutableStateOf<Boolean?>(null) } // null = Hepsi, true = Aldıklarım, false = Almadıklarım
    val tabs = listOf("Geçmiş", "Raporlar & Trendler")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Geçmiş ve Raporlar", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Açık Tema" else "Koyu Tema"
                        )
                    }
                }
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
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (selectedTabIndex == 0) {
                    // History Tab
                    if (allLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Henüz geçmiş kayıt bulunmamaktadır.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val filteredLogs = remember(allLogs, filterStatus) {
                            if (filterStatus == null) allLogs else allLogs.filter { it.isTaken == filterStatus }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Filter buttons row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChipItem(
                                    selected = filterStatus == null,
                                    label = "Hepsi",
                                    onClick = { filterStatus = null }
                                )
                                FilterChipItem(
                                    selected = filterStatus == true,
                                    label = "Aldıklarım",
                                    onClick = { filterStatus = true }
                                )
                                FilterChipItem(
                                    selected = filterStatus == false,
                                    label = "Almadıklarım",
                                    onClick = { filterStatus = false }
                                )
                            }

                            if (filteredLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Seçilen filtreye uygun kayıt bulunmamaktadır.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize().weight(1f)
                                ) {
                                    val groupedLogs = filteredLogs.groupBy { it.date }.toSortedMap(reverseOrder())
                                    groupedLogs.forEach { (date, logs) ->
                                        item {
                                            Text(
                                                text = date,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                            )
                                        }
                                        val sortedLogs = logs.sortedByDescending { log ->
                                            val med = medications.find { it.id == log.medicationId }
                                            log.timestamp ?: med?.time ?: "00:00"
                                        }
                                        items(sortedLogs) { log ->
                                            val med = medications.find { it.id == log.medicationId }
                                            val medName = med?.name ?: "Bilinmeyen İlaç"
                                            val statusText = if (log.isTaken) "Aldım" else "Almadım"
                                            val actualTime = log.timestamp ?: med?.time ?: "--:--"

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (log.isTaken) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = 1.dp,
                                                    color = if (log.isTaken) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(16.dp)
                                                        .fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (log.isTaken) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                            contentDescription = statusText,
                                                            tint = if (log.isTaken) Color(0xFF4CAF50) else Color(0xFFE57373),
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                        
                                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Text(
                                                                text = medName,
                                                                fontWeight = FontWeight.Bold,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (med != null) {
                                                                Text(
                                                                    text = "Doz: ${med.dosage}",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Schedule,
                                                                        contentDescription = "Planlanan",
                                                                        tint = MaterialTheme.colorScheme.outline,
                                                                        modifier = Modifier.size(12.dp)
                                                                    )
                                                                    Text(
                                                                        text = "Planlanan: ${med?.time ?: "--:--"}",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                                
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Schedule,
                                                                        contentDescription = "Uygulama",
                                                                        tint = if (log.isTaken) Color(0xFF4CAF50) else Color(0xFFE57373),
                                                                        modifier = Modifier.size(12.dp)
                                                                    )
                                                                    Text(
                                                                        text = "İşlem Saati: $actualTime",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = if (log.isTaken) Color(0xFF388E3C) else Color(0xFFD32F2F)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    
                                                    Surface(
                                                        color = if (log.isTaken) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFE57373).copy(alpha = 0.15f),
                                                        contentColor = if (log.isTaken) Color(0xFF388E3C) else Color(0xFFD32F2F),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            width = 1.dp,
                                                            color = if (log.isTaken) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFE57373).copy(alpha = 0.5f)
                                                        )
                                                    ) {
                                                        Text(
                                                            text = statusText,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Reports and Trends Tab (Fully scrollable & gorgeous)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // 1. Overall Compliance Rate Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Genel İlaç Uyum Oranı",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val totalLogsCount = allLogs.size
                                val takenLogsCount = allLogs.count { it.isTaken }
                                val missedLogsCount = totalLogsCount - takenLogsCount

                                OverallComplianceDonutChart(
                                    takenCount = takenLogsCount,
                                    missedCount = missedLogsCount
                                )
                            }
                        }

                        // 2. Medication Status Report Comparison Chart
                        if (medications.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                MedicationsComparisonChart(
                                    medications = medications,
                                    allLogs = allLogs
                                )
                            }
                        }

                        // 3. Daily Intake Trend Log Chart (Son 30 Gün)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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

                                Text(
                                    "Günlük Alım Grafiği (Son 30 Gün)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val barColor = MaterialTheme.colorScheme.primary
                                val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                                var animationPlayed by remember { mutableStateOf(false) }
                                val animationProgress by animateFloatAsState(
                                    targetValue = if (animationPlayed) 1f else 0f,
                                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                                )
                                LaunchedEffect(Unit) {
                                    animationPlayed = true
                                }

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                ) {
                                    val canvasWidth = size.width
                                    val canvasHeight = size.height
                                    val barWidth = canvasWidth / (values.size * 1.6f)
                                    val spacing = barWidth * 0.6f

                                    values.forEachIndexed { index, value ->
                                        val x = index * (barWidth + spacing)
                                        val barHeight = (value.toFloat() / maxVal) * canvasHeight * animationProgress
                                        val y = canvasHeight - barHeight

                                        // Draw background placeholder bar
                                        drawRect(
                                            color = backgroundColor,
                                            topLeft = Offset(x, 0f),
                                            size = Size(barWidth, canvasHeight)
                                        )

                                        // Draw actual colored bar
                                        if (barHeight > 0f) {
                                            drawRect(
                                                color = barColor,
                                                topLeft = Offset(x, y),
                                                size = Size(barWidth, barHeight)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Günlük Alınan Doz Sayısı",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                val totalTaken = values.sum()
                                val avgTaken = if (values.isNotEmpty()) totalTaken / values.size.toFloat() else 0f

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Özet Bilgiler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    }
}

@Composable
fun OverallComplianceDonutChart(
    takenCount: Int,
    missedCount: Int,
    modifier: Modifier = Modifier
) {
    val total = takenCount + missedCount
    val complianceRate = if (total > 0) (takenCount.toFloat() / total) * 100f else 0f

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedPercentage by animateFloatAsState(
        targetValue = if (animationPlayed) complianceRate else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
    )

    LaunchedEffect(key1 = takenCount, key2 = missedCount) {
        animationPlayed = true
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                val sizeMin = size.minDimension
                val arcSize = Size(sizeMin - strokeWidth, sizeMin - strokeWidth)
                val topLeftOffset = Offset(strokeWidth / 2, strokeWidth / 2)

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active compliance fill arc
                val sweepAngle = (animatedPercentage / 100f) * 360f
                if (sweepAngle > 0f) {
                    drawArc(
                        color = if (complianceRate >= 80f) Color(0xFF4CAF50) else if (complianceRate >= 50f) Color(0xFFFF9800) else Color(0xFFE57373),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${complianceRate.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Uyum",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "İlaç Uyumu Özeti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50), RoundedCornerShape(2.dp)))
                Text("Alınan Doz: $takenCount gün", style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFFE57373), RoundedCornerShape(2.dp)))
                Text("Kaçırılan Doz: $missedCount gün", style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text(
                text = if (total == 0) "Takip verisi henüz girilmedi."
                       else if (complianceRate >= 85f) "Mükemmel! İlaçlarınızı son derece düzenli kullanıyorsunuz."
                       else if (complianceRate >= 60f) "Güzel, ama biraz daha özenli olabilirsiniz."
                       else "Uyum oranınız oldukça düşük. Hatırlatıcıları kontrol ediniz.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MedicationsComparisonChart(
    medications: List<Medication>,
    allLogs: List<MedicationLog>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "İlaç Bazlı Durum Raporu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        medications.forEach { med ->
            val medLogs = allLogs.filter { it.medicationId == med.id }
            val total = medLogs.size
            val taken = medLogs.count { it.isTaken }
            val missed = total - taken
            val compRate = if (total > 0) (taken.toFloat() / total) * 100f else 0f

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = med.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Alınan: $taken gün • Atlanan: $missed gün",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (total > 0) "${compRate.toInt()}% Uyum" else "Veri Yok",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (compRate >= 80f) Color(0xFF4CAF50) else if (compRate >= 50f) Color(0xFFFF9800) else Color(0xFFE57373)
                    )
                }

                // Custom linear horizontal dual bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                ) {
                    if (total > 0) {
                        val takenRatio = taken.toFloat() / total
                        val missedRatio = missed.toFloat() / total

                        Row(modifier = Modifier.fillMaxSize()) {
                            if (takenRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(takenRatio)
                                        .fillMaxHeight()
                                        .background(
                                            color = Color(0xFF4CAF50),
                                            shape = RoundedCornerShape(
                                                topStart = 4.dp,
                                                bottomStart = 4.dp,
                                                topEnd = if (missedRatio == 0f) 4.dp else 0.dp,
                                                bottomEnd = if (missedRatio == 0f) 4.dp else 0.dp
                                            )
                                        )
                                )
                            }
                            if (missedRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(missedRatio)
                                        .fillMaxHeight()
                                        .background(
                                            color = Color(0xFFE57373),
                                            shape = RoundedCornerShape(
                                                topEnd = 4.dp,
                                                bottomEnd = 4.dp,
                                                topStart = if (takenRatio == 0f) 4.dp else 0.dp,
                                                bottomStart = if (takenRatio == 0f) 4.dp else 0.dp
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        },
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

