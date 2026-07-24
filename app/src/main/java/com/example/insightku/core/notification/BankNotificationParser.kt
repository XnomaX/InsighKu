package com.example.insightku.core.notification

import android.util.Log
import com.example.insightku.core.data.model.DraftConfidence
import com.example.insightku.core.data.model.TransactionType

private const val TAG = "NotificationDebug"

// ── Supported bank/e-wallet package names ─────────────────────────────────────

val SUPPORTED_BANK_PACKAGES = mapOf(
    // ── BCA ────────────────────────────────────────────────────────────────────
    "com.bca"                            to "BCA Mobile",
    "com.bca.mybca.omni.android"         to "myBCA",
    "com.bcadigital.blu"                 to "blu by BCA Digital",
    "com.bca.sakuku"                     to "Sakuku",

    // ── BRI ────────────────────────────────────────────────────────────────────
    "id.co.bri.brimo"                    to "BRImo",

    // ── BNI ────────────────────────────────────────────────────────────────────
    "id.bni.wondr"                       to "wondr by BNI",

    // ── Mandiri ────────────────────────────────────────────────────────────────
    "id.bmri.livin"                      to "Livin by Mandiri",

    // ── SeaBank ────────────────────────────────────────────────────────────────
    "id.co.bankbkemobile.digitalbank"    to "SeaBank",

    // ── Jenius (BTPN) ──────────────────────────────────────────────────────────
    "com.btpn.dc"                        to "Jenius",

    // ── Bank Mega ──────────────────────────────────────────────────────────────
    "com.msmile.bankmega"                to "M-Smile (Bank Mega)",

    // ── CIMB Niaga ─────────────────────────────────────────────────────────────
    "com.cimbniaga.go.mobile.android"    to "OCTO Mobile (CIMB)",

    // ── Permata ────────────────────────────────────────────────────────────────
    "net.myinfosys.PermataMobileX"       to "Permata ME",

    // ── BTN ────────────────────────────────────────────────────────────────────
    "id.co.btn.mobilebanking.android"    to "balé by BTN",

    // ── Bank Jago ──────────────────────────────────────────────────────────────
    "com.jago.digitalBanking"            to "Bank Jago",

    // ── Neobank ────────────────────────────────────────────────────────────────
    "com.bnc.finance"                    to "neobank",

    // ── Allo Bank ──────────────────────────────────────────────────────────────
    "com.alloapp.yump"                   to "Allo Bank",

    // ── LINE Bank ──────────────────────────────────────────────────────────────
    "id.co.linebank"                     to "LINE Bank",

    // ── DBS digibank ───────────────────────────────────────────────────────────
    "com.dbs.id.digibank"               to "digibank by DBS",

    // ── OCBC ───────────────────────────────────────────────────────────────────
    "com.ocbcnisp.onemobileapp"          to "OCBC mobile",

    // ── E-Wallet ───────────────────────────────────────────────────────────────
    "id.dana"                            to "DANA",
    "ovo.id"                             to "OVO",
    "com.gojek.gopay"                    to "GoPay",
    "com.shopeepay.id"                   to "ShopeePay",
    "com.telkom.mwallet"                 to "LinkAja",
    "com.ada.astrapay"                   to "AstraPay",
    "com.isaku.app"                      to "i.saku"
)

// ── Parsed result ─────────────────────────────────────────────────────────────

data class ParsedBankTransaction(
    val amount: Double,
    val merchant: String,
    val type: TransactionType,
    val balance: Double? = null,
    val rawTitle: String = "",
    val rawContent: String = "",
    val bankName: String = "",
    val category: String = "",
    val sourcePackage: String = "",
    val confidence: DraftConfidence = DraftConfidence.MEDIUM
)

// ── Main parser ───────────────────────────────────────────────────────────────

object BankNotificationParser {

    // PERFORMANCE FIX: Pre-compiled regex patterns — compiled once, not per-call
    private val AMOUNT_PATTERNS = listOf(
        Regex("""[Rr][Pp]\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)"""),
        Regex("""IDR\s*([0-9]{1,3}(?:[.,][0-9]{3})*)"""),
        Regex("""(?i)sebesar\s+([0-9]{1,3}(?:[.,][0-9]{3})+)"""),
        Regex("""(\d{1,3}(?:\.\d{3})+(?:,\d{2})?)""")
    )

