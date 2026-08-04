# Technical / Architecture Specification

## Purpose

This document describes InsighKu's current technical baseline and the target integration boundaries for the thesis scope. It supports consistent implementation and provides a concise technical reference for the methodology chapter.

## System Overview

InsighKu is a native Android application built with Kotlin and Jetpack Compose. It is offline-first: Room stores operational data locally, while confirmed financial records synchronize to Firebase Firestore when a network connection is available.

Primary architecture style:

- Clean Architecture and MVVM
- Feature-first package organization
- Repository pattern for local/remote data access
- Hilt dependency injection
- Kotlin `Flow` / `StateFlow` reactive state
- WorkManager for deferred and periodic work

## Technology Stack

| Area | Technology |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Language | Kotlin |
| State | ViewModel, `StateFlow`, Compose state |
| Dependency injection | Hilt |
| Local persistence | Room |
| Preferences | DataStore Preferences |
| Remote sync/authentication | Firebase Auth, Cloud Firestore |
| Background jobs | WorkManager with HiltWorkerFactory |
| Navigation | Navigation Compose |
| Charts | Vico |
| Animation | Lottie |
| Authentication | Email/password and Google Credential Manager |
| Minimum Android version | API 24 |

LSTM inference deployment and OCR engine/library are `[ASUMSI - perlu konfirmasi]`; neither was found in the current dependency graph.

## Repository Structure

```text
InsighKu/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/example/insightku/
│           ├── InsightKuApplication.kt
│           ├── MainActivity.kt
│           ├── core/
│           │   ├── data/          # Room models, DAOs, repositories, preferences
│           │   ├── di/            # Hilt modules and Room provisioning
│           │   ├── domain/        # Shared domain contracts/models
│           │   ├── i18n/          # Locale, number/date, copy helpers
│           │   ├── navigation/    # Root/auth/main routes and graphs
│           │   ├── notification/  # Bank notification listener and parser
│           │   ├── ui/            # Theme, reusable components, root/main UI
│           │   ├── utils/         # Cross-cutting helpers and ErrorBus
│           │   └── worker/        # Sync, reminders, allocation, scheduled work
│           └── feature/
│               ├── auth/
│               ├── accounts/
│               ├── analytics/
│               ├── home/
│               ├── planning/
│               │   ├── budget/
│               │   └── goal/
│               └── settings/
├── docs/
│   ├── PRD.md
│   ├── TECHNICAL_ARCHITECTURE.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── TEST_PLAN.md
│   └── KNOWN_ISSUES.md
└── DESIGN.md
```

## Feature Responsibilities

| Feature | Primary responsibility |
|---|---|
| `auth` | Registration, login, Google sign-in, password reset |
| `home` | Dashboard, transaction CRUD, quick logging, streak, transaction detail, receipt scanner UI shell |
| `analytics` | Transaction-derived behavioral analytics and narrative insights |
| `planning/budget` | Budgets, recurring payments, installments, planning entry point |
| `planning/goal` | Savings goals, contributions, reserved balances, auto-allocation rules |
| `accounts` | Cash, bank, e-wallet, card accounts and allocation visibility |
| `settings` | Preferences, theme/accent, locale, smart-capture consent/whitelist |
| `notification` | Notification listener, parser, draft creation, confidence and deduplication |

## Navigation and State Management

### Navigation

`RootNavGraph` starts at Splash and routes to either Auth or Main based on the authenticated session.

```text
Splash
├── Auth graph
│   ├── Login
│   ├── Sign Up
│   └── Forgot Password
└── Main graph
    ├── Dashboard / Home
    ├── Analytics
    ├── Budgeting / Goals
    ├── Accounts
    ├── Settings
    ├── Transaction detail / edit
    ├── Goal detail
    ├── Auto-detection onboarding
    └── Bank whitelist
```

Main tab routes are Home, Analytics, Budgeting, and Accounts. The central add action opens transaction creation.

### State pattern

Each screen-level ViewModel exposes immutable `UiState` using `StateFlow`. UI emits typed events to the ViewModel; the ViewModel invokes use cases/repositories and updates state.

```text
Compose screen
  → typed Event
  → ViewModel
  → Use case / Repository
  → Room and/or Firestore
  → Flow emission
  → UiState update
  → Compose recomposition
```

Examples:

- `DashboardViewModel` aggregates transactions, budgets, goals, accounts, drafts, recurring payments, and streak data into `DashboardUiState`.
- `AnalyticsViewModel` subscribes to `GetAnalyticsInsightsUseCase` and exposes analytics state by selected period.
- `AddTransactionViewModel` calls `AddTransactionUseCase`, then removes the source draft only after confirmed transaction save.
- `ErrorBus` is a shared error channel rendered by the root UI as a snackbar.

## Data Model and Database Schema

### Persistence design

`InsightKuDatabase` is the Room database. Confirmed entities may synchronize to Firestore. Draft transactions are intentionally local-only.

| Entity | Purpose | Sync behavior |
|---|---|---|
| `Transaction` | Financial ledger record | Room first; Firestore sync using `isSynced` |
| `Account` | Cash, bank, wallet, card account | Synced through repository flow |
| `Category` | Income/expense categorization | Local and repository-managed |
| `Budget` | Budget configuration | Repository-managed |
| `RecurringBudget` | Scheduled recurring payment/budget | Repository-managed |
| `Installment` | Installment schedule | Repository-managed |
| `DraftTransaction` | Bank-notification or allocation draft | Device-local only; never Firestore |
| `Goal` | Savings goal | Synced through goal repository |
| `Contribution` | Goal contribution/withdrawal history | Synced with goal data |
| `GoalAccount` | Goal/account relationship | Local/repository-managed |
| `ReservedBalance` | Amount reserved for goals | Local/repository-managed |
| `AutoAllocationRule` | Rule for income/expense allocation | Local/repository-managed |
| `DailyTarget` | Goal daily target data | Local/repository-managed |
| `User` | User profile/cache data | Auth/Firestore related |

