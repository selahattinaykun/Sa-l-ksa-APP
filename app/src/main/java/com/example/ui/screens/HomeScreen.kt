package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Medication
import com.example.data.MedicationLog
import com.example.viewmodel.MediViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MediViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Günlük İlaçlar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Yeni İlaç Ekle")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Tarih: $currentDate",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (medications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz ilaç eklenmedi.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(medications) { med ->
                        val isTaken = todayLogs.find { it.medicationId == med.id }?.isTaken == true
                        MedicationCard(
                            medication = med,
                            isTaken = isTaken,
                            onToggle = { viewModel.toggleMedicationTaken(med.id, !isTaken) },
                            onEdit = { onEditClick(med.id) },
                            onDelete = { viewModel.deleteMedication(med.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationCard(
    medication: Medication,
    isTaken: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(medication.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Doz: ${medication.dosage} • Saat: ${medication.time}", style = MaterialTheme.typography.bodyMedium)
            }
            
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Daha Fazla")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Düzenle") }, onClick = { expanded = false; onEdit() })
                    DropdownMenuItem(text = { Text("Sil") }, onClick = { expanded = false; onDelete() })
                }
            }
            
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = "Alındı",
                    tint = if (isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
