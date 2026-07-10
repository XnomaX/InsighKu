package com.example.insightku.core.ui.components.dialogs
import com.example.insightku.core.ui.components.dialogs.IconOption
import com.example.insightku.core.ui.components.dialogs.BudgetLimitInput
import com.example.insightku.core.ui.components.dialogs.RecurringPeriodSelector
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver
import com.example.insightku.core.ui.components.dialogs.CategoryIconInfo
import com.example.insightku.core.i18n.NumberFormatter

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.utils.CurrencyUtils
import kotlin.math.roundToInt

// ─── Icon Data ────────────────────────────────────────────────────────────────

data class CategoryIconInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

// ─── EXPENSE Icon Set — warm lifestyle palette ────────────────────────────────

val expenseCategoryIcons = listOf(
    // Food & Beverage
    CategoryIconInfo("Food & Drinks",   Icons.Default.Restaurant,           Color(0xFFF59E0B)),
    CategoryIconInfo("Groceries",       Icons.Default.ShoppingCart,         Color(0xFFEF4444)),
    CategoryIconInfo("Coffee & Cafes",  Icons.Default.Coffee,               Color(0xFFD97706)),
    CategoryIconInfo("Fast Food",       Icons.Default.Fastfood,             Color(0xFFEF4444)),
    CategoryIconInfo("Bakery",          Icons.Default.BakeryDining,         Color(0xFFD97706)),
    CategoryIconInfo("Drinks",          Icons.Default.LocalBar,             Color(0xFF8B5CF6)),
    CategoryIconInfo("Ice Cream",       Icons.Default.Icecream,             Color(0xFFEC4899)),
    CategoryIconInfo("Dining Out",      Icons.Default.DinnerDining,         Color(0xFFF59E0B)),
    // Shopping & Lifestyle
    CategoryIconInfo("Shopping",        Icons.Default.ShoppingBag,          Color(0xFFEC4899)),
    CategoryIconInfo("Clothing",        Icons.Default.Checkroom,            Color(0xFFDB2777)),
    CategoryIconInfo("Electronics",     Icons.Default.Devices,              Color(0xFF3B82F6)),
    CategoryIconInfo("Furniture",       Icons.Default.Chair,                Color(0xFFD97706)),
    CategoryIconInfo("Beauty",          Icons.Default.Face,                 Color(0xFFEC4899)),
    CategoryIconInfo("Accessories",     Icons.Default.Watch,                Color(0xFF8B5CF6)),
    CategoryIconInfo("Laundry",         Icons.Default.LocalLaundryService,  Color(0xFF06B6D4)),
    CategoryIconInfo("Haircut",         Icons.Default.ContentCut,           Color(0xFFEC4899)),
    CategoryIconInfo("Spa & Wellness",  Icons.Default.Spa,                  Color(0xFFEC4899)),
    // Transport
    CategoryIconInfo("Transportation",  Icons.Default.DirectionsCar,        Color(0xFF3B82F6)),
    CategoryIconInfo("Fuel",            Icons.Default.LocalGasStation,      Color(0xFFEF4444)),
    CategoryIconInfo("Train / Bus",     Icons.Default.Train,                Color(0xFF6366F1)),
    CategoryIconInfo("Taxi / Ojek",     Icons.Default.LocalTaxi,            Color(0xFFF59E0B)),
    CategoryIconInfo("Parking",         Icons.Default.LocalParking,         Color(0xFF6366F1)),
    CategoryIconInfo("Travel",          Icons.Default.Flight,               Color(0xFF06B6D4)),
    CategoryIconInfo("Hotel",           Icons.Default.Hotel,                Color(0xFF06B6D4)),
    CategoryIconInfo("Motorcycle",      Icons.Default.TwoWheeler,           Color(0xFF3B82F6)),
    CategoryIconInfo("Vacation",        Icons.Default.BeachAccess,          Color(0xFF06B6D4)),
    // Home & Bills
    CategoryIconInfo("Housing / Rent",  Icons.Default.Home,                 Color(0xFFEF4444)),
    CategoryIconInfo("Electricity",     Icons.Default.ElectricBolt,         Color(0xFFF59E0B)),
    CategoryIconInfo("Water Bill",      Icons.Default.Water,                Color(0xFF06B6D4)),
    CategoryIconInfo("Utilities",       Icons.Default.Bolt,                 Color(0xFFF59E0B)),
    CategoryIconInfo("Internet / WiFi", Icons.Default.Wifi,                 Color(0xFF3B82F6)),
    CategoryIconInfo("Phone",           Icons.Default.PhoneAndroid,         Color(0xFF8B5CF6)),
    CategoryIconInfo("Home Repair",     Icons.Default.Handyman,             Color(0xFFD97706)),
    CategoryIconInfo("Cleaning",        Icons.Default.CleaningServices,     Color(0xFF10B981)),
    // Health & Wellness
    CategoryIconInfo("Healthcare",      Icons.Default.LocalHospital,        Color(0xFF10B981)),
    CategoryIconInfo("Pharmacy",        Icons.Default.MedicalServices,      Color(0xFF059669)),
    CategoryIconInfo("Fitness",         Icons.Default.FitnessCenter,        Color(0xFF10B981)),
    CategoryIconInfo("Dental",          Icons.Default.Healing,              Color(0xFF06B6D4)),
    CategoryIconInfo("Mental Health",   Icons.Default.SelfImprovement,      Color(0xFF8B5CF6)),
    CategoryIconInfo("Vitamins",        Icons.Default.Medication,           Color(0xFF059669)),
    // Entertainment & Leisure
    CategoryIconInfo("Entertainment",   Icons.Default.SportsEsports,        Color(0xFF8B5CF6)),
    CategoryIconInfo("Movies",          Icons.Default.Movie,                Color(0xFF7C3AED)),
    CategoryIconInfo("Music",           Icons.Default.MusicNote,            Color(0xFFEC4899)),
    CategoryIconInfo("Sports",          Icons.Default.SportsSoccer,         Color(0xFF10B981)),
    CategoryIconInfo("Outdoor",         Icons.Default.Park,                 Color(0xFF059669)),
    CategoryIconInfo("Gaming",          Icons.Default.VideogameAsset,       Color(0xFF7C3AED)),
    CategoryIconInfo("Concert",         Icons.Default.TheaterComedy,        Color(0xFFEC4899)),
    CategoryIconInfo("Photography",     Icons.Default.PhotoCamera,          Color(0xFF6366F1)),
    CategoryIconInfo("Reading",         Icons.Default.AutoStories,          Color(0xFF2563EB)),
    // Education
    CategoryIconInfo("Education",       Icons.Default.School,               Color(0xFF3B82F6)),
    CategoryIconInfo("Books", Icons.AutoMirrored.Filled.MenuBook,             Color(0xFF2563EB)),
    CategoryIconInfo("Online Course",   Icons.Default.OndemandVideo,        Color(0xFF7C3AED)),
    CategoryIconInfo("Stationery",      Icons.Default.Edit,                 Color(0xFF6366F1)),
    // Subscriptions & Finance
    CategoryIconInfo("Subscriptions",   Icons.Default.Subscriptions,        Color(0xFF8B5CF6)),
    CategoryIconInfo("Loan / Cicilan",  Icons.Default.AccountBalance,       Color(0xFFEF4444)),
    CategoryIconInfo("Insurance",       Icons.Default.Security,             Color(0xFF3B82F6)),
    CategoryIconInfo("Taxes",           Icons.Default.Receipt,              Color(0xFFEF4444)),
    CategoryIconInfo("Savings",         Icons.Default.Savings,              Color(0xFF10B981)),
    CategoryIconInfo("ATM / Bank Fee",  Icons.Default.LocalAtm,             Color(0xFF6366F1)),
    // Family & Social
    CategoryIconInfo("Pet",             Icons.Default.Pets,                 Color(0xFFF59E0B)),
    CategoryIconInfo("Gift",            Icons.Default.CardGiftcard,         Color(0xFFEC4899)),
    CategoryIconInfo("Charity",         Icons.Default.VolunteerActivism,    Color(0xFF10B981)),
    CategoryIconInfo("Baby / Kids",     Icons.Default.ChildCare,            Color(0xFFEC4899)),
    CategoryIconInfo("Wedding",         Icons.Default.Celebration,          Color(0xFFDB2777)),
    CategoryIconInfo("Social",          Icons.Default.People,               Color(0xFF8B5CF6)),
    CategoryIconInfo("Others",          Icons.Default.Category,             Color(0xFF79747E))
)

