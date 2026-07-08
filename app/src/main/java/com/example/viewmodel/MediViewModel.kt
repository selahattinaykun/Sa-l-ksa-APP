package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.Medication
import com.example.data.MedicationLog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediViewModel(private val repository: AppRepository) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private val _currentDate = MutableStateFlow(dateFormat.format(Date()))
    val currentDate: StateFlow<String> = _currentDate

    val medications = repository.allMedications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLogs = _currentDate.flatMapLatest { date ->
        repository.getLogsByDate(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLogs = repository.getAllLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val thirtyDaysAgo = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, -30)
    }.time
    private val thirtyDaysAgoString = dateFormat.format(thirtyDaysAgo)

    val last30DaysLogs = repository.getLogsBetweenDates(thirtyDaysAgoString, dateFormat.format(Date())).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addMedication(name: String, dosage: String, time: String, notificationsEnabled: Boolean, alarmsEnabled: Boolean, onAdded: (Int) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertMedication(Medication(name = name, dosage = dosage, time = time, notificationsEnabled = notificationsEnabled, alarmsEnabled = alarmsEnabled))
            onAdded(id.toInt())
        }
    }

    fun updateMedication(id: Int, name: String, dosage: String, time: String, notificationsEnabled: Boolean, alarmsEnabled: Boolean) {
        viewModelScope.launch {
            repository.insertMedication(Medication(id = id, name = name, dosage = dosage, time = time, notificationsEnabled = notificationsEnabled, alarmsEnabled = alarmsEnabled))
        }
    }

    fun deleteMedication(id: Int) {
        viewModelScope.launch {
            repository.deleteMedication(id)
        }
    }

    fun toggleMedicationTaken(medId: Int, isTaken: Boolean) {
        viewModelScope.launch {
            repository.toggleLogStatus(medId, _currentDate.value, isTaken)
        }
    }

    fun deleteMedicationLog(medId: Int) {
        viewModelScope.launch {
            repository.deleteLog(medId, _currentDate.value)
        }
    }

    fun setCurrentDate(date: String) {
        _currentDate.value = date
    }
}

class MediViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
