package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType
import com.example.insightku.domain.usecase.transaction.AddTransactionUseCase
import com.example.insightku.domain.usecase.transaction.GetTransactionsUseCase
import com.example.insightku.ui.components.dashboard.DashboardEvent
import com.example.insightku.ui.components.dashboard.DashboardUiState
import com.example.insightku.ui.components.dashboard.ForecastPeriod
import com.example.insightku.ui.components.dashboard.TransactionItem
import com.example.insightku.utils.ErrorBus
import com.example.insightku.utils.SessionManager
import com.example.insightku.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val errorBus: ErrorBus,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    // BUG4 FIX: Simpan referensi Job agar bisa di-cancel sebelum launch baru,
    // mencegah multiple collectors aktif bersamaan (race condition).
    private var loadJob: Job? = null
    private var userNameJob: Job? = null

    init {
        loadUserName()
        loadDashboardData()
        // ISSUE 2 FIX: Setelah subscribe ke Room Flow, langsung trigger refresh dari
        // Firestore di background. Ini memastikan data ter-load setelah:
        //   a) User pertama kali login (Room kosong)
        //   b) Setelah logout+login (Room dibersihkan oleh LogoutUseCase)
        // Room Flow akan otomatis emit ulang begitu data dari Firestore masuk ke Room.
        refreshData()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.LoadDashboardData -> loadDashboardData()
            DashboardEvent.RefreshData       -> refreshData()
            DashboardEvent.ClearError        -> _uiState.update { it.copy(error = null) }
            DashboardEvent.ToggleBalanceVisibility ->
                _uiState.update { it.copy(isBalanceVisible = !it.isBalanceVisible) }
            is DashboardEvent.ToggleForecastPeriod -> _uiState.update {
                it.copy(forecastPeriod = if (event.period == "week") ForecastPeriod.WEEKLY else ForecastPeriod.MONTHLY)
            }
            is DashboardEvent.AddTransaction -> addTransaction(event.transaction)
            else -> {}
        }
    }

    private fun loadUserName() {
        // BUG4 FIX: Cancel job lama sebelum launch baru
        userNameJob?.cancel()
        userNameJob = viewModelScope.launch {
            sessionManager.userName.collect { name ->
                _uiState.update { it.copy(userName = name.ifBlank { "User" }) }
            }
        }
    }

    private fun loadDashboardData() {
        // Cancel job lama sebelum launch baru
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                getTransactionsUseCase().collect { transactions ->
                    // Jika data sudah ada (dari pre-fetch LoginUseCase),
                    // isLoading langsung false. Jika masih kosong, tunggu refreshData().
                    updateStateFromTransactions(transactions)
                }
            } catch (e: CancellationException) {
                // Terjadi normal saat navigasi back atau ViewModel di-clear
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: "Gagal memuat data dashboard"
                _uiState.update { it.copy(isLoading = false, error = msg) }
                errorBus.send(msg)
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                getTransactionsUseCase.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Jika Room kosong (LoginUseCase pre-fetch juga gagal karena offline),
                // tampilkan error agar user tahu dan bisa retry.
                // Jika Room sudah ada data, ini adalah silent fail — data lama masih tampil.
                val hasData = _uiState.value.recentTransactions.isNotEmpty()
                if (!hasData) {
                    val msg = "Tidak dapat memuat data. Periksa koneksi internet."
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    // Tidak kirim ke errorBus agar tidak double-show (sudah ada di state)
                }
            }
        }
    }

    private fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            addTransactionUseCase(transaction)
                .onFailure { exception ->
                    val msg = exception.message ?: "Gagal menambah transaksi"
                    _uiState.update { it.copy(error = msg) }
                    errorBus.send(msg)
                }
        }
    }

    private fun updateStateFromTransactions(transactions: List<Transaction>) {
        val totalBalance   = transactions.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
        val monthlyIncome  = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val monthlyExpenses= transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val recentTransactions = transactions
            .sortedByDescending { it.date }
            .take(5)
            .map { t ->
                TransactionItem(
                    id       = t.id,
                    title    = t.title,
                    category = t.category,
                    amount   = t.amount,
                    // ISSUE 3 FIX: Konversi timestamp ke string relatif human-readable.
                    // Sebelumnya: t.date.toString() → "1777595336895" (raw angka, tidak berguna di UI)
                    // Sekarang: TimeUtils.toShortRelativeTime() → "2h ago", "Just now", "3d ago"
                    // Mapping terjadi di ViewModel (data → UI model), bukan di Composable.
                    time     = TimeUtils.toShortRelativeTime(t.date),
                    isIncome = t.type == TransactionType.INCOME,
                    iconName = "",
                    colorHex = ""
                )
            }

        // ── Streak calculation ─────────────────────────────────────────────────
        // BUG1 FIX: Gunakan SimpleDateFormat("yyyyMMdd") dengan format zero-padded
        // agar tidak ada key collision (misal: 31 Jan "2025031" vs 3 Nov "2025103"
        // bisa bertubrukan jika pakai raw Calendar.MONTH + DAY_OF_MONTH tanpa padding).
        val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val trackedDayKeys: Set<String> = transactions.map { t ->
            dayFmt.format(Date(t.date))
        }.toSet()

        fun dayKey(cal: Calendar): String = dayFmt.format(cal.time)

        val today = Calendar.getInstance()
        val hasTrackedToday = dayKey(today) in trackedDayKeys

        // Walk backwards from today counting consecutive tracked days
        var streak = 0
        val check = today.clone() as Calendar
        // If today not tracked yet, start checking from yesterday for streak count
        if (!hasTrackedToday) check.add(Calendar.DAY_OF_YEAR, -1)
        while (dayKey(check) in trackedDayKeys) {
            streak++
            check.add(Calendar.DAY_OF_YEAR, -1)
        }
        // ───────────────────────────────────────────────────────────────────────

        _uiState.update {
            it.copy(
                isLoading           = false,
                totalBalance        = totalBalance,
                monthlyIncome       = monthlyIncome,
                monthlyExpenses     = monthlyExpenses,
                recentTransactions  = recentTransactions,
                currentStreak       = streak,
                hasTrackedToday     = hasTrackedToday
            )
        }
    }
}