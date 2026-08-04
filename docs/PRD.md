# InsighKu — Product Requirements Document

## TL;DR

InsighKu is an Android personal-finance application for Indonesian university freshmen who are managing money independently for the first time. The thesis project extends the existing offline-first transaction, budgeting, goal, analytics, and bank-notification draft system with LSTM-based 7-day expense forecasting, OCR receipt scanning, actionable insights, and a calm Headspace-inspired design-system rework.

This is an academic project. Its purpose is to demonstrate and evaluate a complete system, ML performance, and usability—not user growth, monetization, retention, or partnerships.

## Context

Students transitioning from SMA to university often have limited income, fluctuating needs, and limited practical financial-literacy experience. Existing finance apps primarily record transactions after they occur. InsighKu addresses this gap through low-friction logging, proactive forecasting, supportive planning guidance, and interpretable insights.

## Academic / Research Goals

- Deliver a functioning end-to-end Android personal-finance system for first-time student money managers.
- Implement and evaluate an LSTM model for 7-day expense forecasting and early projected-balance/deficit guidance.
- Implement and evaluate receipt OCR to reduce manual transaction-entry friction.
- Evaluate usability through task completion and SUS with representative student participants.
- Demonstrate that descriptive analytics can be extended into supportive, actionable recommendations.

## User Goals

- Log income and expenses quickly through manual entry, bank-notification drafts, and receipt scan when implemented.
- See current balances, spending, budgets, goals, and upcoming commitments in one place.
- Receive calm, specific guidance before a budget or projected balance becomes tight.
- Understand spending patterns without being blamed or overwhelmed.

## Non-Goals

- Investment, lending, portfolio management, or financial advice.
- Commercial growth, monetization, retention, acquisition, or partnership targets.
- Rebuilding the existing analytics engine; the scope is to extend it with prescriptive guidance.
- Treating category-history matching as ML; ML claims are limited to the LSTM and OCR work.

## Implementation Status — Code Review Baseline

The code review is based on the repository default branch `authflow`.

### Built

- Kotlin Android app using Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager, Firebase Auth, Firestore, Credential Manager, Vico charts, and Lottie.
- Offline-first transaction persistence and Firestore synchronization.
- Email/password and Google authentication.
- Account, transaction, category, budget, recurring payment, installment, and savings-goal workflows.
- Behavioral analytics using `InsightEngine` / `AnalyticsDomain`.
- Notification-based transaction drafts, with an on-device parser for supported Indonesian bank and e-wallet notifications.
- Goal auto-allocation, allocation drafts, streak tracking, reminders, and consent/whitelist flows.

### Shells / To Build

- OCR receipt scanning is UI-only. `ReceiptScannerDialog.kt` currently uses mock actions and hardcoded extraction values.
- Forecast UI exists, but forecast data and AI insight message are not populated. No LSTM model, TensorFlow Lite dependency, or inference layer was found.

### Needs Strengthening

- Analytics are descriptive and narrative-oriented but need budget-, forecast-, and action-linked recommendations.
- `fallbackToDestructiveMigration(true)` is a participant-data-loss risk and must be removed before evaluation.

## Functional Requirements

### Transaction Management — Existing / Maintain

- Create, edit, view, filter, sort, and delete transactions.
- Support income, expense, transfer, goal contribution/withdrawal, auto-allocation, and balance adjustment types.
- Associate each transaction with one account and optionally preserve receipt path, metadata, and source module.
- Keep manual entry as the fastest, primary daily workflow.

### Bank Notification Drafts — Existing / Maintain

- Listen only after explicit notification permission and bank/e-wallet whitelist consent.
- Parse supported bank/e-wallet notifications into device-local drafts with amount, merchant, transaction type, source, and confidence.
- Let users review, edit, approve, skip, or dismiss every draft before a financial transaction is created.
- Do not send raw notification content or drafts to Firestore.

### OCR Receipt Scanning — High Priority / To Build

- Capture a receipt or select an existing image.
- Extract merchant, total amount, date, and suggested category.
- Show a field-by-field review screen before saving.
- Let users edit every extracted field and retry failed scans.
- Attach the final image through the existing `Transaction.receiptPath` field.
- Clearly distinguish source selection, processing, review, confirmed, failure, and unsupported states.

