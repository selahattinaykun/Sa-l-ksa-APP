package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.SystemUpdate
import com.example.util.UpdateManager
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Medication
import com.example.data.MedicationLog
import com.example.util.AlarmHelper
import com.example.viewmodel.MediViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke

data class WeekDayInfo(
    val dateStr: String,
    val dayName: String,
    val dayOfMonth: Int,
    val isFuture: Boolean
)

private fun getWeeklyStatus(
    dateStr: String,
    medications: List<Medication>,
    allLogs: List<MedicationLog>,
    isFuture: Boolean
): String {
    if (medications.isEmpty()) return "😐"
    
    val dayLogs = allLogs.filter { it.date == dateStr }
    
    if (isFuture) {
        return "⏳"
    }
    
    val hasNotTaken = dayLogs.any { !it.isTaken }
    val takenCount = dayLogs.count { it.isTaken }
    val isAllTaken = medications.isNotEmpty() && takenCount >= medications.size
    
    return when {
        isAllTaken -> "😊"
        hasNotTaken -> "😢"
        else -> {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (dateStr < todayStr) {
                "😢" // Forgotten
            } else {
                "😐" // Pending today
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MediViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Günlük", "Takvim", "Boy & Kilo")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İlaç Takipçim") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = { UpdateManager.checkUpdate(context, isManualCheck = true) }) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Güncelleme Kontrol Et",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Açık Tema" else "Koyu Tema"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "Yeni İlaç Ekle")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Material 3 TabRow
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> DailyMedicationsTab(
                    viewModel = viewModel,
                    medications = medications,
                    todayLogs = todayLogs,
                    allLogs = allLogs,
                    currentDate = currentDate,
                    onEditClick = onEditClick
                )
                1 -> CalendarTab(
                    viewModel = viewModel,
                    medications = medications,
                    allLogs = allLogs,
                    currentDate = currentDate
                )
                2 -> BmiTab()
            }
        }
    }
}