// ─── INCOME Icon Set — cool prosperity palette ────────────────────────────────

val incomeCategoryIcons = listOf(
    // Employment
    CategoryIconInfo("Salary",          Icons.Default.AccountBalanceWallet, Color(0xFF10B981)),
    CategoryIconInfo("Bonus",           Icons.Default.EmojiEvents,          Color(0xFFF59E0B)),
    CategoryIconInfo("Overtime",        Icons.Default.MoreTime,             Color(0xFF059669)),
    CategoryIconInfo("Commission",      Icons.Default.Percent,              Color(0xFF06B6D4)),
    CategoryIconInfo("Allowance",       Icons.Default.CardMembership,       Color(0xFF10B981)),
    CategoryIconInfo("THR",             Icons.Default.Celebration,          Color(0xFFF59E0B)),
    // Self-Employment & Business
    CategoryIconInfo("Freelance",       Icons.Default.Laptop,               Color(0xFF06B6D4)),
    CategoryIconInfo("Business",        Icons.Default.Business,             Color(0xFF3B82F6)),
    CategoryIconInfo("Side Hustle",     Icons.Default.WorkOutline,          Color(0xFF8B5CF6)),
    CategoryIconInfo("Consulting",      Icons.Default.Psychology,           Color(0xFF7C3AED)),
    CategoryIconInfo("Online Shop",     Icons.Default.Storefront,           Color(0xFF06B6D4)),
    CategoryIconInfo("Content Creator", Icons.Default.VideoCall,            Color(0xFFEC4899)),
    CategoryIconInfo("Teaching",        Icons.Default.CastForEducation,     Color(0xFF3B82F6)),
    CategoryIconInfo("Service",         Icons.Default.MiscellaneousServices,Color(0xFF6366F1)),
    // Investment & Finance
    CategoryIconInfo("Investment", Icons.AutoMirrored.Filled.TrendingUp,           Color(0xFF06B6D4)),
    CategoryIconInfo("Dividends", Icons.AutoMirrored.Filled.ShowChart,            Color(0xFF0891B2)),
    CategoryIconInfo("Stock",           Icons.Default.BarChart,             Color(0xFF059669)),
    CategoryIconInfo("Crypto",          Icons.Default.CurrencyBitcoin,      Color(0xFFF59E0B)),
    CategoryIconInfo("Mutual Fund",     Icons.Default.PieChart,             Color(0xFF3B82F6)),
    CategoryIconInfo("Interest",        Icons.Default.Savings,              Color(0xFF10B981)),
    CategoryIconInfo("Gold",            Icons.Default.Diamond,              Color(0xFFF59E0B)),
    // Passive Income
    CategoryIconInfo("Passive Income",  Icons.Default.AutoGraph,            Color(0xFF10B981)),
    CategoryIconInfo("Rental Income",   Icons.Default.HomeWork,             Color(0xFF3B82F6)),
    CategoryIconInfo("Royalty",         Icons.Default.Copyright,            Color(0xFF8B5CF6)),
    CategoryIconInfo("Affiliate",       Icons.Default.Share,                Color(0xFF06B6D4)),
    CategoryIconInfo("Ads Revenue",     Icons.Default.Campaign,             Color(0xFFEC4899)),
    // Transfers & Misc
    CategoryIconInfo("Cashback",        Icons.Default.Redeem,               Color(0xFF10B981)),
    CategoryIconInfo("Gift / Transfer", Icons.Default.CardGiftcard,         Color(0xFFEC4899)),
    CategoryIconInfo("Refund", Icons.AutoMirrored.Filled.AssignmentReturn,     Color(0xFF06B6D4)),
    CategoryIconInfo("Grant",           Icons.Default.Stars,                Color(0xFFF59E0B)),
    CategoryIconInfo("Scholarship",     Icons.Default.School,               Color(0xFF3B82F6)),
    CategoryIconInfo("Pension",         Icons.Default.Elderly,              Color(0xFF6366F1)),
    CategoryIconInfo("Inheritance",     Icons.Default.AccountBalance,       Color(0xFF6366F1)),
    CategoryIconInfo("Lottery",         Icons.Default.ConfirmationNumber,   Color(0xFFEC4899)),
    CategoryIconInfo("Others",          Icons.Default.Category,             Color(0xFF79747E))
)