### LSTM Forecasting — High Priority / To Build

- Produce a 7-day expense forecast from confirmed historical transactions.
- Show projected daily expense and projected balance.
- Distinguish insufficient history, loading, ready, error, and deficit-risk states.
- Explain that the result is a forecast, not a certainty.
- Never use alarm-red as the default deficit presentation.

### Analytics and Prescriptive Insights — High Priority / Extend

The existing analytics engine produces spending personality, mood, weekday/weekend behavior, comparisons, heatmaps, and spending patterns. Extend it with typed, ranked insights using confirmed transactions only.

- `INS-BUDGET-OVER`: Trigger when category spending reaches the configured category budget. Threshold: `[ASUMSI - perlu dikalibrasi]`. Show remaining period context and a gentle planning CTA.
- `INS-BUDGET-NEAR`: Trigger at 80–99% category budget usage with time left. Threshold: `[ASUMSI - perlu dikalibrasi]`. Offer a remaining daily allowance.
- `INS-FORECAST-DEFICIT`: Trigger when the LSTM projected balance falls below a user buffer or zero. Safety buffer: `[ASUMSI - perlu dikalibrasi]`. Link to upcoming payments or budget review.
- `INS-SPEND-SPIKE`: Trigger when weekly spend is 20% above the trailing 4-week average. Threshold: `[ASUMSI - perlu dikalibrasi]`. Highlight the leading category and link to its breakdown.
- `INS-POSITIVE-SAVINGS`: Trigger when monthly savings rate reaches 20%. Threshold: `[ASUMSI - perlu dikalibrasi]`. Offer an optional contribution toward a goal.
- `INS-RECURRING-DUE`: Trigger when a recurring payment or installment is due within 3 days and projected balance may not cover it. Window: `[ASUMSI - perlu dikalibrasi]`.
- `INS-STREAK-SUPPORTIVE`: Invite users to update their financial picture without suggesting failure or a lost streak.
- `INS-COLD-START`: Suppress comparison and forecast alerts until a minimum history length is available. Minimum history: `[ASUMSI - perlu dikalibrasi]`.

Display at most 2–3 ranked insights at once. Suppress an insight for the rest of the day once the user acts on it.

## User Experience

### First-Time Experience

1. Splash routes the user to authentication or the main app.
2. User signs in or creates an account.
3. User adds an account and initial balance.
4. The app introduces quick transaction logging, financial planning, optional notification detection, and its on-device privacy model.
5. Notification access and bank whitelist selection require clear consent before use.

### Core Daily Experience

1. User opens Dashboard and sees balance, a top-priority guidance card, recent activity, budget/goal status, and a prominent quick-add action.
2. User logs a transaction manually or opens a reviewed bank-notification draft.
3. User selects amount, type, account, and category first; optional details are progressively disclosed.
4. The system saves locally first and synchronizes when network is available.
5. Analytics and dashboards update from confirmed transactions.
6. Forecast and OCR states are available only after those features are functionally implemented.

### Design and Accessibility Requirements

- Minimum interactive target: 48dp.
- Support light mode, dark mode, Indonesian copy, hidden amounts, and reduced motion.
- Charts, budget status, and feedback include labels or patterns beyond color.
- Use supportive observation + reassurance + next-action copy.

## Design System Rework

### Direction

The rework takes direction from the supplied Headspace UI Kit reference without copying it. It prioritizes calm color, generous whitespace, friendly typography, warm rounded surfaces, human iconography/illustration, and supportive microcopy.

Reference: https://www.figma.com/design/YxujgzQPbHRgTexnoixXZj/Headspace-Design-UI-Kit--Community---Community-?node-id=58-4

All production screens are in scope. The unrestricted accent picker is replaced with curated warm accents only; red and coral cannot be chosen as a primary accent.

### Key Design System Issues

