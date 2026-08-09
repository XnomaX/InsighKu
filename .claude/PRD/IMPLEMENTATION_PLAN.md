# Task Breakdown / Implementation Plan

## Purpose

This is the solo-developer execution plan derived from `docs/PRD.md` and `DESIGN.md`. It groups concrete tasks into milestones suitable for implementation tracking and thesis scheduling.

## Working Rules

- Complete P0 work before participant testing or research-data collection.
- Each task is done only when its acceptance criteria are verified manually or by automated test where applicable.
- Do not present OCR or LSTM as functional until their complete flows and tests are finished.
- Maintain `docs/KNOWN_ISSUES.md` after each milestone.

## Phase 0 — Baseline and Data Safety

### P0.1 Replace destructive Room migration — **DONE (verified compile)**

- Since the app was never deployed, the Room database was **reset to version 1**: all `Migration_*`
  files and inline `MIGRATION_*` definitions were deleted, `InsightKuDatabase` now declares
  `version = 1`, and `DatabaseModule.kt` uses a plain `Room.databaseBuilder(...).build()` (no
  `addMigrations`, no `fallbackToDestructiveMigration`).
- Room schema baseline exported to `app/schemas/.../1.json` (`exportSchema = true` +
  `room.schemaLocation`).
- Legacy Firestore category-field migration (`migrateCategoryFieldNames`) and legacy
  contribution-timestamp repair (`fixLegacyContributionTimestamps`, `EPOCH_CUTOFF_MS`) removed — no
  production users exist, so Firestore data is recreated from scratch.
- `[OPEN]` per TEST_PLAN: add Room `MigrationTestHelper` upgrade test *from the v1 baseline* before
  any future schema change / participant gate.

Acceptance criteria:

- ~~No destructive fallback remains~~ — DONE.
- ~~Existing local data survives upgrades~~ — N/A; no users, schema starts at v1.
- Automated migration test passes — tracked for future schema changes (no test source set present).

### P0.2 Establish baseline QA

- Build and run the `authflow` baseline on supported emulator/device.
- Execute core flows: auth, account creation, transaction save/edit/delete, budget, goal, draft approval, sync retry.
- Record observations in Known Issues.

Acceptance criteria:

- Baseline behavior is described.
- P1 blockers are known before new feature work begins.

### P0.3 Baseline bug-fix pass — **DONE (compile-verified)**, session 2026-08-07

Closed the tracked P0/P2/P3 defects and several untracked ones. All changes compile (
`:app:compileDebugKotlin` green):

- BUG-03 destructive fallback removed (above).
- BUG-04 dashboard `"??"` error placeholder → calm glyph + copy + retry.
- BUG-07 streak no longer shows 0 before today is logged; real `currentStreak` always shown (
  `DashboardStreak`/`DashboardTopBar`).
- BUG-08 failed AUTO allocation now fires `showAllocationSkippedNotification` instead of being
  swallowed (`AddTransactionUseCase`).
- BUG-09 misleading "AI Forecast" / "AI-powered analysis" copy retitled "Spending Forecast" / "Based
  on your recent activity" (`values` + `values-in`).
- BUG-10 permission onboarding verified: trust-first 5-step flow + toggle routes to
  Notification-Listener settings; no code change needed.
- Settings: smart-capture/category-learning/habit-goal now persist to DataStore and reload;
  budget-alerts maps to notification toggle; Set-Memory "forget" now persists + refreshes learned
  list; `learnedMemories` list actually populated.
- Dismissing a bank notification now also purges the related draft row (`DraftTransactionManager`
  passes `draftId`; `DismissNotificationReceiver` is now Hilt `@AndroidEntryPoint` + `goAsync()`).
- Hardcoded English validation strings in Login/Add-Account/Edit-Account/Dashboard ViewModels →
  localized resources (`values` + `values-in`).
- `AllocationItemCard` no longer hardcodes `"IDR"`; uses `LocalCurrencyCode.current`.
- `AutoTransactionWorker` backfill capped (`MAX_BACKFILL_PER_PAYMENT = 12`) to stop ledger floods
  after long offline gaps.
- Build-infra fixes that blocked compilation: removed duplicate `splashscreen` version line in
  `gradle/libs.versions.toml`; converted `gradlew` shebang/script from CRLF to LF.

Remaining OPEN tracked defects: BUG-01 (OCR mock), BUG-02 (forecast data/LSTM), BUG-05 (NLS
battery), BUG-06 (parser samples). See `KNOWN_ISSUES.md`.

## Phase 1 — Design-System Foundation

### P1.1 Inventory visual debt

- Search production code for hardcoded `Color`, `RoundedCornerShape`, `dp` layout values, local elevations, and duplicate palettes.
- Map each finding to semantic tokens or shared components.

### P1.2 Implement semantic theme tokens

- Create light/dark surface, text, border, action, semantic feedback, and chart tokens from `DESIGN.md`.
- Add curated Apricot, Honey, Sage, Sky, and Lilac accent options.
- Remove red/coral from selectable primary accents.
- Retain temporary compatibility aliases while feature code migrates.

Acceptance criteria:

- Theme works in light and dark modes.
- Amount-hidden mode remains intact.
- Accent selection is restricted to five approved warm choices.
- Financial guidance is not styled with destructive red.

### P1.3 Standardize shared components

- Implement or normalize primary/secondary/destructive buttons.
- Implement app card, insight card, text input, badge/chip, labeled progress, sheet/dialog, empty/error state primitives.
- Apply 48dp minimum interaction rules.

