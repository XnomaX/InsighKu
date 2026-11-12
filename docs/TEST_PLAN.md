# Test Plan

## Purpose

This plan defines functional, technical, and usability testing for InsighKu as a solo-developer thesis project. Results should support the methodology and evaluation chapters.

## Scope

- Existing core finance flows
- Offline-first persistence and synchronization
- Bank-notification drafts and consent
- Design-system rework and accessibility
- OCR receipt scanning after implementation
- LSTM forecasting after implementation
- Prescriptive insights after implementation

## Test Levels

| Level | Objective | Evidence |
|---|---|---|
| Unit test | Verify deterministic domain/use-case logic | Automated test result |
| Integration test | Verify repository, Room, worker, and flow boundaries | Test result / controlled run |
| Functional test | Verify user-visible workflows | Manual test case record |
| Usability test | Evaluate task completion, ease, and perceived usability | Task sheet, observation, SUS, feedback |
| ML evaluation | Measure forecasting/OCR performance | Dataset, metric calculation, analysis |

## Test Environment

- Android emulator and at least one physical Android device `[ASUMSI - perlu konfirmasi: device matrix]`.
- Test account isolated from participant data.
- Network-on and network-off conditions.
- Light mode, dark mode, hidden-amount mode, Indonesian language.
- Supported bank/e-wallet notification samples where permission/testing is lawful and available.

## Entry Criteria

- BUG-03 is resolved: Room destructive fallback has been removed and migrations tested.
- Build completes successfully.
- Test data/account can be reset without affecting production or participant data.
- OCR/LSTM cases are run only after their feature implementation is complete.

## Functional Test Cases

### Authentication and Session

| ID | Scenario | Expected result |
|---|---|---|
| AUTH-01 | Register with valid credentials | Account is created and user enters main app |
| AUTH-02 | Login with valid email/password | User enters main app |
| AUTH-03 | Login with invalid credentials | Clear, supportive error; no session created |
| AUTH-04 | Google sign-in | Authorized user enters main app |
| AUTH-05 | Request password reset | Reset email request is acknowledged clearly |
| AUTH-06 | Relaunch with active session | Splash routes to main app |

### Account and Transaction

| ID | Scenario | Expected result |
|---|---|---|
| TX-01 | Add account with initial balance | Account appears with correct balance |
| TX-02 | Add expense manually | Transaction, account balance, dashboard, and analytics update |
| TX-03 | Add income manually | Transaction and balance update correctly |
| TX-04 | Edit transaction | Updated values propagate to dependent views |
| TX-05 | Delete transaction and confirm | Transaction is removed and balances recalculate |
| TX-06 | Cancel delete confirmation | Transaction remains unchanged |
| TX-07 | Filter/sort transaction list | List reflects selected filter/sort without data loss |
| TX-08 | Create transaction offline | Record persists locally and remains visible |
| TX-09 | Restore network after offline save | Unsynced record is eventually synchronized |

### Budget, Goals, and Allocation

| ID | Scenario | Expected result |
|---|---|---|
| PLAN-01 | Create/edit budget | Budget displays correct limit and progress |
| PLAN-02 | Log expense within budget | Progress updates with comfortable/notice state |
| PLAN-03 | Reach/exceed budget | Guidance is calm; no destructive-red default styling |
| PLAN-04 | Create goal | Goal appears with target and progress |
| PLAN-05 | Contribute/withdraw from goal | Amount, balance, and history update correctly |
| PLAN-06 | Create recurring payment/installment | Due item is visible and scheduled correctly |
| PLAN-07 | Auto-allocation rule fires | Allocation executes or creates a review draft according to rule mode |
| PLAN-08 | Allocation fails/retries | User receives non-alarming status and no duplicate allocation occurs |

### Notification Drafts

| ID | Scenario | Expected result |
|---|---|---|
| DRAFT-01 | Complete onboarding and grant permission | Permission and whitelist state are stored clearly |
| DRAFT-02 | Receive supported notification from whitelisted app | One local draft is created with confidence state |
| DRAFT-03 | Receive duplicate notification | Duplicate draft is prevented |
| DRAFT-04 | Edit and approve draft | Confirmed transaction is created and draft removed |
| DRAFT-05 | Skip/dismiss draft | No transaction is created |
| DRAFT-06 | Check Firestore/network traffic | Raw notification content and drafts are not uploaded |

### OCR — run after implementation

