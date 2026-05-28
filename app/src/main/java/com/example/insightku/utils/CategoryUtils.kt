
package com.example.insightku.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryUtils {

    fun getIconForCategoryName(categoryName: String): ImageVector {
        val n = categoryName.lowercase().trim()
        return when {
            n.contains("food") || n.contains("drink") || n.contains("eat") ||
            n.contains("dining") || n.contains("restaurant") || n.contains("cafe") ||
            n.contains("coffee") || n.contains("lunch") || n.contains("dinner") ||
            n.contains("breakfast") || n.contains("snack") || n.contains("grocery") ||
            n.contains("groceries") || n.contains("market") -> Icons.Default.Restaurant

            n.contains("transport") || n.contains("car") ||
            n.contains("ride") || n.contains("taxi") || n.contains("grab") ||
            n.contains("gojek") || n.contains("bus") || n.contains("train") ||
            n.contains("commute") || n.contains("fuel") || n.contains("petrol") ||
            n.contains("parking") || n.contains("toll") -> Icons.Default.DirectionsCar

            n.contains("vacation") || n.contains("holiday") ||
            n.contains("trip") || n.contains("flight") || n.contains("hotel") ||
            n.contains("travel") -> Icons.Default.Flight

            n.contains("shop") || n.contains("fashion") || n.contains("cloth") ||
            n.contains("apparel") || n.contains("bag") || n.contains("shoes") ||
            n.contains("accessories") || n.contains("retail") -> Icons.Default.ShoppingCart

            n.contains("entertain") || n.contains("movie") || n.contains("cinema") ||
            n.contains("game") || n.contains("gaming") || n.contains("hobby") ||
            n.contains("fun") || n.contains("leisure") || n.contains("sport") ||
            n.contains("gym") || n.contains("fitness") -> Icons.Default.Movie

            n.contains("health") || n.contains("medical") || n.contains("doctor") ||
            n.contains("hospital") || n.contains("pharmacy") || n.contains("medicine") ||
            n.contains("clinic") || n.contains("dental") || n.contains("wellness") -> Icons.Default.LocalHospital

            n.contains("bill") || n.contains("utility") || n.contains("electric") ||
            n.contains("water") || n.contains("internet") || n.contains("phone") ||
            n.contains("subscription") || n.contains("netflix") || n.contains("spotify") ||
            n.contains("streaming") -> Icons.Default.Receipt

            n.contains("house") || n.contains("home") || n.contains("rent") ||
            n.contains("mortgage") || n.contains("property") || n.contains("housing") -> Icons.Default.Home

            n.contains("salary") || n.contains("wage") || n.contains("payroll") ||
            n.contains("income") || n.contains("earning") -> Icons.Default.AttachMoney

            n.contains("freelance") || n.contains("work") || n.contains("project") ||
            n.contains("client") || n.contains("contract") || n.contains("gig") -> Icons.Default.Work

            n.contains("invest") || n.contains("stock") || n.contains("crypto") ||
            n.contains("dividend") || n.contains("return") || n.contains("profit") ||
            n.contains("saving") || n.contains("deposit") -> Icons.Default.TrendingUp

            n.contains("business") || n.contains("company") || n.contains("revenue") ||
            n.contains("sales") || n.contains("commerce") -> Icons.Default.BusinessCenter

            n.contains("gift") || n.contains("bonus") || n.contains("reward") ||
            n.contains("present") || n.contains("prize") -> Icons.Default.CardGiftcard

            n.contains("education") || n.contains("school") || n.contains("course") ||
            n.contains("tuition") || n.contains("book") || n.contains("study") ||
            n.contains("training") || n.contains("learn") -> Icons.Default.School

            n.contains("tax") || n.contains("insurance") || n.contains("fee") ||
            n.contains("charge") || n.contains("fine") -> Icons.Default.AccountBalance

            n.contains("transfer") || n.contains("send") || n.contains("remit") -> Icons.Default.SwapHoriz

            n.contains("child") || n.contains("kid") || n.contains("baby") ||
            n.contains("family") -> Icons.Default.FamilyRestroom

            n.contains("pet") || n.contains("animal") -> Icons.Default.Pets

            n.contains("travel") || n.contains("vacation") || n.contains("holiday") ||
            n.contains("trip") || n.contains("flight") || n.contains("hotel") -> Icons.Default.Flight

            n.contains("beauty") || n.contains("salon") || n.contains("spa") ||
            n.contains("cosmetic") || n.contains("makeup") -> Icons.Default.Spa

            else -> Icons.Default.Category
        }
    }

    fun getColorForCategoryName(categoryName: String): Color {
        val n = categoryName.lowercase().trim()
        return when {
            n.contains("food") || n.contains("drink") || n.contains("eat") ||
            n.contains("dining") || n.contains("restaurant") || n.contains("cafe") ||
            n.contains("coffee") || n.contains("grocery") || n.contains("groceries") ||
            n.contains("market") -> Color(0xFFF59E0B)

            n.contains("transport") || n.contains("car") ||
            n.contains("ride") || n.contains("taxi") || n.contains("bus") ||
            n.contains("train") || n.contains("fuel") || n.contains("petrol") ||
            n.contains("parking") || n.contains("toll") -> Color(0xFF3B82F6)

            n.contains("shop") || n.contains("fashion") || n.contains("cloth") ||
            n.contains("apparel") || n.contains("retail") -> Color(0xFFEC4899)

            n.contains("entertain") || n.contains("movie") || n.contains("game") ||
            n.contains("hobby") || n.contains("leisure") -> Color(0xFF8B5CF6)

            n.contains("health") || n.contains("medical") || n.contains("doctor") ||
            n.contains("hospital") || n.contains("pharmacy") || n.contains("wellness") -> Color(0xFF10B981)

            n.contains("bill") || n.contains("utility") || n.contains("electric") ||
            n.contains("water") || n.contains("internet") || n.contains("subscription") -> Color(0xFFEF4444)

            n.contains("house") || n.contains("home") || n.contains("rent") ||
            n.contains("housing") -> Color(0xFF6B7280)

            n.contains("salary") || n.contains("wage") || n.contains("income") ||
            n.contains("earning") || n.contains("payroll") -> Color(0xFF10B981)

            n.contains("freelance") || n.contains("work") || n.contains("project") ||
            n.contains("contract") -> Color(0xFF3B82F6)

            n.contains("invest") || n.contains("stock") || n.contains("crypto") ||
            n.contains("saving") || n.contains("deposit") -> Color(0xFF8B5CF6)

            n.contains("business") || n.contains("revenue") || n.contains("sales") -> Color(0xFFF59E0B)

            n.contains("gift") || n.contains("bonus") || n.contains("reward") -> Color(0xFFF59E0B)

            n.contains("education") || n.contains("school") || n.contains("course") ||
            n.contains("book") || n.contains("study") -> Color(0xFF8B5CF6)

            n.contains("tax") || n.contains("insurance") || n.contains("fee") -> Color(0xFF6B7280)

            n.contains("travel") || n.contains("vacation") || n.contains("flight") ||
            n.contains("hotel") -> Color(0xFF06B6D4)

            n.contains("beauty") || n.contains("salon") || n.contains("spa") -> Color(0xFFEC4899)

            n.contains("sport") || n.contains("gym") || n.contains("fitness") -> Color(0xFF10B981)

            else -> Color(0xFF7C4DFF)
        }
    }
}