// ─── Backward-compat alias (used by existing code that references defaultCategoryIcons) ──

val defaultCategoryIcons = expenseCategoryIcons

// ─── Global CategoryIconResolver — single source of truth ────────────────────
//
// Maps a category icon name string → ImageVector + Color.
// Used by transaction list, budgeting cards, and category chips so every
// screen always renders the same icon for the same category.

object CategoryIconResolver {

    private val allIcons: Map<String, CategoryIconInfo> =
        (expenseCategoryIcons + incomeCategoryIcons).associateBy { it.name }

    fun resolve(iconName: String?): CategoryIconInfo {
        if (iconName.isNullOrBlank()) return fallback()
        // Exact match first
        allIcons[iconName]?.let { return it }
        // Fuzzy match by keyword — use safe ?: fallback() instead of !!
        val n = iconName.lowercase()
        return when {
            // ── Expense: Food & Beverage ──────────────────────────────────
            "food" in n || "drink" in n || "restaurant" in n || "makan" in n -> allIcons["Food & Drinks"] ?: fallback()
            "grocer" in n || "market" in n || "supermarket" in n             -> allIcons["Groceries"] ?: fallback()
            "coffee" in n || "cafe" in n || "kopi" in n                      -> allIcons["Coffee & Cafes"] ?: fallback()
            "fast food" in n || "fastfood" in n || "burger" in n             -> allIcons["Fast Food"] ?: fallback()
            "bakery" in n || "bread" in n || "roti" in n                     -> allIcons["Bakery"] ?: fallback()
            "bar" in n || "alcohol" in n || "beer" in n || "minuman" in n    -> allIcons["Drinks"] ?: fallback()
            "ice cream" in n || "dessert" in n || "snack" in n               -> allIcons["Ice Cream"] ?: fallback()
            "dining" in n || "makan malam" in n                              -> allIcons["Dining Out"] ?: fallback()
            // ── Expense: Shopping & Lifestyle ────────────────────────────
            "shop" in n || "mall" in n || "belanja" in n                     -> allIcons["Shopping"] ?: fallback()
            "cloth" in n || "fashion" in n || "baju" in n || "pakaian" in n  -> allIcons["Clothing"] ?: fallback()
            "electron" in n || "gadget" in n || "laptop" in n                -> allIcons["Electronics"] ?: fallback()
            "furnitur" in n || "perabot" in n                                -> allIcons["Furniture"] ?: fallback()
            "beauty" in n || "makeup" in n || "kosmetik" in n                -> allIcons["Beauty"] ?: fallback()
            "accessori" in n || "watch" in n || "jam" in n                   -> allIcons["Accessories"] ?: fallback()
            "laundry" in n || "cuci" in n                                    -> allIcons["Laundry"] ?: fallback()
            "haircut" in n || "salon" in n || "barber" in n || "potong" in n -> allIcons["Haircut"] ?: fallback()
            "spa" in n || "wellness" in n || "pijat" in n                    -> allIcons["Spa & Wellness"] ?: fallback()
            // ── Expense: Transport ───────────────────────────────────────
            "transport" in n || "car" in n || "mobil" in n                   -> allIcons["Transportation"] ?: fallback()
            "fuel" in n || "gas" in n || "petrol" in n || "bensin" in n      -> allIcons["Fuel"] ?: fallback()
            "train" in n || "bus" in n || "commut" in n || "kereta" in n     -> allIcons["Train / Bus"] ?: fallback()
            "taxi" in n || "ojek" in n || "grab" in n || "gojek" in n        -> allIcons["Taxi / Ojek"] ?: fallback()
            "parking" in n || "parkir" in n                                  -> allIcons["Parking"] ?: fallback()
            "travel" in n || "flight" in n || "trip" in n || "liburan" in n  -> allIcons["Travel"] ?: fallback()
            "hotel" in n || "penginapan" in n || "villa" in n                -> allIcons["Hotel"] ?: fallback()
            "motor" in n || "motorcycle" in n || "sepeda" in n               -> allIcons["Motorcycle"] ?: fallback()
            "vacation" in n || "pantai" in n || "wisata" in n                -> allIcons["Vacation"] ?: fallback()
            // ── Expense: Home & Bills ────────────────────────────────────
            "home" in n || "rent" in n || "hous" in n || "kos" in n || "sewa" in n -> allIcons["Housing / Rent"] ?: fallback()
            "util" in n || "listrik" in n || "pln" in n || "electric" in n   -> allIcons["Electricity"] ?: fallback()
            "water" in n || "air" in n || "pdam" in n                        -> allIcons["Water Bill"] ?: fallback()
            "wifi" in n || "internet" in n || "indihome" in n                -> allIcons["Internet / WiFi"] ?: fallback()
            "phone" in n || "mobile" in n || "pulsa" in n || "telpon" in n   -> allIcons["Phone"] ?: fallback()
            "repair" in n || "renovasi" in n || "servis" in n                -> allIcons["Home Repair"] ?: fallback()
            "clean" in n || "bersih" in n                                    -> allIcons["Cleaning"] ?: fallback()
            "bill" in n                                                       -> allIcons["Utilities"] ?: fallback()
            // ── Expense: Health ──────────────────────────────────────────
            "health" in n || "hospital" in n || "clinic" in n || "dokter" in n -> allIcons["Healthcare"] ?: fallback()
            "pharma" in n || "medicine" in n || "obat" in n || "apotek" in n -> allIcons["Pharmacy"] ?: fallback()
            "fitness" in n || "gym" in n || "olahraga" in n                  -> allIcons["Fitness"] ?: fallback()
            "dental" in n || "gigi" in n                                     -> allIcons["Dental"] ?: fallback()
            "mental" in n || "psikolog" in n || "terapi" in n                -> allIcons["Mental Health"] ?: fallback()
            "vitamin" in n || "suplemen" in n                                -> allIcons["Vitamins"] ?: fallback()
            // ── Expense: Entertainment ───────────────────────────────────
            "entertain" in n || "game" in n || "gaming" in n                 -> allIcons["Entertainment"] ?: fallback()
            "movie" in n || "cinema" in n || "film" in n || "bioskop" in n   -> allIcons["Movies"] ?: fallback()
            "music" in n || "spotify" in n || "konser" in n                  -> allIcons["Music"] ?: fallback()
            "sport" in n || "futsal" in n || "badminton" in n                -> allIcons["Sports"] ?: fallback()
            "outdoor" in n || "hiking" in n || "camping" in n                -> allIcons["Outdoor"] ?: fallback()
            "concert" in n || "theater" in n || "pertunjukan" in n           -> allIcons["Concert"] ?: fallback()
            "photo" in n || "kamera" in n                                    -> allIcons["Photography"] ?: fallback()
            "reading" in n || "baca" in n                                    -> allIcons["Reading"] ?: fallback()
            // ── Expense: Education ───────────────────────────────────────
            "edu" in n || "school" in n || "sekolah" in n || "kuliah" in n   -> allIcons["Education"] ?: fallback()
            "book" in n || "buku" in n                                       -> allIcons["Books"] ?: fallback()
            "course" in n || "kursus" in n || "les" in n || "online" in n    -> allIcons["Online Course"] ?: fallback()
            "stationer" in n || "alat tulis" in n                            -> allIcons["Stationery"] ?: fallback()
            // ── Expense: Finance ─────────────────────────────────────────
            "subscri" in n || "netflix" in n || "langganan" in n             -> allIcons["Subscriptions"] ?: fallback()
            "loan" in n || "cicilan" in n || "debt" in n || "hutang" in n    -> allIcons["Loan / Cicilan"] ?: fallback()
            "insur" in n || "asuransi" in n                                  -> allIcons["Insurance"] ?: fallback()
            "tax" in n || "pajak" in n                                       -> allIcons["Taxes"] ?: fallback()
            "saving" in n || "tabung" in n                                   -> allIcons["Savings"] ?: fallback()
            "atm" in n || "bank fee" in n || "admin" in n                    -> allIcons["ATM / Bank Fee"] ?: fallback()
            // ── Expense: Family & Social ─────────────────────────────────
            "pet" in n || "animal" in n || "hewan" in n                      -> allIcons["Pet"] ?: fallback()
            "gift" in n || "present" in n || "hadiah" in n                   -> allIcons["Gift"] ?: fallback()
            "charit" in n || "donat" in n || "sedekah" in n || "zakat" in n  -> allIcons["Charity"] ?: fallback()
            "baby" in n || "kids" in n || "anak" in n || "bayi" in n         -> allIcons["Baby / Kids"] ?: fallback()
            "wedding" in n || "nikah" in n || "pernikahan" in n              -> allIcons["Wedding"] ?: fallback()
            "social" in n || "hangout" in n || "nongkrong" in n              -> allIcons["Social"] ?: fallback()
            // ── Income: Employment ───────────────────────────────────────
            "salary" in n || "gaji" in n                                     -> allIcons["Salary"] ?: fallback()
            "bonus" in n || "thr" in n                                       -> allIcons["Bonus"] ?: fallback()
            "overtime" in n || "lembur" in n                                 -> allIcons["Overtime"] ?: fallback()
            "commission" in n || "komisi" in n                               -> allIcons["Commission"] ?: fallback()
            "allowance" in n || "tunjangan" in n || "uang saku" in n         -> allIcons["Allowance"] ?: fallback()
            // ── Income: Self-Employment ──────────────────────────────────
            "freelanc" in n                                                  -> allIcons["Freelance"] ?: fallback()
            "business" in n || "bisnis" in n || "usaha" in n                 -> allIcons["Business"] ?: fallback()
            "side" in n || "hustle" in n || "sampingan" in n                 -> allIcons["Side Hustle"] ?: fallback()
            "consult" in n || "konsultan" in n                               -> allIcons["Consulting"] ?: fallback()
            "online shop" in n || "jualan" in n || "toko" in n               -> allIcons["Online Shop"] ?: fallback()
            "content" in n || "creator" in n || "youtuber" in n              -> allIcons["Content Creator"] ?: fallback()
            "teach" in n || "ngajar" in n                                    -> allIcons["Teaching"] ?: fallback()
            "service" in n || "jasa" in n                                    -> allIcons["Service"] ?: fallback()
            // ── Income: Investment ───────────────────────────────────────
            "invest" in n || "investasi" in n                                -> allIcons["Investment"] ?: fallback()
            "dividend" in n || "dividen" in n                                -> allIcons["Dividends"] ?: fallback()
            "stock" in n || "saham" in n                                     -> allIcons["Stock"] ?: fallback()
            "crypto" in n || "bitcoin" in n || "kripto" in n                 -> allIcons["Crypto"] ?: fallback()
            "mutual" in n || "reksa" in n || "reksadana" in n                -> allIcons["Mutual Fund"] ?: fallback()
            "interest" in n || "bunga" in n                                  -> allIcons["Interest"] ?: fallback()
            "gold" in n || "emas" in n                                       -> allIcons["Gold"] ?: fallback()
            // ── Income: Passive ──────────────────────────────────────────
            "passive" in n || "pasif" in n                                   -> allIcons["Passive Income"] ?: fallback()
            "rental" in n || "sewa" in n                                     -> allIcons["Rental Income"] ?: fallback()
            "royalt" in n || "royalti" in n                                  -> allIcons["Royalty"] ?: fallback()
            "affiliat" in n                                                  -> allIcons["Affiliate"] ?: fallback()
            "ads" in n || "iklan" in n || "adsense" in n                     -> allIcons["Ads Revenue"] ?: fallback()
            // ── Income: Transfers & Misc ─────────────────────────────────
            "cashback" in n || "reward" in n                                 -> allIcons["Cashback"] ?: fallback()
            "refund" in n || "return" in n || "kembalian" in n               -> allIcons["Refund"] ?: fallback()
            "grant" in n                                                     -> allIcons["Grant"] ?: fallback()
            "scholar" in n || "beasiswa" in n                                -> allIcons["Scholarship"] ?: fallback()
            "pension" in n || "pensiun" in n                                 -> allIcons["Pension"] ?: fallback()
            "inherit" in n || "warisan" in n                                 -> allIcons["Inheritance"] ?: fallback()
            "lottery" in n || "lotre" in n || "undian" in n                  -> allIcons["Lottery"] ?: fallback()
            else -> fallback()
        }
    }

