package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    val context = LocalContext.current

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
                onValueChange = { time = it },
                label = { Text("Saat (örn: 08:30)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
            
            Button(
                onClick = {
                    if (name.isNotBlank() && dosage.isNotBlank() && time.isNotBlank()) {
                        if (existingMed != null) {
                            viewModel.updateMedication(existingMed.id, name, dosage, time)
                            AlarmHelper.scheduleMedicationAlarm(context, existingMed.id, name, dosage, time)
                        } else {
                            viewModel.addMedication(name, dosage, time)
                            AlarmHelper.scheduleMedicationAlarm(context, name.hashCode(), name, dosage, time)
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Kaydet")
            }
        }
    }
}
