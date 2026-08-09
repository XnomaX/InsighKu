
package com.example.insightku.core.utils

import androidx.compose.ui.graphics.Color
import com.example.insightku.core.ui.theme.AppPalette

object CategoryUtils {

    fun getColorForCategoryName(categoryName: String): Color {
        val n = categoryName.lowercase().trim()
        return when {
            n.contains("food") || n.contains("drink") || n.contains("eat") ||
            n.contains("dining") || n.contains("restaurant") || n.contains("cafe") ||
            n.contains("coffee") || n.contains("grocery") || n.contains("groceries") ||
            n.contains("market") -> AppPalette.warning

            n.contains("transport") || n.contains("car") ||
            n.contains("ride") || n.contains("taxi") || n.contains("bus") ||
            n.contains("train") || n.contains("fuel") || n.contains("petrol") ||
            n.contains("parking") || n.contains("toll") -> AppPalette.defaultBlue

            n.contains("shop") || n.contains("fashion") || n.contains("cloth") ||
            n.contains("apparel") || n.contains("retail") -> AppPalette.locationPink

            n.contains("entertain") || n.contains("movie") || n.contains("game") ||
            n.contains("hobby") || n.contains("leisure") -> AppPalette.notesPurple

            n.contains("health") || n.contains("medical") || n.contains("doctor") ||
            n.contains("hospital") || n.contains("pharmacy") || n.contains("wellness") -> AppPalette.success

            n.contains("bill") || n.contains("utility") || n.contains("electric") ||
            n.contains("water") || n.contains("internet") || n.contains("subscription") -> AppPalette.error

            n.contains("house") || n.contains("home") || n.contains("rent") ||
            n.contains("housing") -> AppPalette.gray

            n.contains("salary") || n.contains("wage") || n.contains("income") ||
            n.contains("earning") || n.contains("payroll") -> AppPalette.success

            n.contains("freelance") || n.contains("work") || n.contains("project") ||
            n.contains("contract") -> AppPalette.defaultBlue

            n.contains("invest") || n.contains("stock") || n.contains("crypto") ||
            n.contains("saving") || n.contains("deposit") -> AppPalette.notesPurple

            n.contains("business") || n.contains("revenue") || n.contains("sales") -> AppPalette.warning

            n.contains("gift") || n.contains("bonus") || n.contains("reward") -> AppPalette.warning

            n.contains("education") || n.contains("school") || n.contains("course") ||
            n.contains("book") || n.contains("study") -> AppPalette.notesPurple

            n.contains("tax") || n.contains("insurance") || n.contains("fee") -> AppPalette.gray

            n.contains("travel") || n.contains("vacation") || n.contains("flight") ||
            n.contains("hotel") -> AppPalette.cyan

            n.contains("beauty") || n.contains("salon") || n.contains("spa") -> AppPalette.locationPink

            n.contains("sport") || n.contains("gym") || n.contains("fitness") -> AppPalette.success

            else -> AppPalette.accent
        }
    }
}