| ID | Severity | Finding | Rework Decision |
|---|---|---|---|
| DS-01 | Major | Color logic is distributed across theme, palette, and feature files. | Consolidate semantic colors into one source of truth. |
| DS-02 | Major | Purple/high-saturation visual language conflicts with requested calm warmth. | Use curated warm accent tokens. |
| DS-03 | Major | Red is used for common financial feedback. | Reserve destructive Brick for final destructive/system failure states only. |
| DS-04 | Major | Current accent selection can make red/coral primary. | Restrict selection to five approved warm accents. |
| DS-05 | Major | Brand, status, category, and destructive colors are mixed. | Separate surface, action, semantic feedback, and chart/category roles. |
| DS-06 | Major | Typography uses `FontFamily.Default` throughout. | Introduce friendly hierarchy with system fallback. |
| DS-07 | Major | Local fixed hex colors, shapes, spacing, shadows are used in screens. | Require semantic tokens and reusable components. |
| DS-08 | Major | Material icons, emoji, flame, and local motifs create a mixed visual voice. | Define consistent rounded icon and warm illustration language. |
| DS-09 | Minor | Spacing and radius values are inconsistent. | Normalize to defined scales. |
| DS-10 | Major | Budget/forecast feedback uses warning-like red/orange cues. | Use Honey, Sky, and Lilac guidance states. |
| DS-11 | Minor | Main navigation/FAB motion and shadow are prominent. | Reduce elevation and urgency-like motion. |
| DS-12 | Minor | Dark semantic coverage is incomplete. | Define paired light/dark semantic tokens. |

### Core Tokens

#### Color

| Role | Light | Dark |
|---|---:|---:|
| Canvas | `#FFF9F2` Cloud | `#211D1A` Night Cocoa |
| Surface | `#FFFFFF` Milk | `#2E2824` Roast |
| Subtle surface | `#F7F0E7` Oat | `#39312C` Mocha |
| Border | `#E8DDD0` Sand | `#4B413B` |
| Primary text | `#2D2926` Ink | `#FFF7F0` |
| Secondary text | `#6E625B` Cocoa | `#D5C8BF` |

Curated primary accents: Apricot `#F59E72` default, Honey `#E7B84B`, Sage `#7FAE92`, Sky `#79AFCB`, and Lilac `#A898CF`.

Semantic feedback: Positive Sage `#6FA986`; Notice Honey `#C9972D`; Reflect Sky `#5E9DBB`; Neutral Guide Lilac `#9380BC`; Destructive Brick `#B85C52`.

#### Typography

- Balance: 36sp / 44sp medium.
- Display: 32sp / 40sp medium.
- Headline: 24sp / 32sp semibold.
- Section title: 20sp / 28sp semibold.
- Title: 17sp / 24sp semibold.
- Body: 16sp / 24sp regular.
- Supporting: 14sp / 20sp regular.
- Label: 13sp / 18sp medium.
- Custom rounded display font: `[ASUMSI - perlu konfirmasi]`; use Android system sans-serif until approved.

#### Layout, Shape, and Motion

- Base spacing 4dp: 4, 8, 12, 16, 24, 32, 40dp.
- Horizontal screen padding 20dp; default section gap 32dp; card gap 12dp.
- Input/compact control radius 16dp; card 24dp; hero card/sheet/dialog 28dp.
- Prefer border and tonal separation over shadows; standard elevation 0–2dp.
- Motion 160–240ms ease-out; respect reduced motion; avoid continuous pulsing and competitive reward motion.

#### Shared Components

- Primary button: 52dp, selected warm accent, 16dp radius.
- Secondary button: surface/Oat fill, Sand border.
- Destructive button: Brick only in a final irreversible confirmation.
- Card: semantic surface, 24dp radius, 20dp padding, 1dp border.
- Input: 56dp minimum height, persistent label, supporting text, accent focus ring.
- Badge/chip: pill, semantic soft fill, text label.
- Progress: 8dp rounded track plus visible value/percentage.
- Sheet/dialog: 28dp top corners, clear close and action areas.

## Screen-by-Screen Specification

