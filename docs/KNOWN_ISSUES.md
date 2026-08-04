# Known Issues / Technical Debt Log

## Purpose

This is the active defect and technical-debt register for the solo InsighKu project. Update it after each milestone, focused manual QA pass, model evaluation, or usability session.

## Severity

| Severity | Meaning |
|---|---|
| P0 | Blocks research data collection, risks participant data, or creates a critical integrity/security failure |
| P1 | Breaks a primary feature or makes a thesis-scope feature misleading/unusable |
| P2 | Degrades a core experience, reliability, or expected user trust |
| P3 | Polish, copy, documentation, or lower-risk improvement |

## Active Items

| ID | Area | Severity | Status | Finding | Evidence | Required action | Gate |
|---|---|---|---|---|---|---|---|
| BUG-03 | Data safety | P0 | Open | Room destructive fallback can erase local participant data after schema change. | `DatabaseModule.kt`: `fallbackToDestructiveMigration(true)` | Add/test explicit migrations; remove fallback. | Must close before participant testing/data collection |
| BUG-01 | OCR | P1 | Open | Receipt scanner is UI-only; actions are mocked and result uses hardcoded merchant/amount/category/date. | `ReceiptScannerDialog.kt` | Implement source, image, OCR, editable review, retry, storage pipeline. | Must close before OCR evaluation/demo |
| BUG-02 | Forecasting | P1 | Open | Forecast card is wired but no ViewModel forecast data or LSTM inference exists. | `DashboardState.kt`, `DashboardViewModel.kt`, `DashboardForecast.kt` | Implement inference integration and state handling. | Must close before LSTM evaluation/demo |
| BUG-05 | Notification reliability | P2 | Open | OS may stop notification listener; drafts can be missed. | listener keepalive/rebind/restart code | Test supported devices; expose capture-health status. | Review before usability test |
| BUG-06 | Parser accuracy | P2 | Open | Per-service regex templates can drift and produce missed/low-confidence drafts. | `BankNotificationParser.kt` | Retain review/confidence; test samples; update parser rules. | Review before usability test |
| BUG-07 | Streak framing | P2 | Open | Displayed streak appears zero before today is logged even when historical streak exists. | `DashboardState.kt` | Use calm routine-oriented representation/copy. | Close during design rework |
| BUG-08 | Allocation edge case | P2 | Open | Failed auto-allocation may retry without visible user context. | `AddTransactionUseCase.kt` | Show non-alarming pending/retry status; preserve idempotency. | Review before usability test |
| BUG-04 | Dashboard polish | P3 | Open | Error state renders a literal `??` placeholder. | `DashboardScreen.kt` | Replace with approved empty/error component. | Close during design rework |
| BUG-09 | Research claim accuracy | P3 | Open | Category memory is historical matching, not ML. | `CategoryMemory.kt` | Use accurate thesis/UI terminology. | Close before thesis writing/final demo |
| BUG-10 | Permission onboarding | P3 | Open | User may deny notification permission without understanding impact. | auto-detection onboarding and whitelist flows | Explain benefit/privacy before permission request. | Close during design rework |

## Update Protocol

For each newly discovered item:

1. Assign an ID, area, severity, status, evidence, and required action.
2. Record the reproduction condition or test case ID when applicable.
3. Link the fix to a commit or milestone after resolution.
4. Mark status as `Resolved`, `Deferred`, or `Won't Fix` only with a short rationale.
5. Reassess priority before usability testing and before final thesis submission.

## Pre-Evaluation Gate

Before participant testing or research data collection:

- BUG-03 must be resolved and migration tested.
- All P1 issues relevant to the evaluated feature set must be resolved.
- Test accounts/data must be separated from participant data.
- Notification and receipt test data must be synthetic or explicitly consented.
