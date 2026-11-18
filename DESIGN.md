# InsightKu Design System

## Purpose

This document defines the visual system for InsightKu, an Android personal-finance app for Indonesian university students managing money independently for the first time.

Financial management should feel clear, personal, and supportive. Interfaces reduce friction in logging, monitoring, planning, and reviewing money without fear, shame, or alarm-heavy feedback.

This document describes what ships today. It is not a proposal. Values below are taken from the token layer, not from a mood board. Where the app and this document disagree, the code is the source of truth until the token layer is changed deliberately.

## Principles

1. **Calm over alarm.** Budget and forecast states guide with plain feedback. Red is never the default treatment for normal spending, reached budgets, or deficit risk.
2. **Actionable over judgmental.** Every insight states what happened and offers one practical next step. Copy avoids blame, urgency, and competitive pressure.
3. **Quiet chrome, one moment.** Surfaces, borders, and text stay neutral. Color lives in one accent, the progress bar, and a chart where each value means something.
4. **Frictionless daily use.** Logging a transaction stays the dominant primary action. Forms reveal optional fields progressively. Important actions have a minimum 48dp touch target.
5. **Accessible by default.** Color is never the only way to communicate status. Text and controls meet contrast requirements in light and dark themes.

## Source of truth

All UI consumes semantic tokens from `core/ui/theme/` and shared components from `core/ui/components/`. Screen code does not introduce local hardcoded colors, shapes, spacing, elevation, or button styles unless the token layer is extended first.

| Concern | File |
|---|---|
| Surfaces, text roles, semantic colors | `AppPalette.kt` |
| Accent swatches | `AccentPalette` in `AppPalette.kt` |
| Spacing, radius, sizes | `Dimens.kt` |
| Type scale | `Type.kt` |
| Theme + CompositionLocals | `Theme.kt`, `PersonalizationProvider.kt` |
| Bottom sheet wrapper | `core/ui/components/bottomsheet/SafeBottomSheet.kt` |

`AGENTS.md` protects `AppPalette.kt`. Do not restyle it as part of a screen fix.

## Color

Surfaces are a cool lavender ramp, not cream or warm beige.

| Role | Light | Dark |
|---|---|---|
| Canvas | `#FAF9FE` | `#0F0A1E` |
| Card / sheet | `#FFFFFF` | `#1A1030` |
| Raised / nested | `#F7F4FE` | `#241840` |
| Hairline | `#ECE7F6` | `#2D2050` |
| Text primary | `#1A1A2E` | `#EDE9FE` |
| Text muted | `#6B647D` | `#AB8FD4` |

Three text roles only: primary, muted, and accent. No per-feature text colors.

### Accent

One accent at a time, chosen by the user. Apricot is the default.

| Name | Value |
|---|---|
| Apricot | `#F59E72` |
| Honey | `#E7B84B` |
| Sage | `#7FAE92` |
| Sky | `#79AFCB` |
| Lilac | `#A898CF` |

Apricot with white text is 2.10:1 and fails WCAG AA. **Text on an accent fill uses `AppPalette.textPrimary` (8.14:1).** Accent fills are reserved for the primary action and progress. Keep the accent off chip backgrounds, icon tiles, card borders, and large surfaces.

Each goal also stores a user-chosen color. It appears as a 6dp identity dot only, never as a button fill, gradient, or icon tint. Category quick-pick colors are data (`QuickIcon.color`) and are kept as stored values.

### Semantic color

Only for real meaning, toned down to sit inside the ramp: `success`, `error`, `warning`. Never for decoration, never as the default for a normal state. No hue gradients, gradient text, glows, colored shadows, radial washes, or neon borders.

## Typography

System sans-serif (`FontFamily.Default`) everywhere. No custom display face is packaged.

Hierarchy comes from size and color, not weight. Weights: 400 (Normal) and 500 (Medium); 600 only for rare emphasis. No `Bold`, `ExtraBold`, gradient text, or italic.

| Role | Style |
|---|---|
| Page / sheet title | `titleMedium`, Medium |
| Card figure | `titleLarge`, Medium |
| Section heading | `bodyLarge`, Medium |
| Body | `bodyMedium` / `bodyLarge` |
| Meta, caption | `bodySmall` / `labelSmall`, muted |

Sentence case everywhere. No all-caps labels, no wide tracking on small text, no negative tracking on small text. Numbers use one format per screen (`NumberFormatter.formatCurrencyCompact`) and tabular figures where the font supports them.

## Spacing and layout

Base unit `4dp`. Values come from `Dimens.kt`.

