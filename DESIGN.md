# InsighKu Design System

## Purpose

This document defines the visual and interaction system for InsighKu, an Android personal-finance application for Indonesian university students managing money independently for the first time.

The system is inspired by the calm, human, rounded direction in the referenced Headspace UI Kit:

- Reference: https://www.figma.com/design/YxujgzQPbHRgTexnoixXZj/Headspace-Design-UI-Kit--Community---Community-?node-id=58-4
- This is a direction reference, not a visual copy.

The outcome must make financial management feel clear, personal, and supportive. It must reduce friction in logging, monitoring, planning, and reviewing money without using fear, shame, or alarm-heavy feedback.

## Design Principles

1. Calm over alarm
   - Budget and forecast states guide the user with soft, readable feedback.
   - Red is never the default treatment for overspending, reached budgets, or deficit risk.

2. Actionable over judgmental
   - Every insight explains what happened and offers one practical next step.
   - Copy avoids blame, urgency, and competitive pressure.

3. Friendly over corporate
   - Use generous whitespace, rounded surfaces, approachable typography, and warm iconography.
   - Use human-centered illustration for onboarding, empty states, forecasting, and insights when assets are approved.

4. Frictionless daily use
   - Logging a transaction must remain the dominant primary action.
   - Forms reveal optional fields progressively.
   - Important actions have a minimum 48dp touch target.

5. Accessible by default
   - Color is never the only way to communicate a status.
   - Charts must provide labels, values, or patterns.
   - Text and controls must meet contrast and readability requirements in light and dark themes.

## Source of Truth

All new or reworked UI must consume semantic tokens and shared components. Screen code must not introduce local hardcoded colors, shape values, spacing, elevation, or button styles unless the token layer is first extended.

Target implementation areas:

- `core/ui/theme/AppPalette.kt`
- `core/ui/theme/Color.kt`
- `core/ui/theme/Theme.kt`
- `core/ui/theme/Type.kt`
- `core/ui/theme/Dimens.kt`
- `core/ui/theme/Shape.kt`
- `core/ui/theme/PersonalizationProvider.kt`
- `core/ui/components/`

Existing `AnalyticsPalette`, `SettingsPalette`, and transaction tokens may remain temporary compatibility aliases while screens migrate to the unified semantic layer.

## Color System

### Light surfaces

| Token | Value | Usage |
|---|---:|---|
| `canvas` | `#FFF9F2` | Primary app background |
| `surface` | `#FFFFFF` | Cards, sheets, dialogs, inputs |
| `surfaceSubtle` | `#F7F0E7` | Nested panels, unselected controls, subtle fills |
| `border` | `#E8DDD0` | Hairline borders, dividers |
| `textPrimary` | `#2D2926` | Headings, primary values, primary body text |
| `textSecondary` | `#6E625B` | Supporting text, metadata |
| `textDisabled` | `#A99F97` | Disabled text and placeholder states |

### Dark surfaces

| Token | Value | Usage |
|---|---:|---|
| `canvas` | `#211D1A` | Primary app background |
| `surface` | `#2E2824` | Cards, sheets, dialogs, inputs |
| `surfaceSubtle` | `#39312C` | Nested panels, unselected controls |
| `border` | `#4B413B` | Hairline borders, dividers |
| `textPrimary` | `#FFF7F0` | Headings and primary text |
| `textSecondary` | `#D5C8BF` | Supporting text and metadata |

### Curated warm accents

Users may choose only one of these approved accents. Apricot is the default.

| Accent | Value | Token |
|---|---:|---|
| Apricot | `#F59E72` | `accentApricot` |
| Honey | `#E7B84B` | `accentHoney` |
| Sage | `#7FAE92` | `accentSage` |
| Sky | `#79AFCB` | `accentSky` |
| Lilac | `#A898CF` | `accentLilac` |

Red and coral must not be offered as selectable primary accents.

### Semantic feedback

| Token | Foreground | Soft background | Purpose |
|---|---:|---:|---|
| `positive` | `#6FA986` | `#E6F2E9` | Progress, healthy balance, completed action |
| `notice` | `#C9972D` | `#FBF2D8` | Attention, nearing a limit, reminder |
| `reflect` | `#5E9DBB` | `#E5F2F7` | Information, planning, forecast context |
| `neutralGuide` | `#9380BC` | `#EEEAF7` | Reflection, insight, reached budget guidance |
| `destructive` | `#B85C52` | `#F8E8E5` | Final delete, account/system failure, unrecoverable error |