| Screen / Surface | Current function | Required visual rework |
|---|---|---|
| Splash | Routes to auth or main app | Quiet Cloud canvas, warm brand mark, gentle transition. Branded illustration: `[ASUMSI - perlu konfirmasi]`. |
| Login / Sign Up | Account access and registration | Spacious single-column forms, warm intro area, clear labels, full-width primary action, calm secondary Google action. |
| Forgot Password | Sends reset email | One focused card and reassuring success explanation. |
| Dashboard | Balance, transactions, budgets, goals, drafts, payments, forecast, insights | Generous balance hero, one top insight, soft modular cards, calm charts, prominent quick add; replace flame/competitive language with routine cue. |
| Analytics | Weekly/monthly/annual behavioral insights | Narrative sequence, segmented pill period selector, direct chart labels, one recommended action per insight. Module disclosure: `[ASUMSI - perlu konfirmasi]`. |
| Budgeting | Budget categories, income, recurring payments, installments | Planning overview, clear category cards, soft progress feedback, one create action. |
| Goals | Goals list and creation | Warm progress cards, labeled progress/date context, one contribution CTA. |
| Goal Detail | Contributions, withdrawals, timeline, allocation, archive/delete | Progress as hero, advanced controls lower in hierarchy, isolated destructive actions. |
| Accounts | Cash, bank, e-wallet, credit card management | Calm available-balance summary, rounded account icons, grouped account types, plain-language allocation explanations. |
| Transaction Details | Transaction list/detail, filter, edit, delete | Warm category icons, scanable date grouping, progressive filters, final delete confirmation only. |
| Settings | Preferences, accent, smart capture, account | Group into Personalization, Money Preferences, Smart Capture & Privacy, Account; five labeled accent swatches only. |
| Auto-Detection Onboarding | Notification-capture education and consent | Three steps: value, on-device privacy, permission. Separate consent flow detail: `[ASUMSI - perlu konfirmasi]`. |
| Bank Whitelist | Select supported services | Searchable grouped app list, clear selected state, editable later. |
| Add Transaction | Manual transaction and draft prefill | Calm bottom sheet: amount first, type/account/category, optional details progressively disclosed, persistent save CTA. |
| Receipt Scanner | OCR UI shell | Explicit source, capture, processing, review, confirm/edit, failure/retry, unsupported states. No claim of functionality before OCR implementation. |
| Edit Transaction | Update saved transaction | Reuse Add Transaction language; save changes as primary; delete separated. |
| Account forms | Add/edit account | Account type icon, initial balance explanation, clear name; deletion only from edit confirmation. |
| Budget forms | Add/edit category budget | Category, amount, period, live calm planning preview. Weekly guidance in form: `[ASUMSI - perlu konfirmasi]`. |
| Recurring payment / installment forms | Schedule and manage commitments | Plain-language date/frequency preview and calm timeline. |
| Goal forms | Create/edit goal | Target, date, account, optional allocation in progressive flow. Illustration selector: `[ASUMSI - perlu konfirmasi]`. |
| Contribution / withdrawal | Move money to/from goal | Explicit source/destination and balance, clear amount, neutral withdrawal explanation. |
| Auto-allocation | Configure allocation rules | Readable trigger, amount/percentage, destination, approval mode, plain-language preview. |
| Allocation Draft Review | Approve/edit/skip allocation suggestion | Emphasize user control, amount, goal, context, and non-mandatory choice. |
| Delete/archive/logout confirmations | Confirm sensitive action | Restrained Brick only for final confirmation; cancel/back receives default focus. |
| Language/currency sheets | Choose language or currency | 48dp list rows, selected checkmark/text, active-preview, clear done action. |

Notification Debug is a debug-only engineering screen and excluded from visual-rework scope.

## Known Issues / Technical Debt

All items are Open. This is the working defect list for the solo project and must be expanded through manual QA.

| ID | Area | Severity | Status | Finding | Evidence | Required action |
|---|---|---|---|---|---|---|
| BUG-03 | Data safety | P0 — Highest Priority | Open | Must be fixed before user testing or research data collection begins. Destructive Room migration can erase participant data. | `DatabaseModule.kt` uses `fallbackToDestructiveMigration(true)`. | Add and test explicit Room migrations; remove destructive fallback. |
| BUG-01 | OCR | P1 | Open | Receipt scanning is non-functional; extraction is mocked. | `ReceiptScannerDialog.kt`. | Build capture, image, OCR, review, and error pipeline. |
| BUG-02 | Forecast | P1 | Open | AI forecast card has no populated data or AI insight. | `DashboardState.kt`, `DashboardViewModel.kt`, `DashboardForecast.kt`. | Integrate LSTM output and distinct state handling. |
| BUG-04 | Dashboard polish | P3 | Open | Error state renders literal `??` placeholder. | `DashboardScreen.kt`. | Replace with on-brand error visual. |
| BUG-05 | Notification reliability | P2 | Open | Notification listener can be killed by the OS and drafts can be missed. | Notification listener keepalive/rebind code. | Verify OEM behavior and show detection health status. |
| BUG-06 | Parser accuracy | P2 | Open | Per-bank regex formats can change or fail. | `BankNotificationParser.kt`. | Keep confidence/review; test supported formats. |
| BUG-07 | Streak framing | P2 | Open | Displayed streak can appear as zero until today is logged. | `DashboardState.kt`. | Reframe with supportive routine language. |
| BUG-08 | Allocation edge cases | P2 | Open | Failed auto-allocation may be skipped without visible notice. | `AddTransactionUseCase.kt`. | Surface non-alarming retry/failure state. |
| BUG-09 | Research claim accuracy | P3 | Open | Category memory is historical matching, not ML. | `CategoryMemory.kt`. | Describe accurately in UI and thesis. |
| BUG-10 | Permission onboarding | P3 | Open | Users may deny notification permission without understanding impact. | Auto-detection onboarding/whitelist flows. | Explain benefit and on-device privacy before permission. |

