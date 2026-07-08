package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.util.AlarmHelper
import com.example.viewmodel.MediViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    viewModel: MediViewModel,
    medicationId: Int? = null,
    onBack: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val existingMed = remember(medications, medicationId) { medications.find { it.id == medicationId } }

    var name by remember(existingMed) { mutableStateOf(existingMed?.name ?: "") }
    var dosage by remember(existingMed) { mutableStateOf(existingMed?.dosage ?: "") }
    var time by remember(existingMed) { mutableStateOf(existingMed?.time ?: "") }
    var notificationsEnabled by remember(existingMed) { mutableStateOf(existingMed?.notificationsEnabled ?: true) }
    var alarmsEnabled by remember(existingMed) { mutableStateOf(existingMed?.alarmsEnabled ?: false) }
    
    val context = LocalContext.current

    val isValidTime = remember(time) {
        val parts = time.split(":")
        if (parts.size != 2) false
        else {
            val h = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            h != null && m != null && h in 0..23 && m in 0..59 && parts[1].length == 2
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingMed != null) "İlacı Düzenle" else "Yeni İlaç Ekle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("İlaç Adı") },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
            
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Doz (örn: 1 Tablet)") },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
            
            OutlinedTextField(
                value = time,
                onValueChange = { newValue ->
                    if (newValue.length <= 5 && newValue.all { it.isDigit() || it == ':' }) {
                        val digitsOnly = newValue.filter { it.isDigit() }
                        if (newValue.length > time.length) { // user is typing
                            if (digitsOnly.length == 3 && !newValue.contains(':')) {
                                time = "${digitsOnly.substring(0, 2)}:${digitsOnly.substring(2)}"
                            } else if (digitsOnly.length == 2 && newValue.length == 2) {
                                time = "$newValue:"
                            } else {
                                time = newValue
                            }
                        } else { // user is deleting
                            time = newValue
                        }
                    }
                },
                label = { Text("Saat (00:00 formatında)") },
                placeholder = { Text("00:00") },
                isError = time.isNotEmpty() && !isValidTime,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = {
                        val currentParts = time.split(":")
                        val initialHour = currentParts.getOrNull(0)?.toIntOrNull() ?: 8
                        val initialMinute = currentParts.getOrNull(1)?.toIntOrNull() ?: 0
                        android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val formattedHour = String.format(java.util.Locale.US, "%02d", hourOfDay)
                                val formattedMinute = String.format(java.util.Locale.US, "%02d", minute)
                                time = "$formattedHour:$formattedMinute"
                            },
                            initialHour,
                            initialMinute,
                            true
                        ).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Saat Seç"
                        )
                    }
                },
                supportingText = {
                    if (time.isNotEmpty() && !isValidTime) {
                        Text("Lütfen geçerli bir saat girin (örn: 08:30)", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Klavyeden yazabilir veya sağdaki butona dokunarak seçebilirsiniz.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bildirim Etkin", style = MaterialTheme.typography.titleMedium)
                    Text("İlaç saati yaklaştığında bildirim gönderilsin", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sesli Alarm Çalsın", style = MaterialTheme.typography.titleMedium)
                    Text("İlaç vaktinde sesli ve titreşimli alarm çalsın", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = alarmsEnabled,
                    onCheckedChange = { alarmsEnabled = it }
                )
            }
            
            Button(
                onClick = {
                    if (name.isNotBlank() && dosage.isNotBlank() && isValidTime) {
                        val parts = time.split(":")
                        val formattedHour = String.format(java.util.Locale.US, "%02d", parts[0].toInt())
                        val formattedMinute = String.format(java.util.Locale.US, "%02d", parts[1].toInt())
                        val formattedTime = "$formattedHour:$formattedMinute"

                        if (existingMed != null) {
                            viewModel.updateMedication(existingMed.id, name, dosage, formattedTime, notificationsEnabled, alarmsEnabled)
                            AlarmHelper.scheduleMedicationAlarm(context, existingMed.id, name, dosage, formattedTime, notificationsEnabled, alarmsEnabled)
                            onBack()
                        } else {
                            viewModel.addMedication(name, dosage, formattedTime, notificationsEnabled, alarmsEnabled) { generatedId ->
                                AlarmHelper.scheduleMedicationAlarm(context, generatedId, name, dosage, formattedTime, notificationsEnabled, alarmsEnabled)
                                onBack()
                            }
                        }
                    }
                },
                enabled = name.isNotBlank() && dosage.isNotBlank() && isValidTime,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Kaydet")
            }
        }
    }
}
