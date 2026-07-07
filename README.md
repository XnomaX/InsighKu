# InsightKu

<p align="center">
  <img src="app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" width="100" />
</p>

<p align="center">
  <b>Smart Financial Tracking with AI</b><br>
  <sub>Track expenses, manage budgets, detect bank notifications automatically, and gain AI-powered insights — all in one app.</sub>
</p>

<p align="center">
  <a href="https://developer.android.com/about/versions/nougat" target="_blank">
    <img alt="Min SDK" src="https://img.shields.io/badge/Min%20SDK-24-brightgreen"/>
  </a>
  <a href="https://developer.android.com/about/versions/14" target="_blank">
    <img alt="Target SDK" src="https://img.shields.io/badge/Target%20SDK-36-blue"/>
  </a>
  <a href="https://kotlinlang.org" target="_blank">
    <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin"/>
  </a>
  <a href="https://developer.android.com/jetpack/compose" target="_blank">
    <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.05-4285F4?logo=jetpackcompose"/>
  </a>
</p>

---

## Features

### Authentication & Security
- **Email/Password** sign up and sign in
- **Google Sign-In** via Credential Manager
- **Forgot Password** via Firebase email reset
- **Biometric Authentication** (fingerprint / face recognition)
- Firebase Auth token validation on app launch

### Dashboard
- **Balance Overview** — total balance across all accounts, hide/show amounts
- **Monthly Summary** — income, expenses, and savings at a glance
- **Recent Transactions** — latest 5 transactions with category icons
- **AI Forecast** — predictive spending insights based on historical data
- **Streak Calendar** — daily tracking streak with freeze/repair mechanics
- **Upcoming Payments** — recurring and installment payment reminders with due date badges
- **Goal Progress** — preview of active savings goals

### Analytics
- **Monthly Financial Insights** — savings rate, income vs expenses
- **Expense Categories** — spending breakdown by category with charts
- **Income Sources** — income breakdown by source
- **Savings Rate Tracking** — progress toward 20% savings target with visual feedback

### Budgeting
- **Category Budgets** — set spending limits per category with alerts at configurable thresholds
- **Budget Overview** — overall progress, spent, and remaining amounts
- **Budget Alerts** — notifications when approaching or exceeding limits

### Recurring Payments & Installments
- **Recurring Payments** — weekly, biweekly, monthly, quarterly, yearly subscriptions and bills
- **Installments** — track multi-month installment plans with auto-calculation of remaining months
- **Custom Icons** — personalized icon selection for each payment
- **Mark Paid** — one-tap payment tracking with automatic next-due advancement

### Accounts
- **Multiple Accounts** — manage cash, bank accounts, e-wallets, and credit cards
- **Balance Tracking** — real-time balance updates
- **Default Account** — set a primary account for transactions

### Savings Goals
- **Goal Creation** — set target amounts with deadlines
- **Progress Tracking** — visual progress bars and completion status
- **Auto-Allocation** — automatic savings contributions based on rules
- **Account Allocation** — distribute goals across multiple accounts
- **Goal Details** — contribution history, allocation rules, and daily targets

### Bank Notification Detection
- **Auto-Detect Transactions** — parse bank/e-wallet notifications (BCA, BRI, BNI, Mandiri, SeaBank, Jenius, and more)
- **Draft Transactions** — detected transactions saved as drafts for review
- **Deep Link Integration** — tap notification to pre-fill the Add Transaction form
- **Notification Listener Service** — runs in background with keepalive service
- **Bank Whitelist** — user-configurable allowed notification sources

### Smart Features
- **Category Learning** — remembers your category preferences for auto-categorization
- **Smart Receipt Capture** — OCR-based receipt scanning
- **Draft Reminders** — daily reminders for unreviewed draft transactions
- **Offline-First Sync** — works offline, syncs to Firestore when connected
- **Scheduled Allocation** — automatic goal contributions on a schedule

### Settings & Customization
- **Dark Mode** — light and dark theme support
- **Currency Selection** — multi-currency support (default: IDR)
- **Language** — English and Bahasa Indonesia
- **Accent Color** — customizable accent color palette
- **Visual Density** — adjustable UI density
- **Insight Tone** — choose the tone of AI-generated insights
- **Comfort Mode** — relaxed UI mode
- **Hide Amounts** — privacy mode to hide financial figures
- **WhatsApp Integration** — share transactions via WhatsApp
- **Push Notifications** — configurable notification preferences

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.1.0 |
| **UI** | Jetpack Compose (BOM 2025.05) with Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **DI** | Hilt 2.55 |
| **Database** | Room (local) + Firestore (cloud) |
| **Auth** | Firebase Auth (Email + Google) |
| **Background** | WorkManager 2.10 |
| **Navigation** | Compose Navigation |
| **Animation** | Lottie |
| **Testing** | JUnit + Kotest (property-based) |
| **Build** | Gradle 8.13 + KSP |

---

## Architecture

```
app/
├── core/
│   ├── data/          # Room entities, DAOs, repositories, Firestore sync
│   ├── di/            # Hilt modules (AppModule, DatabaseModule)
│   ├── datastore/     # SessionManager, UserPreferences
│   ├── navigation/    # Route definitions, nav graphs
│   ├── notification/  # Bank notification parsing, listener service
│   ├── ui/            # Theme, colors, shared components
│   └── worker/        # WorkManager workers (sync, reminders, allocations)
└── feature/
    ├── auth/          # Login, Sign Up, Forgot Password
    ├── home/          # Dashboard, transactions, streak
    ├── analytics/     # Financial insights and charts
    ├── planning/      # Budgeting, goals, recurring payments
    ├── accounts/      # Account management
    └── settings/      # App preferences and configuration
```

---

## Supported Banks & E-Wallets

InsightKu can auto-detect transactions from the following notification sources:

| Bank / E-Wallet | Status |
|---|---|
| BCA | Supported |
| BRI | Supported |
| BNI | Supported |
| Mandiri | Supported |
| SeaBank | Supported |
| Jenius | Supported |
| Other banks | Configurable via whitelist |

---

## Getting Started

### Prerequisites
- Android Studio Ladybug or later
- JDK 11+
- Firebase project with Auth and Firestore enabled
- `google-services.json` in `app/` directory

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/InsighKu.git
   cd InsighKu
   ```

2. **Configure Firebase**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable **Authentication** (Email/Password + Google)
   - Enable **Cloud Firestore**
   - Download `google-services.json` and place it in `app/`

3. **Build and Run**
   ```bash
   ./gradlew :app:assembleDebug
   ```
   Or open the project in Android Studio and click Run.

---

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Firebase sync and API calls |
| `CAMERA` | Receipt scanning |
| `READ_MEDIA_IMAGES` | Gallery access for receipts |
| `POST_NOTIFICATIONS` | Budget alerts and reminders |
| `RECEIVE_BOOT_COMPLETED` | Restart services after reboot |
| `USE_BIOMETRIC` | Fingerprint/face authentication |
| `FOREGROUND_SERVICE` | Keep notification listener alive |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read bank notifications |

---

## Testing

```bash
# Run unit tests
./gradlew :app:testDebugUnitTest

# Run instrumented tests
./gradlew :app:connectedDebugAndroidTest
```

---

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ for better financial management
</p>
