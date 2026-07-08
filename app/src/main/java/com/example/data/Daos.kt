package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY time ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications ORDER BY time ASC")
    suspend fun getAllMedicationsList(): List<Medication>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedicationById(id: Int)
}

@Dao
interface MedicationLogDao {
    @Query("SELECT * FROM medication_logs WHERE date = :date")
    fun getLogsByDate(date: String): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs")
    fun getAllLogs(): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<MedicationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog)

    @Query("UPDATE medication_logs SET isTaken = :isTaken WHERE medicationId = :medId AND date = :date")
    suspend fun updateLogStatus(medId: Int, date: String, isTaken: Boolean)

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medId AND date = :date LIMIT 1")
    suspend fun getLog(medId: Int, date: String): MedicationLog?

    @Query("DELETE FROM medication_logs WHERE medicationId = :medId AND date = :date")
    suspend fun deleteLog(medId: Int, date: String)
}