### Transaction model

`Transaction` is the central financial record and has fields for:

- `id`, `title`, `amount`, `category`, `date`, `type`, `description`
- `accountId`, optional related account and transfer IDs
- optional goal metadata and source-module metadata
- `receiptPath` for future OCR receipt attachment
- `isSynced` for offline-first synchronization
- `createdAt` for device creation time and sync/dedup context

Each transaction belongs to one account. Analytics and budget calculations consume confirmed transactions; draft records are structurally excluded.

### Draft transaction model

`DraftTransaction` stores parser or auto-allocation suggestions:

- Guessed amount, merchant, type, bank, category
- Source package, raw notification title/content, confidence, status
- Deduplication hash
- Optional allocation rule/goal/source-account context

Raw notification content stays on device. The user must review before a draft becomes a confirmed transaction.

### Database safety requirement

The current database builder uses destructive fallback migration. This is a P0 issue. Before research data collection, explicit Room migrations must be added and destructive fallback removed.

## Repository and Use-Case Flows

### Manual transaction flow

```text
AddTransactionDialog
→ AddTransactionViewModel.addTransaction()
→ AddTransactionUseCase(transaction)
→ TransactionRepository.addTransaction()
→ Insert Room record with isSynced = false
→ Attempt Firestore sync
→ Mark synced on success or schedule retry worker
→ Evaluate AutoAllocationEngine
→ Auto-execute allocation or create review draft
→ Room Flow updates dashboard and analytics
```

### Bank-notification draft flow

```text
BankNotificationListenerService
→ verify supported package / user whitelist
→ BankNotificationParser.parse()
→ DraftTransactionManager
→ DraftTransactionRepository inserts device-local draft
→ Dashboard Draft Inbox
→ user reviews / edits / confirms
→ AddTransactionUseCase
→ confirmed transaction record
```

The parser supports multiple Indonesian bank and e-wallet packages and assigns HIGH, MEDIUM, or LOW confidence. It must not bypass user review.

### Analytics flow

```text
Transaction Room Flow
→ GetTransactionsUseCase
→ GetAnalyticsInsightsUseCase
→ InsightEngine.derive(transactions, now, period)
→ AnalyticsInsights
→ AnalyticsViewModel.UiState
→ AnalyticsScreen
```

The analytics engine currently derives descriptive and behavioral results such as spending mood, comparison, patterns, heatmaps, personalities, and notable transactions. The planned prescriptive layer should be a pure engine that consumes analytics, budgets, goals, balances, and LSTM outputs, then returns ranked typed insights.

### Forecast flow — target

```text
Confirmed transaction history
→ preprocessing and validation
→ LSTM inference [ASUMSI - perlu konfirmasi: on-device or service]
→ seven daily expense predictions
→ projected balance calculation
→ forecast state and risk classification
→ Dashboard and prescriptive insight engine
```

A forecast must return explicit `insufficient_history`, `loading`, `ready`, `error`, or `risk` states. It must not be represented as a certainty.

### OCR flow — target

```text
Camera or gallery image
→ image preparation
→ OCR engine [ASUMSI - perlu konfirmasi]
→ merchant / total / date extraction
→ suggested category
→ editable review
→ confirmed Transaction with receiptPath
```

OCR confidence and field-level errors should be visible to the user. No extracted field may be saved without review.

## Background Work

`InsightKuApplication` configures WorkManager through `HiltWorkerFactory` and schedules background operations.

| Worker / helper | Responsibility |
|---|---|
| `SyncTransactionWorker` | Retry unsynced transaction upload |
| `SyncGoalWorker` | Retry goal/contribution synchronization |
| `AutoTransactionWorker` | Create due recurring and installment transactions |
| `ScheduledAllocationWorker` | Trigger scheduled allocation processing |
| `AllocationSafetyNetWorker` | Retry/safety processing for allocation flow |
| `DraftReminderWorker` | Remind user about pending drafts |
| `PaymentReminderWorker` | Check and notify payment reminders |
| `GoalReminderWorker` | Check and notify goal reminders |
| notification keepalive/restart services | Improve listener recovery after OS interruption |

## Security and Privacy

- Firebase authentication controls user identity.
- Financial data is local-first and syncs after confirmation.
- Notification permission is explicit and followed by a source whitelist.
- Notification raw content and drafts are local-only.
- OCR permissions, image retention, and storage policy are `[ASUMSI - perlu konfirmasi]` and must be documented before usability testing.
- Do not log raw receipt images, tokens, passwords, or raw notification content in release builds.

## Architecture Quality Rules

- New screen business logic belongs in a ViewModel/use case, not a Composable.
- Composables render immutable state and emit typed events.
- Repositories own local/remote synchronization boundaries.
- Room is the operational source of truth for UI flows.
- Drafts never enter analytics/budget computations until user confirmation.
- New ML/OCR integrations must expose testable interfaces and explicit failure states.
- Design tokens and shared UI components are mandatory for reworked screens.
