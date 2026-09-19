package com.example.insightku.feature.home.presentation

import com.example.insightku.feature.home.domain.receipt.ReceiptScanData
import com.example.insightku.feature.home.domain.receipt.ReceiptScanItem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ReceiptScanMapperTest : FunSpec({

    test("total -> amount, items -> description, merchant blank (expense)") {
        val data = ReceiptScanData(
            merchant = null,
            total = "32.000",
            items = listOf(
                ReceiptScanItem("ES KOPI", "1", "22000"),
                ReceiptScanItem("GORENAN", "2", "10000")
            ),
            rawText = "ignored"
        )
        val form = data.toTransactionFormData(isIncome = false)
        form.merchant shouldBe ""
        form.amount shouldBe "32000"
        form.isIncome shouldBe false
        form.description shouldBe "ES KOPI x1 = 22000\nGORENAN x2 = 10000"
    }

    test("falls back to raw text when items list is empty") {
        val data = ReceiptScanData(
            merchant = null, total = "1000", items = emptyList(), rawText = "some text"
        )
        val form = data.toTransactionFormData(isIncome = true)
        form.amount shouldBe "1000"
        form.description shouldBe "some text"
        form.isIncome shouldBe true
    }

    test("strips currency symbols and thousand separators from total") {
        val data = ReceiptScanData(
            merchant = null, total = "Rp 50.000", items = emptyList(), rawText = ""
        )
        val form = data.toTransactionFormData(isIncome = false)
        form.amount shouldBe "50000"
    }

    test("empty total yields a blank amount") {
        val data = ReceiptScanData(
            merchant = null, total = "", items = emptyList(), rawText = ""
        )
        val form = data.toTransactionFormData(isIncome = false)
        form.amount shouldBe ""
    }

    test("item without qty/price is listed by name only") {
        val data = ReceiptScanData(
            merchant = null,
            total = "0",
            items = listOf(ReceiptScanItem("ONLYNAME", "", "")),
            rawText = ""
        )
        val form = data.toTransactionFormData(isIncome = false)
        form.description shouldBe "ONLYNAME"
    }
})