| Token | Value |
|---|---|
| Screen edge | `ScreenHorizontalPadding` = 18dp |
| Card gap | `CardSpacing` = 12dp |
| Card padding | `CardInnerPaddingLarge` = 20dp |
| Compact padding | `CardInnerPadding` = 16dp |
| Sheet content padding | `BottomSheetContentPadding` = 24dp |
| Bottom clearance above nav | `ContentBottomPadding` = 88dp |

Space shows grouping: gaps inside a group are smaller than gaps between sections. Icons sit in one column, labels start on one line, numbers align right. Prefer whitespace over dividers and shadows.

## Shape, elevation, motion

| Element | Radius |
|---|---|
| Control / input / row | `CornerRadiusMedium` = 12dp |
| Button | `ButtonRadius` = 16dp |
| Card | `CardRadius` = 20dp |
| Sheet / panel | `BottomSheetRadius` = 28dp |
| Progress track | `ProgressBarHeight` = 8dp |

Nested radius = outer radius − padding, never below 4dp. Not everything is a pill. No pill badges, no `CircleShape` icon tiles.

Depth comes from surface steps and a 1dp hairline, not shadows. Standard cards use `0dp` elevation. Motion: one ease-out curve, `tween(300)` for progress, short color/surface transitions for feedback. No looping pulses, bouncy springs, parallax, or hover scale.

## Components

### Buttons

| Component | Rule |
|---|---|
| Primary | `SheetPrimaryButton`: accent fill, dark text, 52dp, 16dp radius |
| Secondary | `SheetSecondaryButton`: hairline outline, primary text, same size |
| Text | `SheetTextButton`: no container, 48dp target, muted text |

One primary and at most one secondary action per view. No icon on a button unless it explains the action.

### Card

`AppPalette.card`, `CardRadius` 20dp, `CardInnerPaddingLarge` 20dp, 1dp hairline, no shadow. One card = one group of related information. No nested cards inside a card or a sheet.

### Bottom sheet

All sheets use `SafeBottomSheet`. It owns:

- the single drag handle (`DefaultDragHandle`, 40 × 4dp),
- status-bar guard and `WindowInsets.safeDrawing`,
- `imePadding()` so fields stay visible above the keyboard,
- an optional fixed `footer` for actions that must not scroll away.

Sheets use `SheetHeader(title, subtitle)` — a `titleMedium` title plus one muted line. No chip above the title, no full-width divider under it, no oversized hero icon. Form labels are sentence case with zero tracking. Sheet-specific content (form fields, pickers, action lists) stays specific; only the shell is shared.

`PremiumDialog` (confirmation) and `PremiumDatePicker` are `Dialog` windows, not sheets. The date picker must render above a sheet, so it stays a window.

### Progress

`ProgressBarHeight` 8dp, rounded, solid accent on a hairline track. Always show percentage or remaining amount with the visual state. No milestone dots, no gradient fill.

### Charts

One main chart, one accent series, muted grid, muted labels. No rainbow series, glowing bars, uppercase day labels, or area gradient. The sign of a value is shown as text, not only as color.

### Icons

One set: Material `Icons.Outlined.*` (or `AutoMirrored` where direction matters). 18dp (`IconSizeMedium`), `AppPalette.textMuted`. No emoji, no trophy, no sparkle, no per-icon color, no tinted icon box.

## Copy

- Sentence case, no trailing period on labels, buttons, or headings.
- Short and specific. No exclamation marks, no hype words, no "AI-powered" on everything.
- No unverified claims. OCR and forecasting are not shipped as working features; copy must not imply they are.
- A change says what it compares to.
- Empty states state the next action in one plain sentence.
- Indonesian-first strings in `values-id/`, English in `values/`. Both locales must fit without wrapping: section titles and nav labels use `FitText` or a short label, never a second line.

## Screen rules

- **Dashboard:** balance is the hero; one priority insight only.
- **Goals / Goal Detail:** one aggregated figure leads; progress is the single colored moment; nominal sisa closes the progress row.
- **Budgeting:** section headers use `SectionHeaderWithAdd`; the add action stays on the title row in every locale. Empty recurring/installment states use the same header, spacing, and button style as the sections above them.
- **All transactions:** search, filter, and sort stay quiet; rows are a list, not a grid of cards.
- **Bottom nav:** five items, one label line each. The center add button is the only filled element.
- **Settings:** grouped lists, no decorative tiles.

## Definition of done

- All screens use token values, not literals.
- Accent fills carry dark text.
- No hue gradient, glow, emoji-as-icon, pill badge, or nested card.
- Sheets use `SafeBottomSheet` + `SheetHeader`; primary actions sit in the fixed footer.
- Indonesian copy never wraps into a second line in bounded controls.
- Dark theme, hidden-amount mode, and reduced-motion settings are checked.
- Visual work does not claim OCR, LSTM forecasting, or offline receipt scanning is functional. Their status lives in `.claude/PRD/KNOWN_ISSUES.md`.