    private val BALANCE_REGEX = Regex(
        """(?i)(?:saldo|sisa|balance)\s*:?\s*[Rr][Pp]\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)"""
    )

    private val MERCHANT_PATTERNS = mapOf(
        "ke" to Regex("""(?i)\bke\b\s+([A-Za-z0-9][A-Za-z0-9\s\-&.']{1,40})"""),
        "dari" to Regex("""(?i)\bdari\b\s+([A-Za-z0-9][A-Za-z0-9\s\-&.']{1,40})"""),
        "di" to Regex("""(?i)\bdi\b\s+([A-Za-z0-9][A-Za-z0-9\s\-&.']{1,40})"""),
        "to" to Regex("""(?i)\bto\b\s+([A-Za-z0-9][A-Za-z0-9\s\-&.']{1,40})"""),
        "at" to Regex("""(?i)\bat\b\s+([A-Za-z0-9][A-Za-z0-9\s\-&.']{1,40})""")
    )

    private val MERCHANT_STOP_WORDS = setOf("saldo", "balance", "rp", "idr", "sebesar", "senilai", "dengan", "sisa")

    fun isSupportedPackage(packageName: String): Boolean =
        SUPPORTED_BANK_PACKAGES.containsKey(packageName)

    fun getBankName(packageName: String): String =
        SUPPORTED_BANK_PACKAGES[packageName] ?: packageName

    /**
     * Parse a bank notification into a [ParsedBankTransaction].
     * Returns null if the notification does not look like a transaction.
     */
    fun parse(
        packageName: String,
        title: String,
        content: String
    ): ParsedBankTransaction? {
        val bankName = getBankName(packageName)
        Log.d(TAG, "Parsing notification from $bankName ($packageName)")
        Log.d(TAG, "Title = $title")
        Log.d(TAG, "Content = $content")

        val combined = "$title $content"

        val result = when (packageName) {
            // BCA
            "com.bca",
            "com.bca.mybca.omni.android"         -> parseBCA(title, content, combined, bankName)
            // blu by BCA Digital
            "com.bcadigital.blu"                 -> parseBlu(title, content, combined, bankName)
            // Sakuku (BCA e-wallet)
            "com.bca.sakuku"                     -> parseDANA(title, content, combined, bankName)
            // BRI
            "id.co.bri.brimo"                    -> parseBRI(title, content, combined, bankName)
            // BNI
            "id.bni.wondr"                       -> parseBNI(title, content, combined, bankName)
            // Mandiri
            "id.bmri.livin"                      -> parseMandiri(title, content, combined, bankName)
            // SeaBank
            "id.co.bankbkemobile.digitalbank"    -> parseSeaBank(title, content, combined, bankName)
            // Jenius
            "com.btpn.dc"                        -> parseJenius(title, content, combined, bankName)
            // DANA
            "id.dana"                            -> parseDANA(title, content, combined, bankName)
            // OVO
            "ovo.id"                             -> parseOVO(title, content, combined, bankName)
            // GoPay
            "com.gojek.gopay"                    -> parseGoPay(title, content, combined, bankName)
            // ShopeePay
            "com.shopeepay.id"                   -> parseShopeePay(title, content, combined, bankName)
            // Other banks — generic income/expense parser
            "com.msmile.bankmega",
            "com.cimbniaga.go.mobile.android",
            "net.myinfosys.PermataMobileX",
            "id.co.btn.mobilebanking.android",
            "com.jago.digitalBanking",
            "com.bnc.finance",
            "com.alloapp.yump",
            "id.co.linebank",
            "com.dbs.id.digibank",
            "com.ocbcnisp.onemobileapp",
            "com.telkom.mwallet",
            "com.ada.astrapay",
            "com.isaku.app"                      -> parseGenericIncome(title, content, combined, bankName)
            else                                 -> parseGeneric(title, content, combined, bankName)
        }

        if (result == null) {
            Log.d(TAG, "Parse failed — no transaction pattern matched")
        } else {
            Log.d(TAG, "Amount parsed = ${result.amount}")
            Log.d(TAG, "Merchant parsed = ${result.merchant}")
            Log.d(TAG, "Type parsed = ${result.type}")
        }

        return result?.copy(
            sourcePackage = packageName,
            confidence = computeConfidence(result, combined)
        )
    }