    fun resolveIcon(iconName: String?): ImageVector = resolve(iconName).icon
    fun resolveColor(iconName: String?): Color = resolve(iconName).color

    private fun fallback() = CategoryIconInfo(
        name  = "Others",
        icon  = Icons.Default.Category,
        color = Color(0xFF79747E) // muted gray — intentionally not in AppPalette
    )
}

// ─── Shared Composables ───────────────────────────────────────────────────────

@Composable
fun IconOption(
    iconData: CategoryIconInfo,
    isSelected: Boolean,
    accentColor: Color = com.example.insightku.core.ui.theme.AppPalette.accent,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else com.example.insightku.core.ui.theme.AppPalette.card)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else com.example.insightku.core.ui.theme.AppPalette.cardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconData.color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconData.icon,
                contentDescription = iconData.name,
                tint = iconData.color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Category Type Selector ───────────────────────────────────────────────────

@Composable
internal fun CategoryTypeSelector(
    selected: CategoryType,
    onSelect: (CategoryType) -> Unit
) {
    val purple = com.example.insightku.core.ui.theme.AppPalette.accent
    val green  = com.example.insightku.core.ui.theme.AppPalette.success
    val border = com.example.insightku.core.ui.theme.AppPalette.cardBorder

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CategoryType.entries.forEach { type ->
            val isSelected   = selected == type
            val activeColor  = if (type == CategoryType.EXPENSE) purple else green
            val bgColor      = if (isSelected) activeColor else com.example.insightku.core.ui.theme.AppPalette.card
            val contentColor = if (isSelected) Color.White else com.example.insightku.core.ui.theme.AppPalette.textDialogMuted
            val borderColor  = if (isSelected) activeColor else border

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onSelect(type) },
                shape  = RoundedCornerShape(50.dp),
                color  = bgColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text       = if (type == CategoryType.EXPENSE) stringResource(R.string.type_expense) else stringResource(R.string.type_income),
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color      = contentColor
                    )
                }
            }
        }
    }
}