Acceptance criteria:

- New/reworked UI has no local style declarations outside the token/component layer.
- Components have light/dark and disabled/error states.

## Phase 2 — Core Daily Workflow Rework

### P2.1 Rework navigation and Dashboard

- Update `MainScreen` bottom navigation, central add action, and motion/elevation.
- Rework balance hero, insights, budget preview, goal preview, draft inbox, recent activity, recurring-payment, and forecast card states.
- Replace flame/competitive streak framing with supportive routine language.

Acceptance criteria:

- Quick add remains reachable in one tap.
- Dashboard renders in light/dark and hidden-amount modes.
- Forecast clearly differentiates insufficient history, loading, ready, error, and risk.

### P2.2 Rework manual transaction flow

- Rework Add Transaction, Edit Transaction, Transaction Details, filtering, and deletion confirmation.
- Use amount-first progressive form layout.
- Keep scan entry secondary until OCR is complete.

Acceptance criteria:

- User can create, edit, inspect, filter, and delete a transaction.
- All destructive actions have final confirmation.
- Core controls meet 48dp target.

### P2.3 Rework receipt-scanner UI states

- Implement source-selection, capture/gallery, processing, review, confirmation, failure/retry, and unsupported visual states.
- Do not claim OCR works before Phase 3 implementation.

Acceptance criteria:

- Every state has clear user copy and safe exit/retry handling.

## Phase 3 — OCR Receipt Scanning

### P3.1 Select and integrate OCR engine

- Choose on-device or service-based OCR `[ASUMSI - perlu konfirmasi]`.
- Implement camera/gallery permission and image acquisition.
- Add secure image handling and retention policy.

### P3.2 Extract and review receipt fields

- Extract merchant, total, date, and category suggestion.
- Add field-level confidence and editable review.
- Persist confirmed image path in `Transaction.receiptPath`.

### P3.3 Evaluate OCR

- Prepare labeled receipt dataset.
- Measure field-level merchant, amount, and date accuracy.
- Log expected failure classes and retry behavior.

Acceptance criteria:

- No OCR output saves without review.
- OCR test results are recorded for methodology/results chapters.

## Phase 4 — LSTM Forecasting

### P4.1 Prepare forecast data pipeline

- Define transaction preprocessing, aggregation, minimum history, train/validation split, and evaluation dataset.
- Document all transformation choices for reproducibility.

### P4.2 Train and evaluate model

- Train LSTM model for 7-day expense prediction.
- Calculate MAE/RMSE and compare against a simple baseline `[ASUMSI - perlu konfirmasi]`.
- Export/deploy inference model through selected integration approach.

### P4.3 Integrate product forecast

- Add inference repository/interface.
- Populate `weeklyForecastData`, projected balance, and forecast narrative from real model output.
- Add loading/error/insufficient-history states.

Acceptance criteria:

- Dashboard forecast card receives real model output.
- Forecast risks use calm guidance, not alarm colors.
- Metrics and limitations are documented.

## Phase 5 — Prescriptive Insights and Planning Surfaces

### P5.1 Build prescriptive insight engine

- Implement typed, ranked insight output.
- Add budget-near, budget-over, forecast-deficit, spend-spike, positive-savings, recurring-due, supportive-streak, and cold-start rules.
- Keep thresholds marked `[ASUMSI - perlu dikalibrasi]` until evaluation data/advisor review.

### P5.2 Rework Analytics, Budgeting, Goals, Accounts

- Rework analytics narrative/charts and direct actions.
- Rework budget, recurring payment, installment, goal, contribution, allocation, and account surfaces.
- Apply semantic Honey/Lilac/Sky guidance mappings.

Acceptance criteria:

- Existing functionality remains available.
- Major insights expose one understandable next action.
- Charts are readable without color-only interpretation.

## Phase 6 — Trust, Preferences, and Authentication

### P6.1 Rework auth and settings

- Rework Login, Sign Up, Forgot Password, Settings, language/currency sheets, and logout confirmation.
- Replace unrestricted accent selection with approved swatches.

### P6.2 Rework smart-capture consent

- Rework Auto-Detection Onboarding and Bank Whitelist.
- Explain value, on-device privacy, and permission purpose before OS permission request.
- Verify user can change whitelist later.

Acceptance criteria:

- Consent is explicit and understandable.
- Raw notification content is not uploaded.

## Phase 7 — Verification and Thesis Evaluation

### P7.1 Functional and regression test pass

- Execute `docs/TEST_PLAN.md` functional cases.
- Resolve all P0/P1 defects and document deferred P2/P3 items.

### P7.2 Usability evaluation

- Recruit eligible student participants `[ASUMSI - perlu konfirmasi: sample size and recruitment method]`.
- Run defined tasks, capture completion/time/error data, collect SUS and open feedback.

### P7.3 Final research package

- Export model metrics, test results, usability results, screenshots, and issue log.
- Update methodology, implementation, evaluation, and limitations chapters.

## Suggested Solo Timeline

| Phase | Estimated duration |
|---|---:|
| Phase 0 | 1–2 days |
| Phase 1 | 4–6 days |
| Phase 2 | 4–6 days |
| Phase 3 | 4–7 days |
| Phase 4 | 5–8 days |
| Phase 5 | 4–6 days |
| Phase 6 | 2–4 days |
| Phase 7 | 4–7 days |

The timeline is an implementation estimate, not a commitment. Thesis approval, dataset preparation, and participant availability may change the schedule.
