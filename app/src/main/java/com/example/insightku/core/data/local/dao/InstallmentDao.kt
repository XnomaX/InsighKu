package com.example.insightku.core.data.local.dao

import androidx.room.*
import com.example.insightku.core.data.model.Installment
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {

    @Query("SELECT COUNT(*) FROM installments WHERE isActive = 1")
    suspend fun countActiveInstallments(): Int

    @Query("SELECT * FROM installments WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getAllInstallments(): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getInstallmentById(id: String): Installment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: Installment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<Installment>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInstallmentsFromRemote(installments: List<Installment>)

    @Update
    suspend fun updateInstallment(installment: Installment)

    @Query("DELETE FROM installments WHERE id = :id")
    suspend fun deleteInstallment(id: String)

    @Query("DELETE FROM installments")
    suspend fun deleteAllInstallments()
}