// ─── Recurring Period Selector ────────────────────────────────────────────────

@Composable
private fun getRecurringPeriods(): List<Pair<String, String>> = listOf(
    stringResource(R.string.period_weekly) to "Weekly",
    stringResource(R.string.dialog_recurring_monthly) to "Monthly",
    stringResource(R.string.dialog_recurring_yearly) to "Yearly"
)

@Composable
fun RecurringPeriodSelector(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val purple = com.example.insightku.core.ui.theme.AppPalette.accent
    val border = com.example.insightku.core.ui.theme.AppPalette.cardBorder

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val noneSelected = selected == null
        val noneBg by animateColorAsState(
            targetValue   = if (noneSelected) purple else com.example.insightku.core.ui.theme.AppPalette.card,
            animationSpec = tween(180),
            label         = "none_bg"
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(noneBg)
                .border(1.dp, if (noneSelected) purple else border, RoundedCornerShape(50.dp))
                .clickable { onSelect(null) },
            contentAlignment = Alignment.Center
        ) {                Text(
                    text       = stringResource(R.string.dialog_recurring_none),
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = if (noneSelected) FontWeight.Bold else FontWeight.Normal,
                color      = if (noneSelected) Color.White else com.example.insightku.core.ui.theme.AppPalette.textDialogMuted
            )
        }

        getRecurringPeriods().forEach { (label, value) ->
            val isSelected = selected == value
            val bg by animateColorAsState(
                targetValue   = if (isSelected) purple else com.example.insightku.core.ui.theme.AppPalette.card,
                animationSpec = tween(180),
                label         = "period_bg_$value"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(bg)
                    .border(1.dp, if (isSelected) purple else border, RoundedCornerShape(50.dp))
                    .clickable { onSelect(if (isSelected) null else value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color      = if (isSelected) Color.White else com.example.insightku.core.ui.theme.AppPalette.textDialogMuted
                )
            }
        }
    }
}