| ID | Scenario | Expected result |
|---|---|---|
| OCR-01 | Select image/camera source | Permission and source state are handled safely |
| OCR-02 | Process readable receipt | Merchant, amount, date, category suggestion appear for review |
| OCR-03 | Edit extracted field | Edited value is saved, not original extraction |
| OCR-04 | Process unreadable receipt | Clear failure/retry state; no incorrect transaction is saved |
| OCR-05 | Confirm reviewed result | Transaction is created with receipt path |

### Forecast and Insights — run after implementation

| ID | Scenario | Expected result |
|---|---|---|
| FC-01 | Insufficient history | User sees clear setup state; no fabricated forecast |
| FC-02 | Valid history | Dashboard shows 7-day model output and projected balance |
| FC-03 | Inference failure | Error/retry state is clear and non-alarming |
| FC-04 | Projected deficit | User sees calm risk guidance and a relevant action |
| INS-01 | Near-budget rule | Supportive message and planning CTA appear |
| INS-02 | Spend-spike rule | Explanation identifies delta/category without blame |
| INS-03 | Cold-start rule | Forecast/comparison alerts are suppressed |
| INS-04 | Act on insight | Insight is suppressed for the rest of day when required |

### Design and Accessibility

| ID | Scenario | Expected result |
|---|---|---|
| UI-01 | Review each production screen in light/dark | Text, surfaces, controls, charts remain legible |
| UI-02 | Change accent | Only Apricot, Honey, Sage, Sky, Lilac are selectable |
| UI-03 | Budget/forecast warning | No default destructive-red treatment |
| UI-04 | Hide amounts | Sensitive values are concealed across core views |
| UI-05 | Increase system font scale | Core text does not clip or overlap `[ASUMSI - perlu konfirmasi: supported scale]` |
| UI-06 | Screen-reader inspection | Icon-only controls have meaningful labels |
| UI-07 | Touch inspection | Core interactive targets are at least 48dp |
| UI-08 | Reduced motion | Nonessential animation is reduced or disabled |

## ML Evaluation

### LSTM

1. Prepare a reproducible dataset from confirmed transactions.
2. Document data cleaning, feature engineering, lookback window, split strategy, and training configuration.
3. Evaluate MAE and RMSE on held-out data.
4. Compare against a simple baseline `[ASUMSI - perlu konfirmasi]`.
5. Report failure cases and limitations, including sparse histories and irregular spending.

Target thresholds are `[ASUMSI - perlu dikalibrasi]`.

### OCR

1. Prepare labeled receipt images.
2. Measure field-level precision/accuracy for merchant, total amount, and date.
3. Record unreadable, partial, skewed, and unsupported receipt outcomes.
4. Confirm review/edit prevents automatic acceptance of uncertain output.

Target thresholds are `[ASUMSI - perlu dikalibrasi]`.

## Usability Test Plan

### Participants

Target participants are university students who recently began managing personal finances independently.

- Sample size: `[ASUMSI - perlu konfirmasi]`.
- Recruitment and consent procedure: `[ASUMSI - perlu konfirmasi]`.
- Do not collect real bank-notification content, credentials, or sensitive financial data from participants.

### Task Script

1. Create an account or use a provided test account.
2. Add a cash/e-wallet account with a starting balance.
3. Log one expense manually.
4. Review and approve a prepared draft transaction.
5. Create a food budget.
6. Find the current budget status and explain the guidance.
7. Review a forecast state after LSTM is implemented.
8. Scan and review a prepared receipt after OCR is implemented.
9. Change an accent or privacy setting.

### Measures

| Measure | Collection method |
|---|---|
| Task completion | Complete / partial / failed |
| Time on task | Start/end timestamps |
| Errors and assistance | Moderator observation sheet |
| Perceived usability | SUS questionnaire |
| Perceived tone and clarity | Open-ended post-task questions |
| Design feedback | Short interview notes |

Suggested post-test questions:

- Bagian mana yang paling mudah digunakan?
- Adakah pesan budget atau forecast yang terasa menghakimi atau membingungkan?
- Apakah kamu memahami alasan aplikasi meminta izin notifikasi?
- Perubahan apa yang paling membantu saat mencatat transaksi?

SUS target is `[ASUMSI - perlu dikalibrasi]`.

## Exit Criteria

- All P0 and P1 known issues are resolved or formally justified before final evaluation.
- Core functional cases pass.
- Room migration is verified.
- OCR/LSTM evaluation results are recorded if those features are included in the final thesis scope.
- Usability evidence, consent records, anonymized task results, SUS results, and limitations are ready for the thesis report.