### Financial feedback mapping

| State | Primary treatment | Required content |
|---|---|---|
| Budget comfortable | `positive` | Remaining amount or percentage |
| Budget nearing limit | `notice` | Remaining days and a supportive planning cue |
| Budget reached or exceeded | `notice` + `neutralGuide` | Clear observation and a calm next action |
| Forecast deficit risk | `reflect` + `neutralGuide` | Time window, projected impact, next action |
| Delete or irreversible action | `destructive` | Explicit consequence and confirmation |

No normal spending, budget, or forecast state may default to destructive red.

### Category visualization

Category charts must use muted values derived from Apricot, Honey, Sage, Sky, Lilac, and Cocoa-neutral tones. Adjacent chart values must differ through luminance, direct labels, patterns, or ordering; color alone is insufficient.

## Typography

### Font strategy

Use a friendly rounded display face only after licensing and Android packaging are approved. Until then, use the Android system sans-serif family. Body text must use a high-legibility system sans-serif.

Custom display font selection is `[ASUMSI - perlu konfirmasi]`.

### Type scale

| Role | Size / line height | Weight | Usage |
|---|---:|---|---|
| Balance display | 36sp / 44sp | Medium | Total balance and major financial value |
| Display | 32sp / 40sp | Medium | High-impact onboarding or empty states |
| Headline | 24sp / 32sp | Semibold | Screen heading |
| Section title | 20sp / 28sp | Semibold | Major content section |
| Title | 17sp / 24sp | Semibold | Card title, row title |
| Body | 16sp / 24sp | Regular | Primary explanatory copy |
| Supporting | 14sp / 20sp | Regular | Secondary content and metadata |
| Label | 13sp / 18sp | Medium | Button, chip, small control label |

Rules:

- Use sentence case.
- Do not use all caps for finance status, error, or primary actions.
- Use Indonesian-first strings in production.
- Use tabular figures for balances and financial values where the chosen font supports them.
- Keep one clear hierarchy per viewport; avoid multiple display-scale elements competing for attention.

## Spacing and Layout

Base unit: `4dp`.

| Token | Value |
|---|---:|
| `space1` | 4dp |
| `space2` | 8dp |
| `space3` | 12dp |
| `space4` | 16dp |
| `space5` | 24dp |
| `space6` | 32dp |
| `space7` | 40dp |

Layout rules:

- Screen horizontal padding: `20dp`.
- Default section gap: `32dp`.
- Card gap: `12dp`.
- Standard card inner padding: `20dp`.
- Compact component padding: `16dp`.
- Minimum touch target: `48dp`.
- Prefer one primary action per viewport.
- Use whitespace to separate decisions; do not use excessive dividers or shadows as the primary grouping method.

## Shape, Elevation, and Motion

### Shapes

| Component | Radius |
|---|---:|
| Compact control and input | 16dp |
| Card | 24dp |
| Hero card and dialog | 28dp |
| Bottom sheet top corners | 28dp |
| Status badge, filter chip | Pill |

### Elevation

- Standard card: `0–2dp`; prefer border and tonal separation.
- Bottom navigation and floating action: up to `4dp` only when separation is needed.
- Do not use heavy shadows as a default decorative effect.

### Motion

- Standard feedback duration: `160–240ms`, ease-out.
- Respect system reduced-motion settings.
- Avoid continuous pulsing, urgency animations, score-like reward emphasis, and motion that frames ordinary money behavior as a crisis.

## Components

### Buttons

| Component | Visual rule | Usage |
|---|---|---|
| Primary button | 52dp height, selected warm accent fill, 16dp radius, one clear action | Save transaction, continue, confirm normal action |
| Secondary button | Surface or Oat fill, Sand border, Ink text | Alternative action |
| Text button | No container, accent text, 48dp target | Low-emphasis action |
| Destructive button | Brick outline or fill only in final confirmation | Delete or irreversible action |

### App card

- Use `surface` or dark equivalent.
- Radius: `24dp`.
- Padding: `20dp`.
- Border: `1dp` semantic border.
- No heavy default shadow.
- A card must present one clear group of related information or decision.

### Insight card

Required structure:

1. Warm icon or approved illustration container.
2. Plain-language title.
3. One supportive sentence.
4. One optional CTA.

Priority must be shown through semantic tint and language, not alarm color.

### Input

- Minimum height: `56dp`.
- Radius: `16dp`.
- Persistent label.
- Supporting or validation text below the field.
- Selected accent focus ring.
- Do not use a red border unless submission is blocked by the error.