    /**
     * Hitung tingkat keyakinan dari sinyal parser — lebih kaya daripada heuristik post-hoc
     * karena tahu apakah type ditentukan keyword eksplisit atau tebakan default.
     *
     * - LOW    : merchant kosong/fallback ke nama bank, ATAU type tidak punya keyword
     *            eksplisit di teks (kemungkinan tebakan default parser).
     * - HIGH   : type punya keyword eksplisit DAN merchant jelas (bukan nama bank).
     * - MEDIUM : sisanya (sebagian sinyal kuat, sebagian ragu).
     */
    private fun computeConfidence(parsed: ParsedBankTransaction, combined: String): DraftConfidence {
        val merchantClean = parsed.merchant.trim()
        val merchantIsFallback = merchantClean.isBlank() ||
            merchantClean.equals(parsed.bankName.trim(), ignoreCase = true)

        val incomeKeywords = listOf("kredit", "credit", "masuk", "diterima", "terima", "menerima", "cashback", "refund", "top up", "topup")
        val expenseKeywords = listOf("debit", "keluar", "bayar", "pembayaran", "transfer", "membayar")
        val hasTypeKeyword = when (parsed.type) {
            TransactionType.INCOME  -> incomeKeywords.any { combined.contains(it, ignoreCase = true) }
            TransactionType.EXPENSE -> expenseKeywords.any { combined.contains(it, ignoreCase = true) }
            else -> false // Newer types (transfers, goals, etc.) are never created by notification parsing
        }

        return when {
            merchantIsFallback || !hasTypeKeyword -> DraftConfidence.LOW
            merchantClean.length >= 3            -> DraftConfidence.HIGH
            else                                 -> DraftConfidence.MEDIUM
        }
    }

    // ── BCA ───────────────────────────────────────────────────────────────────
    // Sample: "Transaksi BCA" / "Debit Rp100.000 ke TOKOPEDIA. Saldo Rp5.000.000"
    // Sample: "Kredit Rp500.000 dari TRANSFER. Saldo Rp5.500.000"