@Composable
fun DailyMedicationsTab(
    viewModel: MediViewModel,
    medications: List<Medication>,
    todayLogs: List<MedicationLog>,
    allLogs: List<MedicationLog>,
    currentDate: String,
    onEditClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val isAllTakenToday = medications.isNotEmpty() && medications.all { med ->
        todayLogs.any { log -> log.medicationId == med.id && log.isTaken }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Weekly status strip
        WeeklyCalendarStrip(
            selectedDate = currentDate,
            medications = medications,
            allLogs = allLogs,
            onDateSelected = { date -> viewModel.setCurrentDate(date) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Celebratory Card
        if (isAllTakenToday) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎉",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tebrikler!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "İlaçlarınızı aldınız, günlük işlemleriniz tamamlandı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Header showing active date and Return to Today action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tarih: $currentDate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            
            val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
            if (currentDate != todayStr) {
                TextButton(onClick = { viewModel.setCurrentDate(todayStr) }) {
                    Text("Bugüne Dön", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (medications.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Henüz ilaç eklenmedi.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(medications) { med ->
                    val todayLog = todayLogs.find { it.medicationId == med.id }
                    MedicationCard(
                        medication = med,
                        todayLog = todayLog,
                        allLogs = allLogs,
                        onSetStatus = { isTaken -> viewModel.toggleMedicationTaken(med.id, isTaken) },
                        onClearStatus = { viewModel.deleteMedicationLog(med.id) },
                        onEdit = { onEditClick(med.id) },
                        onDelete = {
                            viewModel.deleteMedication(med.id)
                            AlarmHelper.cancelMedicationAlarms(context, med.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyCalendarStrip(
    selectedDate: String,
    medications: List<Medication>,
    allLogs: List<MedicationLog>,
    onDateSelected: (String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember { sdf.format(Date()) }
    
    val weeklyDays = remember {
        val cal = Calendar.getInstance(Locale("tr"))
        cal.firstDayOfWeek = Calendar.MONDAY
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        val diff = if (currentDay == Calendar.SUNDAY) -6 else Calendar.MONDAY - currentDay
        cal.add(Calendar.DAY_OF_YEAR, diff)
        
        (0 until 7).map {
            val dateVal = cal.time
            val dateStr = sdf.format(dateVal)
            val dayName = when (it) {
                0 -> "Pzt"
                1 -> "Sal"
                2 -> "Çar"
                3 -> "Per"
                4 -> "Cum"
                5 -> "Cmt"
                6 -> "Paz"
                else -> ""
            }
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val isFuture = dateVal.time > System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            WeekDayInfo(dateStr, dayName, dayOfMonth, isFuture)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Haftalık Takip",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyDays.forEach { dayInfo ->
                    val isSelected = dayInfo.dateStr == selectedDate
                    val isToday = dayInfo.dateStr == todayStr
                    val face = getWeeklyStatus(dayInfo.dateStr, medications, allLogs, dayInfo.isFuture)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDateSelected(dayInfo.dateStr) }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = dayInfo.dayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else if (isToday) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = dayInfo.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = if (medications.isEmpty()) "—" else face,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarTab(
    viewModel: MediViewModel,
    medications: List<Medication>,
    allLogs: List<MedicationLog>,
    currentDate: String
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var calendarInstance by remember { mutableStateOf(Calendar.getInstance()) }
    
    val currentMonthYearLabel = remember(calendarInstance) {
        val monthName = calendarInstance.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("tr")) ?: ""
        val year = calendarInstance.get(Calendar.YEAR)
        if (monthName.isNotEmpty()) {
            "${monthName.substring(0, 1).uppercase()}${monthName.substring(1)} $year"
        } else {
            "$year"
        }
    }

    val daysInMonth = remember(calendarInstance) {
        val tempCal = calendarInstance.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeekJava = tempCal.get(Calendar.DAY_OF_WEEK)
        val offset = if (firstDayOfWeekJava == Calendar.SUNDAY) 6 else firstDayOfWeekJava - Calendar.MONDAY
        Pair(maxDays, offset)
    }

    val totalDays = daysInMonth.first
    val offsetDays = daysInMonth.second

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Month Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val temp = calendarInstance.clone() as Calendar
                        temp.add(Calendar.MONTH, -1)
                        calendarInstance = temp
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Önceki Ay")
                    }

                    Text(
                        text = currentMonthYearLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = {
                        val temp = calendarInstance.clone() as Calendar
                        temp.add(Calendar.MONTH, 1)
                        calendarInstance = temp
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sonraki Ay")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weekdays Labels
                Row(modifier = Modifier.fillMaxWidth()) {
                    val weekDays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
                    weekDays.forEach { dayName ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid Cells
                val totalCells = totalDays + offsetDays
                val rowsCount = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (r in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                if (cellIndex < offsetDays || cellIndex >= offsetDays + totalDays) {
                                    Box(modifier = Modifier.weight(1f))
                                } else {
                                    val dayNum = cellIndex - offsetDays + 1
                                    val cellCal = calendarInstance.clone() as Calendar
                                    cellCal.set(Calendar.DAY_OF_MONTH, dayNum)
                                    val cellDateStr = sdf.format(cellCal.time)
                                    val isSelected = cellDateStr == currentDate
                                    val isToday = sdf.format(Date()) == cellDateStr
                                    val isFuture = cellCal.timeInMillis > System.currentTimeMillis()

                                    val face = getWeeklyStatus(cellDateStr, medications, allLogs, isFuture)

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable { viewModel.setCurrentDate(cellDateStr) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else if (isToday) {
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 2.dp else if (isToday) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (face != "⏳" && medications.isNotEmpty()) {
                                                Text(
                                                    text = face,
                                                    style = MaterialTheme.typography.bodySmall
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
        }

        // Preview medication list for selected calendar day
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Seçilen Gün İlaçları ($currentDate)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val selectedDayLogs = allLogs.filter { it.date == currentDate }

                if (medications.isEmpty()) {
                    Text("Ekli ilaç yok.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    medications.forEach { med ->
                        val log = selectedDayLogs.find { it.medicationId == med.id }
                        val status = when {
                            log == null -> "Pending"
                            log.isTaken -> "Taken"
                            else -> "NotTaken"
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(med.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Saat: ${med.time} • Doz: ${med.dosage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = {
                                    if (status == "Taken") {
                                        viewModel.deleteMedicationLog(med.id)
                                    } else {
                                        viewModel.toggleMedicationTaken(med.id, true)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Aldım",
                                        tint = if (status == "Taken") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                }

                                IconButton(onClick = {
                                    if (status == "NotTaken") {
                                        viewModel.deleteMedicationLog(med.id)
                                    } else {
                                        viewModel.toggleMedicationTaken(med.id, false)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Circle,
                                        contentDescription = "Almadım",
                                        tint = if (status == "NotTaken") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun BmiTab() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("bmi_prefs", Context.MODE_PRIVATE) }
    
    var weightStr by remember { mutableStateOf(sharedPrefs.getString("weight", "") ?: "") }
    var heightStr by remember { mutableStateOf(sharedPrefs.getString("height", "") ?: "") }
    var gender by remember { mutableStateOf(sharedPrefs.getString("gender", "Erkek") ?: "Erkek") }
    
    var bmiResult by remember { 
        val w = weightStr.toFloatOrNull()
        val h = heightStr.toFloatOrNull()
        mutableStateOf(
            if (w != null && h != null && h > 0) {
                w / ((h / 100f) * (h / 100f))
            } else {
                null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Vücut Kitle Endeksi (VKE) Takibi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Boy, kilo ve cinsiyet bilgilerinizi girerek sağlık durumunuza göre kişiselleştirilmiş tavsiyeler alın.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Cinsiyetiniz",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val genderOptions = listOf("Erkek", "Kadın")
            genderOptions.forEach { option ->
                val isSelected = gender == option
                Button(
                    onClick = { gender = option },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(option, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = heightStr,
            onValueChange = { heightStr = it.filter { char -> char.isDigit() } },
            label = { Text("Boy (cm)") },
            placeholder = { Text("Örn: 175") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
            value = weightStr,
            onValueChange = { weightStr = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Kilo (kg)") },
            placeholder = { Text("Örn: 70") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )

        Button(
            onClick = {
                val w = weightStr.toFloatOrNull()
                val h = heightStr.toFloatOrNull()
                if (w != null && h != null && h > 0) {
                    val bmi = w / ((h / 100f) * (h / 100f))
                    bmiResult = bmi
                    sharedPrefs.edit()
                        .putString("weight", weightStr)
                        .putString("height", heightStr)
                        .putString("gender", gender)
                        .apply()
                } else {
                    bmiResult = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text("Kaydet ve Hesapla", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        bmiResult?.let { bmi ->
            val formattedBmi = String.format(Locale.US, "%.1f", bmi)
            
            // Calculate ideal weight based on Robinson formula
            val hVal = heightStr.toFloatOrNull()
            val idealWeight = if (hVal != null && hVal > 0) {
                if (gender == "Erkek") {
                    if (hVal > 152.4f) {
                        52.0f + 1.9f * ((hVal - 152.4f) / 2.54f)
                    } else {
                        52.0f
                    }
                } else {
                    if (hVal > 152.4f) {
                        49.0f + 1.7f * ((hVal - 152.4f) / 2.54f)
                    } else {
                        49.0f
                    }
                }
            } else null

            val (category, color, advice) = when {
                bmi < 18.5 -> {
                    if (gender == "Erkek") {
                        Triple(
                            "Zayıf",
                            MaterialTheme.colorScheme.error,
                            "Erkekler için sağlıklı vücut direnci ve kas kütlesi kazanımı adına yüksek proteinli beslenmeli, hafif ağırlık egzersizleri yapmalı ve doktorunuza veya diyetisyeninize danışmalısınız."
                        )
                    } else {
                        Triple(
                            "Zayıf",
                            MaterialTheme.colorScheme.error,
                            "Kadınlar için sağlıklı hormonal denge ve ideal yağ oranı kazanımı son derece önemlidir. Düzenli beslenmeli, doktor veya diyetisyen kontrolünde sağlıklı kilo almalısınız."
                        )
                    }
                }
                bmi < 25.0 -> {
                    if (gender == "Erkek") {
                        Triple(
                            "Normal Kilolu",
                            MaterialTheme.colorScheme.primary,
                            "Harika! Erkekler için sağlıklı bir kilodasınız. Formunuzu korumak için dengeli beslenmeye, düzenli su tüketimine ve ilaç saatlerinize sadık kalmaya devam edin!"
                        )
                    } else {
                        Triple(
                            "Normal Kilolu",
                            MaterialTheme.colorScheme.primary,
                            "Harika! Kadınlar için sağlıklı bir kilodasınız. Hormonal dengenizi ve formunuzu korumak için dengeli beslenmeye ve ilaç saatlerinize uymaya devam edin!"
                        )
                    }
                }
                bmi < 30.0 -> {
                    if (gender == "Erkek") {
                        Triple(
                            "Fazla Kilolu",
                            MaterialTheme.colorScheme.error,
                            "Dengeli beslenmeli, hafif egzersizler yapmalı ve günlük hareket miktarınızı artırmalısınız. Erkekler için kas kütlesini koruyarak kardiyo yapmak metabolizmanızı hızlandıracaktır."
                        )
                    } else {
                        Triple(
                            "Fazla Kilolu",
                            MaterialTheme.colorScheme.error,
                            "Dengeli beslenmeli, hafif egzersizler yapmalı ve günlük hareket miktarınızı artırmalısınız. Kadınlar için porsiyon kontrolü ve düzenli yürüyüşler metabolizmayı destekleyecektir."
                        )
                    }
                }
                else -> {
                    if (gender == "Erkek") {
                        Triple(
                            "Obezite",
                            MaterialTheme.colorScheme.error,
                            "Sağlığınız için düzenli egzersiz yapmanız, doktor ve diyetisyen tavsiyelerine uymanız son derece önemlidir. Erkeklerde obezite kalp damar hastalıkları riskini artırabileceğinden tedavi başarısı için ilaç planınızı aksatmayın."
                        )
                    } else {
                        Triple(
                            "Obezite",
                            MaterialTheme.colorScheme.error,
                            "Sağlığınız için düzenli egzersiz yapmanız, doktor ve diyetisyen tavsiyelerine uymanız son derece önemlidir. Kadınlarda obezite hormonal dengeleri etkileyebileceğinden tedavi başarısı için ilaç planınızı aksatmayın."
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = color.copy(alpha = 0.12f)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, color)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VKE Sonucunuz: $formattedBmi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Surface(
                            color = color,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    idealWeight?.let { iw ->
                        val formattedIw = String.format(Locale.US, "%.1f", iw)
                        Text(
                            text = "Cinsiyetinize Göre İdeal Kilonuz: $formattedIw kg",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = color.copy(alpha = 0.3f))

                    Text(
                        text = "🩺 Sağlık Tavsiyesi:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )

                    Text(
                        text = advice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationCard(
    medication: Medication,
    todayLog: MedicationLog?,
    allLogs: List<MedicationLog> = emptyList(),
    onSetStatus: (Boolean) -> Unit,
    onClearStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val status = when {
        todayLog == null -> "Pending"
        todayLog.isTaken -> "Taken"
        else -> "NotTaken"
    }

    val containerColor = when (status) {
        "Taken" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        "NotTaken" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val outlineColor = when (status) {
        "Taken" -> MaterialTheme.colorScheme.primary
        "NotTaken" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    val medLogs = allLogs.filter { it.medicationId == medication.id }
    val takenCount = medLogs.count { it.isTaken }
    val missedCount = medLogs.count { !it.isTaken }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, outlineColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (status) {
                            "Taken" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "NotTaken" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "Doz: ${medication.dosage} • Saat: ${medication.time}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (status) {
                            "Taken" -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            "NotTaken" -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Chips to show taken/missed days count
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("$takenCount Alınan") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = Color(0xFF4CAF50),
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f))
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("$missedCount Atlanan") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = Color(0xFFE57373),
                                containerColor = Color(0xFFE57373).copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.2f))
                        )
                    }
                }
                
                // Mini circular chart on the right
                MiniMedicationCircularChart(
                    taken = takenCount,
                    missed = missedCount,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Daha Fazla",
                            tint = when (status) {
                                "Taken" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "NotTaken" -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Düzenle") }, onClick = { expanded = false; onEdit() })
                        DropdownMenuItem(text = { Text("Sil") }, onClick = { expanded = false; onDelete() })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Aldım / Almadım Choice buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Aldım button
                Button(
                    onClick = {
                        if (status == "Taken") {
                            onClearStatus()
                        } else {
                            onSetStatus(true)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == "Taken") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (status == "Taken") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    ),
                    border = if (status != "Taken") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Aldım",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aldım", style = MaterialTheme.typography.labelLarge)
                }

                // Almadım button
                Button(
                    onClick = {
                        if (status == "NotTaken") {
                            onClearStatus()
                        } else {
                            onSetStatus(false)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == "NotTaken") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface,
                        contentColor = if (status == "NotTaken") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error
                    ),
                    border = if (status != "NotTaken") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Almadım",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Almadım", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun MiniMedicationCircularChart(
    taken: Int,
    missed: Int,
    modifier: Modifier = Modifier
) {
    val total = taken + missed
    val percent = if (total > 0) taken.toFloat() / total else 0f
    
    Box(
        modifier = modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            val sizeMin = size.minDimension
            val arcSize = Size(sizeMin - strokeWidth, sizeMin - strokeWidth)
            val topLeftOffset = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background gray circle track
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (total > 0) {
                val sweepTaken = percent * 360f
                val sweepMissed = (1f - percent) * 360f

                // Draw Taken portion in beautiful green
                if (sweepTaken > 0f) {
                    drawArc(
                        color = Color(0xFF4CAF50),
                        startAngle = -90f,
                        sweepAngle = sweepTaken,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Draw Missed portion in beautiful red/coral
                if (sweepMissed > 0f) {
                    drawArc(
                        color = Color(0xFFE57373),
                        startAngle = -90f + sweepTaken,
                        sweepAngle = sweepMissed,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
        
        Text(
            text = if (total > 0) "${(percent * 100).toInt()}%" else "0%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