## Technical Considerations

### Data and Privacy

- Room is the local source for offline-first operation.
- Confirmed transactions may sync to Firestore.
- Notification drafts and raw notification content remain device-local and are never sent to Firestore.
- Notification detection requires explicit OS permission and service whitelisting.
- OCR images and extracted data require clear storage, retention, and permission handling before user testing.

### Model Evaluation

- LSTM: evaluate MAE/RMSE against held-out transaction data. Target thresholds: `[ASUMSI - perlu dikalibrasi]`.
- OCR: evaluate merchant, total, and date extraction accuracy against labeled receipts. Target thresholds: `[ASUMSI - perlu dikalibrasi]`.
- UX: evaluate task completion, error rate, time on task, and SUS with student participants. Participant count and SUS target: `[ASUMSI - perlu konfirmasi]`.

## Milestones and Sequencing

### Team

One developer/researcher: product, design, Android implementation, ML integration, QA, and thesis evaluation.

### Phase 1 — Data Safety and Foundation

- Fix BUG-03 before any participant testing or data collection.
- Inventory and migrate hardcoded visual tokens.
- Build semantic light/dark tokens and curated warm accent palette.

### Phase 2 — OCR Integration

- Implement capture/gallery flow, OCR extraction, editable review, error/retry, and receipt attachment.
- Evaluate OCR with labeled receipt data.

### Phase 3 — LSTM Integration

- Build inference path and 7-day forecast presentation.
- Add projected balance and calm deficit-risk guidance.
- Evaluate forecast accuracy.

### Phase 4 — Actionable Analytics and Core UI

- Implement prescriptive insight engine.
- Rework Dashboard, Add/Edit Transaction, Transaction Details, bottom navigation, and receipt scanner states.

### Phase 5 — Planning, Trust, and Remaining Screens

- Rework Analytics, Budgeting, Goals, Accounts, auth, Settings, notification onboarding, and bank whitelist.

### Phase 6 — Verification and Research Evaluation

- Complete manual QA against Known Issues / Technical Debt.
- Verify contrast, 48dp targets, Indonesian copy, dark mode, hidden amounts, charts, and reduced motion.
- Conduct model and usability evaluation.

## Success Metrics

### Model and Technical

- LSTM MAE/RMSE on held-out data: `[ASUMSI - perlu dikalibrasi]`.
- OCR field extraction accuracy for merchant, amount, and date: `[ASUMSI - perlu dikalibrasi]`.
- Core flows complete without critical error during evaluation.
- No destructive data migration risk before participant testing.

### Usability

- Participant task-completion rate for logging, reviewing a draft, creating a budget, and interpreting a forecast: `[ASUMSI - perlu dikalibrasi]`.
- SUS score: `[ASUMSI - perlu dikalibrasi]`.
- Qualitative evidence that students find budget/forecast messaging supportive and understandable.

## Definition of Done

- BUG-03 is resolved before research data collection.
- OCR and LSTM are functional, evaluated, and clearly represented in UI states.
- Analytics include supportive action-linked insights.
- All production screens use the semantic design-system layer.
- Only curated warm accents are selectable.
- Normal finance feedback does not default to red.
- Core UI works in light/dark, Indonesian, and hidden-amount modes.
- Accessibility and manual QA requirements are met.