    private fun parseBCA(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — BCA"); return null
        }
        val type = when {
            combined.contains("kredit", true) || combined.contains("masuk", true) -> TransactionType.INCOME
            combined.contains("debit", true)  || combined.contains("keluar", true) -> TransactionType.EXPENSE
            else -> return null.also { Log.d(TAG, "Unsupported format — BCA type unknown") }
        }
        val merchant = extractMerchant(combined, listOf("ke", "dari", "di")) ?: bank
        val balance  = extractBalance(combined)
        return ParsedBankTransaction(amount, merchant, type, balance, title, content, bank)
    }

    // ── BRI ───────────────────────────────────────────────────────────────────
    // Sample: "BRImo - Berhasil" / "Transfer Rp200.000 ke 081234567890. Sisa Rp4.800.000"

    private fun parseBRI(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — BRI"); return null
        }
        val type = when {
            combined.contains("terima", true) || combined.contains("masuk", true) ||
            combined.contains("credit", true) -> TransactionType.INCOME
            combined.contains("transfer", true) || combined.contains("bayar", true) ||
            combined.contains("debit", true)  -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
        }
        val merchant = extractMerchant(combined, listOf("ke", "dari", "kepada")) ?: bank
        val balance  = extractBalance(combined)
        return ParsedBankTransaction(amount, merchant, type, balance, title, content, bank)
    }

    // ── BNI ───────────────────────────────────────────────────────────────────
    private fun parseBNI(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — BNI"); return null
        }
        val type = when {
            combined.contains("credit", true) || combined.contains("masuk", true) -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
        val merchant = extractMerchant(combined, listOf("ke", "dari", "merchant")) ?: bank
        val balance  = extractBalance(combined)
        return ParsedBankTransaction(amount, merchant, type, balance, title, content, bank)
    }

    // ── Mandiri ───────────────────────────────────────────────────────────────
    // Sample: "Mandiri Online" / "Pembayaran Rp150.000 ke PLN. Saldo Rp3.000.000"

    private fun parseMandiri(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — Mandiri"); return null
        }
        val type = when {
            combined.contains("terima", true) || combined.contains("credit", true) ||
            combined.contains("masuk", true) -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
        val merchant = extractMerchant(combined, listOf("ke", "dari", "kepada")) ?: bank
        val balance  = extractBalance(combined)
        return ParsedBankTransaction(amount, merchant, type, balance, title, content, bank)
    }

    // ── SeaBank ───────────────────────────────────────────────────────────────
    private fun parseSeaBank(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — SeaBank"); return null
        }
        val type = if (combined.contains("diterima", true) || combined.contains("masuk", true))
            TransactionType.INCOME else TransactionType.EXPENSE
        val merchant = extractMerchant(combined, listOf("ke", "dari", "di")) ?: bank
        return ParsedBankTransaction(amount, merchant, type, null, title, content, bank)
    }

    // ── Jenius ────────────────────────────────────────────────────────────────
    // Sample: "$ign / Cashback / Send It" notifications
    private fun parseJenius(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — Jenius"); return null
        }
        val type = when {
            combined.contains("receive", true) || combined.contains("cashback", true) ||
            combined.contains("masuk", true)  -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
        val merchant = extractMerchant(combined, listOf("to", "from", "at", "ke", "dari")) ?: bank
        val balance  = extractBalance(combined)
        return ParsedBankTransaction(amount, merchant, type, balance, title, content, bank)
    }

    // ── Generic income/expense (for other supported banks) ────────────────────
    private fun parseGenericIncome(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — $bank"); return null
        }
        val type = when {
            combined.contains("masuk", true)   || combined.contains("diterima", true) ||
            combined.contains("terima", true)  || combined.contains("credit", true)   ||
            combined.contains("top up", true)  || combined.contains("topup", true)    -> TransactionType.INCOME
            combined.contains("keluar", true)  || combined.contains("debit", true)    ||
            combined.contains("bayar", true)   || combined.contains("transfer", true) -> TransactionType.EXPENSE
            else -> return null.also { Log.d(TAG, "GenericIncome type unknown — $bank") }
        }
        val merchant = extractMerchant(combined, listOf("ke", "dari", "di", "kepada")) ?: bank
        return ParsedBankTransaction(amount, merchant, type, extractBalance(combined), title, content, bank)
    }

    // ── Blu by BCA ────────────────────────────────────────────────────────────
    private fun parseBlu(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — Blu"); return null
        }
        val type = if (combined.contains("masuk", true) || combined.contains("terima", true))
            TransactionType.INCOME else TransactionType.EXPENSE
        val merchant = extractMerchant(combined, listOf("ke", "dari", "di")) ?: bank
        return ParsedBankTransaction(amount, merchant, type, null, title, content, bank)
    }

    // ── DANA ──────────────────────────────────────────────────────────────────
    // Sample: "Pembayaran Berhasil" / "Kamu membayar Rp50.000 ke GoFood"
    private fun parseDANA(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — DANA"); return null
        }
        val type = when {
            combined.contains("menerima", true) || combined.contains("masuk", true) ||
            combined.contains("cashback", true) -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
        val merchant = extractMerchant(combined, listOf("ke", "dari", "di", "to")) ?: bank
        val balance  = extractBalance(combined)
        return ParsedBankTransaction(amount, merchant, type, balance, title, content, bank)
    }

    // ── OVO ───────────────────────────────────────────────────────────────────
    // Sample: "OVO Points / Transfer / Payment"
    private fun parseOVO(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — OVO"); return null
        }
        val type = when {
            combined.contains("diterima", true) || combined.contains("masuk", true) ||
            combined.contains("points earned", true) -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
        val merchant = extractMerchant(combined, listOf("ke", "dari", "at", "to")) ?: bank
        val balance  = extractBalance(combined)
        return ParsedBankTransaction(amount, merchant, type, balance, title, content, bank)
    }

    // ── GoPay ─────────────────────────────────────────────────────────────────
    // Sample: "Pembayaran GoPay" / "Kamu membayar Rp75.000 di GoFood"
    private fun parseGoPay(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — GoPay"); return null
        }
        val type = when {
            combined.contains("diterima", true) || combined.contains("cashback", true) ||
            combined.contains("refund", true)  -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
        val merchant = extractMerchant(combined, listOf("di", "ke", "dari", "at")) ?: bank
        val balance  = extractBalance(combined)
        return ParsedBankTransaction(amount, merchant, type, balance, title, content, bank)
    }

    // ── ShopeePay ─────────────────────────────────────────────────────────────
    private fun parseShopeePay(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — ShopeePay"); return null
        }
        val type = when {
            combined.contains("diterima", true) || combined.contains("cashback", true) ||
            combined.contains("refund", true)  -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
        val merchant = extractMerchant(combined, listOf("di", "ke", "dari", "to")) ?: bank
        return ParsedBankTransaction(amount, merchant, type, null, title, content, bank)
    }

    // ── Generic fallback ──────────────────────────────────────────────────────
    private fun parseGeneric(title: String, content: String, combined: String, bank: String): ParsedBankTransaction? {
        val amount = extractAmount(combined) ?: run {
            Log.d(TAG, "Missing amount — generic"); return null
        }
        val type = when {
            combined.contains("masuk", true) || combined.contains("credit", true) ||
            combined.contains("diterima", true) -> TransactionType.INCOME
            combined.contains("keluar", true) || combined.contains("debit", true) ||
            combined.contains("bayar", true)    -> TransactionType.EXPENSE
            else -> return null.also { Log.d(TAG, "Unsupported format — cannot determine type") }
        }
        val merchant = extractMerchant(combined, listOf("ke", "dari", "di", "to", "at")) ?: bank
        return ParsedBankTransaction(amount, merchant, type, null, title, content, bank)
    }

    // ── Extraction helpers ────────────────────────────────────────────────────

    /**
     * Extracts the first IDR amount found in text.
     * Handles: Rp100.000 / Rp 100,000 / IDR 100000 / 100.000,00
     */
    fun extractAmount(text: String): Double? {
        // PERFORMANCE FIX: Pre-compiled regex patterns (compiled once, not per-call)
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues[1]
                .replace(".", "")
                .replace(",", ".")
                .trim()
            val result = raw.toDoubleOrNull()
            if (result != null && result > 0) {
                Log.d(TAG, "extractAmount: pattern=${pattern.pattern.take(30)}, raw='${match.groupValues[1]}', result=$result")
                return result
            }
        }
        Log.d(TAG, "extractAmount: no match found in text='${text.take(80)}'")
        return null
    }

    /**
     * Extracts merchant name after a keyword like "ke", "di", "to".
     * Takes up to 4 words after the keyword, stopping at punctuation or common endings.
     */
    fun extractMerchant(text: String, keywords: List<String>): String? {
        for (kw in keywords) {
            val pattern = MERCHANT_PATTERNS[kw] ?: continue
            val match   = pattern.find(text) ?: continue
            val raw     = match.groupValues[1].trim()
            val words   = raw.split(" ")
                .takeWhile { it.lowercase() !in MERCHANT_STOP_WORDS && !it.startsWith("Rp") }
                .take(4)
                .joinToString(" ")
                .trimEnd('.', ',', ';', ':')
            if (words.isNotBlank()) return words
        }
        return null
    }

    /**
     * Extracts remaining balance from text.
     * Handles: "Saldo Rp5.000.000" / "Sisa Rp..." / "Balance Rp..."
     */
    fun extractBalance(text: String): Double? {
        val balanceSection = BALANCE_REGEX.find(text) ?: return null
        return balanceSection.groupValues[1]
            .replace(".", "")
            .replace(",", ".")
            .toDoubleOrNull()
    }
}