### Badge and chip

- Pill shape.
- Semantic soft background.
- Text label with optional icon.
- Never rely on color without label text.

### Progress

- Track height: `8dp`.
- Rounded track.
- Always show percentage, value, or remaining amount with the visual state.

### Sheet and dialog

- `28dp` top corners for sheets.
- `20–24dp` content padding.
- Clear title and close affordance.
- Fixed action area when a primary action is present.
- Destructive action requires an explicit consequence statement.

### Iconography and illustration

- Use a consistent rounded-stroke icon language.
- Reduce mixed use of Material Icons, emoji, and flame/reward motifs.
- Use approved warm, human-centered spot illustrations for onboarding, empty, forecast, and insight states.
- Illustration asset production is `[ASUMSI - perlu konfirmasi]`.

## Microcopy

### Formula

Use:

1. Observation
2. Reassurance
3. Next action

### Rules

- Do not shame spending.
- Do not imply the user has failed, lost control, or broken a habit.
- Do not use urgency language for ordinary budget or forecast states.
- Explain why a permission is needed before requesting it.
- Keep one action per insight where possible.

### Approved tone

- "Kamu sudah memakai sebagian besar budget makan. Pilihan kecil hari ini bisa membantu sisa minggu tetap nyaman."
- "Pengeluaran minggu ini terlihat lebih tinggi dari biasanya. Yuk lihat kategori yang paling berpengaruh."
- "Catat transaksi hari ini agar gambaran keuanganmu tetap akurat."

## Screen Rules

### Dashboard and quick logging

- Balance is the visual hero, but the first insight is limited to one top-priority item.
- Quick add remains highly visible and reachable in one tap.
- Replace flame or competitive streak treatment with a gentle routine cue.
- Forecast must show distinct states for insufficient history, loading, ready, and risk.
- Risk state uses Sky/Lilac guidance, never destructive red.

### Analytics

- Keep existing insight richness while reducing visible density into a narrative sequence.
- Use a segmented pill control for weekly, monthly, and annual periods.
- Label charts directly.
- Each major insight surfaces one relevant next action.

### Budgeting and goals

- Use soft progress states from the financial feedback mapping.
- Goal cards show progress, target context, and one contribution action.
- Advanced controls and destructive actions appear lower in the hierarchy.

### Add and edit transaction

- Use a bottom-sheet form.
- Lead with amount, then transaction type, account, and category.
- Place optional details behind progressive disclosure.
- Keep save visible as the persistent primary action.
- Receipt scan entry remains secondary and must not appear functional until OCR is implemented.

### Settings and auto-detection consent

- Group Settings into Personalization, Money Preferences, Smart Capture & Privacy, and Account.
- Expose only the five approved accent swatches.
- Explain the effect of each preference.
- Auto-detection onboarding follows three steps: value, on-device privacy, permission action.
- State clearly that raw notifications stay on-device and users confirm drafts before financial records are created.

## Accessibility Checklist

- Verify text and interactive contrast in light and dark mode.
- Provide content descriptions for icon-only buttons.
- Provide text, pattern, or labels in addition to color for charts and status.
- Ensure all core interactive controls are at least `48dp`.
- Test Indonesian copy without clipping at supported font scaling.
- Test amount-hidden mode.
- Test reduced-motion settings.

## Implementation Sequence

1. Stabilize data safety and inventory hardcoded visual values.
2. Introduce semantic theme tokens and curated accents.
3. Standardize shared component primitives.
4. Rework Dashboard, transaction workflows, receipt scanner states, navigation.
5. Rework Analytics, Budgeting, Goals, Goal Detail, and Accounts.
6. Rework auth, Settings, auto-detection onboarding, and bank whitelist.
7. Complete screenshot and manual QA across all production surfaces.

Visual work does not claim LSTM forecasting or OCR is functional. Their implementation status remains governed by the product requirements and Known Issues / Technical Debt register.

## Definition of Done

The rework is complete when:

- All production screens use the semantic token layer.
- Only the curated warm accents are selectable.
- Normal financial feedback does not default to red.
- Cards, buttons, inputs, badges, sheets, and progress controls use shared component tokens.
- All screens are reviewed in light mode, dark mode, hidden-amount mode, and Indonesian language mode.
- Charts and feedback include non-color meaning.
- No core target is smaller than `48dp`.
- New illustration assets or unconfirmed features are not treated as committed functionality.
