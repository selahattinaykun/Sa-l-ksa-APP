package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao
) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()

    suspend fun insertMedication(medication: Medication) = medicationDao.insertMedication(medication)

    suspend fun deleteMedication(id: Int) = medicationDao.deleteMedicationById(id)

    fun getLogsByDate(date: String): Flow<List<MedicationLog>> = medicationLogDao.getLogsByDate(date)

    fun getAllLogs(): Flow<List<MedicationLog>> = medicationLogDao.getAllLogs()
    
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<MedicationLog>> = medicationLogDao.getLogsBetweenDates(startDate, endDate)

    suspend fun toggleLogStatus(medId: Int, date: String, isTaken: Boolean) {
        val existing = medicationLogDao.getLog(medId, date)
        if (existing != null) {
            medicationLogDao.updateLogStatus(medId, date, isTaken)
        } else {
            medicationLogDao.insertLog(MedicationLog(medicationId = medId, date = date, isTaken = isTaken))
        }
    }
}
