package com.example.insightku.feature.analytics.presentation

import com.example.insightku.core.ui.components.dialogs.CategoryIconInfo
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver as DialogCategoryIconResolver

// Thin alias so analytics files can import from their own package.
// The authoritative implementation lives in core.ui.components.dialogs.CategoryIconResolver.
object CategoryIconResolver {
    fun resolve(categoryName: String): CategoryIconInfo =
        DialogCategoryIconResolver.resolve(categoryName)
}
