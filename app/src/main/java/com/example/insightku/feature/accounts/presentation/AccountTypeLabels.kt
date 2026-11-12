package com.example.insightku.feature.accounts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.data.model.AccountType

/**
 * Localized display label for an [AccountType].
 *
 * Replaces `AccountType.displayName`, which was hardcoded English (`"Bank Account"`,
 * `"Credit Card"`) and rendered verbatim in the accounts list header, the row subtitle, the
 * detail sheet and as icon content descriptions — so those never followed the app language.
 * That enum property became unused once every render site moved here, and was removed.
 *
 * No stored data changes: `Account.accountType` persists `AccountType.name` (e.g.
 * `BANK_ACCOUNT`), never the display text. The helper lives in the presentation layer
 * rather than on the enum, mirroring `TransactionTypePresentation`, because a
 * `core/data/model` type should not depend on `R`.
 *
 * Full vs short: the type chips have little horizontal room and already used the short
 * strings ("Bank", "Card"), while the list row and detail sheet used the fuller
 * `displayName`. Both forms are kept so no English copy silently changes length.
 */
@Composable
internal fun accountTypeLabel(type: AccountType): String = stringResource(
    when (type) {
        AccountType.CASH -> R.string.account_type_cash
        AccountType.BANK_ACCOUNT -> R.string.account_type_bank_account
        AccountType.E_WALLET -> R.string.account_type_ewallet
        AccountType.CREDIT_CARD -> R.string.account_type_credit_card
    }
)

/** Compact label for the type-selection chips. */
@Composable
internal fun accountTypeShortLabel(type: AccountType): String = stringResource(
    when (type) {
        AccountType.CASH -> R.string.account_type_cash
        AccountType.BANK_ACCOUNT -> R.string.account_type_bank
        AccountType.E_WALLET -> R.string.account_type_ewallet
        AccountType.CREDIT_CARD -> R.string.account_type_card
    }
)