// ─── Budget Limit Input ───────────────────────────────────────────────────────

@Composable
fun BudgetLimitInput(
    budgetLimitText: String,
    onBudgetLimitTextChange: (String) -> Unit,
    alertThreshold: Float,
    onAlertThresholdChange: (Float) -> Unit
) {
    val purple      = com.example.insightku.core.ui.theme.AppPalette.accent
    val border      = com.example.insightku.core.ui.theme.AppPalette.cardBorder
    val parsedLimit = budgetLimitText.filter { it.isDigit() }.toLongOrNull() ?: 0L

    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = com.example.insightku.core.ui.theme.AppPalette.card,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {                Text(
                    text       = stringResource(R.string.dialog_monthly_budget),
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.example.insightku.core.ui.theme.AppPalette.textDialogMuted
                )
                if (parsedLimit > 0) {
                    Text(
                        text       = NumberFormatter.formatCurrency(parsedLimit.toDouble()),
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = purple
                    )
                }
            }
            OutlinedTextField(
                value         = com.example.insightku.core.utils.CurrencyUtils.formatInputThousands(budgetLimitText),
                onValueChange = { input -> onBudgetLimitTextChange(com.example.insightku.core.utils.CurrencyUtils.stripThousands(input)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                placeholder   = {
                    Text("e.g. 2.000.000", color = com.example.insightku.core.ui.theme.AppPalette.placeholder, style = MaterialTheme.typography.bodyMedium)
                },
                leadingIcon   = {
                    Text(NumberFormatter.getCurrencySymbol(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = purple)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape  = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = purple,
                    unfocusedBorderColor    = border,
                    focusedContainerColor   = com.example.insightku.core.ui.theme.AppPalette.card,
                    unfocusedContainerColor = com.example.insightku.core.ui.theme.AppPalette.card
                )
            )
        }
    }

    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = com.example.insightku.core.ui.theme.AppPalette.card,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.dialog_alert_at), style = MaterialTheme.typography.bodyMedium, color = com.example.insightku.core.ui.theme.AppPalette.textDialogMuted)
                Text(
                    stringResource(R.string.dialog_pct_of_budget, alertThreshold.roundToInt()),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color      = purple
                )
            }
            if (parsedLimit > 0) {
                Text(
                    "≈ ${NumberFormatter.formatCurrency((parsedLimit * alertThreshold / 100).toDouble())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = com.example.insightku.core.ui.theme.AppPalette.textMuted
                )
            }
            Slider(
                value         = alertThreshold,
                onValueChange = onAlertThresholdChange,
                valueRange    = 50f..100f,
                steps         = 9,
                colors        = SliderDefaults.colors(
                    thumbColor        = purple,
                    activeTrackColor  = purple,
                    inactiveTrackColor = border
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("50%",  style = MaterialTheme.typography.labelSmall, color = com.example.insightku.core.ui.theme.AppPalette.placeholder)
                Text("100%", style = MaterialTheme.typography.labelSmall, color = com.example.insightku.core.ui.theme.AppPalette.placeholder)
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

internal fun Int.formatCurrency(): String =
    NumberFormatter.formatInteger(this.toLong())








