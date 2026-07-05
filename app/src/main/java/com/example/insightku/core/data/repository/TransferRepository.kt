package com.example.insightku.core.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.local.database.InsightKuDatabase
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.worker.SyncTransactionWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TransferRepository — atomic transfer between two accounts.
 *
 * Creates two linked Transaction records (TRANSFER_OUT + TRANSFER_IN) sharing the same
 * [transferId]. Both writes happen in a single Room transaction so the ledger is always consistent.
 *
 * All Transactions is the single source of truth — Account History is just filtered transactions.
 */
@Singleton
class TransferRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val database: InsightKuDatabase,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TransferRepository"
    }

    /**
     * Execute an atomic transfer between two accounts.
     *
     * Creates two linked Transaction records:
     * - TRANSFER_OUT from source (amount negative)
     * - TRANSFER_IN to destination (amount positive)
     *
     * Both share the same [transferId] for traceability.
     * All writes are atomic — either both succeed or both roll back.
     */
    suspend fun transfer(
        sourceAccountId: String,
        destinationAccountId: String,
        amount: Double,
        note: String = "",
        date: Long = System.currentTimeMillis()
    ): Result<Pair<Transaction, Transaction>> {
        return try {
            if (amount <= 0) {
                return Result.failure(IllegalArgumentException("Transfer amount must be positive"))
            }
            if (sourceAccountId == destinationAccountId) {
                return Result.failure(IllegalArgumentException("Cannot transfer to the same account"))
            }

            val sourceAccount = accountDao.getAccountById(sourceAccountId)
                ?: return Result.failure(IllegalArgumentException("Source account not found"))
            val destAccount = accountDao.getAccountById(destinationAccountId)
                ?: return Result.failure(IllegalArgumentException("Destination account not found"))

            if (sourceAccount.balance < amount) {
                return Result.failure(IllegalArgumentException("Insufficient balance in source account"))
            }

            val transferId = UUID.randomUUID().toString()

            val pair = executeAtomicTransfer(
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                amount = amount,
                transferId = transferId,
                note = note,
                date = date,
                sourceName = sourceAccount.name,
                destName = destAccount.name
            )

            Log.d(TAG, "Transfer completed: $sourceAccountId → $destinationAccountId, amount=$amount, transferId=$transferId")
            Result.success(pair)
        } catch (e: Exception) {
            Log.e(TAG, "Transfer failed", e)
            Result.failure(e)
        }
    }

    private suspend fun executeAtomicTransfer(
        sourceAccountId: String,
        destinationAccountId: String,
        amount: Double,
        transferId: String,
        note: String,
        date: Long,
        sourceName: String,
        destName: String
    ): Pair<Transaction, Transaction> {
        return database.withTransaction {
            val now = System.currentTimeMillis()

            // 1. Create TRANSFER_OUT from source (negative amount)
            val transferOut = Transaction(
                title = "Transfer to $destName",
                amount = -amount,
                category = "Transfer Out",
                date = date,
                type = TransactionType.TRANSFER_OUT,
                description = note.ifBlank { "Transfer from $sourceName to $destName" },
                accountId = sourceAccountId,
                relatedAccountId = destinationAccountId,
                transferId = transferId,
                sourceModule = "transfer",
                createdAt = now
            )
            transactionDao.insertTransaction(transferOut.copy(isSynced = false))

            // 2. Create TRANSFER_IN to destination (positive amount)
            val transferIn = Transaction(
                title = "Transfer from $sourceName",
                amount = amount,
                category = "Transfer In",
                date = date,
                type = TransactionType.TRANSFER_IN,
                description = note.ifBlank { "Transfer from $sourceName to $destName" },
                accountId = destinationAccountId,
                relatedAccountId = sourceAccountId,
                transferId = transferId,
                sourceModule = "transfer",
                createdAt = now
            )
            transactionDao.insertTransaction(transferIn.copy(isSynced = false))

            // 3. Update both account balances atomically
            accountDao.updateBalance(sourceAccountId, -amount, now)
            accountDao.updateBalance(destinationAccountId, amount, now)

            Pair(
                transferOut.copy(isSynced = false),
                transferIn.copy(isSynced = false)
            )
        }
    }

    /** Get both transactions in a transfer pair. */
    suspend fun getTransferPair(transferId: String): List<Transaction> {
        return transactionDao.getTransactionsByTransferId(transferId)
    }
}
