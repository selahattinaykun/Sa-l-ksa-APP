package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppRepository(
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao
) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()

    suspend fun insertMedication(medication: Medication): Long = medicationDao.insertMedication(medication)

    suspend fun deleteMedication(id: Int) = medicationDao.deleteMedicationById(id)

    fun getLogsByDate(date: String): Flow<List<MedicationLog>> = medicationLogDao.getLogsByDate(date)

    fun getAllLogs(): Flow<List<MedicationLog>> = medicationLogDao.getAllLogs()
    
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<MedicationLog>> = medicationLogDao.getLogsBetweenDates(startDate, endDate)

    suspend fun deleteLog(medId: Int, date: String) {
        medicationLogDao.deleteLog(medId, date)
    }

    suspend fun toggleLogStatus(medId: Int, date: String, isTaken: Boolean) {
        val currentTimestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val existing = medicationLogDao.getLog(medId, date)
        if (existing != null) {
            medicationLogDao.insertLog(existing.copy(isTaken = isTaken, timestamp = currentTimestamp))
        } else {
            medicationLogDao.insertLog(MedicationLog(medicationId = medId, date = date, isTaken = isTaken, timestamp = currentTimestamp))
        }
    }
}
