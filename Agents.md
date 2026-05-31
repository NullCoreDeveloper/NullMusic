# Echo Music — AGENTS.md

> **Purpose:** This document is the authoritative guide for any AI agent, automated tool, or contributor making changes to the Echo Music codebase. It establishes inviolable rules for UI consistency, Material Design 3 compliance, spacing, theming, and aesthetic integrity. Every feature addition, modification, or removal must pass the checks defined here before being merged.

---

## Table of Contents

1. [Project Identity](#1-project-identity)
2. [The Non-Negotiable UI Contract](#2-the-non-negotiable-ui-contract)
3. [Material Design 3 — The Law](#3-material-design-3--the-law)
4. [Spacing & Layout System](#4-spacing--layout-system)
5. [Typography System](#5-typography-system)
6. [Color & Theming System](#6-color--theming-system)
7. [Component-by-Component Rules](#7-component-by-component-rules)
8. [Screen-by-Screen Guidelines](#8-screen-by-screen-guidelines)
9. [Animation & Motion Rules](#9-animation--motion-rules)
10. [Adding a New Feature — Checklist](#10-adding-a-new-feature--checklist)
11. [Removing a Feature — Checklist](#11-removing-a-feature--checklist)
12. [What Agents Must Never Do](#12-what-agents-must-never-do)
13. [Architecture Conventions Agents Must Follow](#13-architecture-conventions-agents-must-follow)
14. [Code Style Enforcement](#14-code-style-enforcement)

---

## 1. Project Identity

Echo Music is a **modern, open-source Android YouTube Music client** built with:

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Design System:** Material Design 3 (Material You)
- **Architecture:** MVVM + Repository Pattern
- **DI:** Koin
- **Async:** Coroutines + Flow
- **Media:** ExoPlayer via `SimpleMediaService`
- **Database:** Room
- **Networking:** Ktor + OkHttp
- **Image Loading:** Coil

**Brand feel:** Clean, dark-first, music-centric, immersive, premium. The UI should feel like it belongs alongside Spotify and YouTube Music — never like a hobby project. Every screen, every component, every padding value communicates craft.

---

## 2. The Non-Negotiable UI Contract

These rules apply to **every single change** — no exceptions, no shortcuts.

### 2.1 Consistency Over Cleverness
If a new component you're adding doesn't match the visual weight, border radius, spacing rhythm, or color role of existing components on the same screen, **it must be redesigned before merging**. A feature that looks out of place is worse than no feature.

### 2.2 No Hardcoded Values
Never hardcode pixel values, colors, font sizes, or shape values inline. Always pull from the app's theme tokens (`MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, `MaterialTheme.shapes.*`, or the shared `Dimens` object). Hardcoded values will drift and break the UI as soon as the user changes their dynamic color or system font scale.

### 2.3 No Visual Regressions
A change to one screen must not cause visual regressions on any other screen. Check adjacent screens, the bottom sheet, the mini-player bar, and the Now Playing screen after every change. These surfaces share components and state.

### 2.4 Dark Mode Is Primary
Echo Music is a dark-first application. Every new component must look correct and polished in dark mode. Light mode support is secondary but must not be broken. Always verify both.

### 2.5 Dynamic Color (Material You) Must Always Work
The app supports Android 12+ dynamic color. No component may use a fixed brand color that bypasses `MaterialTheme.colorScheme`. If a component requires a specific accent (e.g., the Echo Find pulse ring), derive it from `colorScheme.primary` or `colorScheme.tertiary`, never from a hardcoded hex.

---

## 3. Material Design 3 — The Law

Echo Music is a **Material Design 3 (Material You)** application. All UI work must strictly follow M3 specifications. The following rules are absolute.

### 3.1 Color Roles — Use the Right Role for the Right Job

| Role | When to Use |
|---|---|
| `colorScheme.primary` | Primary interactive elements, key CTAs, active states |
| `colorScheme.onPrimary` | Text/icons placed on a primary-colored surface |
| `colorScheme.primaryContainer` | Tonal containers for selected/active chip or card state |
| `colorScheme.onPrimaryContainer` | Content inside a primary container |
| `colorScheme.secondary` | Supporting interactive elements, secondary actions |
| `colorScheme.tertiary` | Accents, special highlights |
| `colorScheme.surface` | Cards, bottom sheets, dialog backgrounds |
| `colorScheme.surfaceVariant` | Slightly elevated surface containers (e.g., search bar bg) |
| `colorScheme.background` | Screen background |
| `colorScheme.error` | Error states only — never for decorative purposes |
| `colorScheme.outline` | Borders, dividers, unfocused outlines |
| `colorScheme.outlineVariant` | Subtle dividers that shouldn't compete with content |

**Never** use `colorScheme.error` for anything that isn't an actual error. **Never** repurpose `colorScheme.background` as a card color.

### 3.2 Elevation & Tonal Elevation

M3 uses **tonal elevation** (surface tint) rather than shadow-only elevation. Use `Surface(tonalElevation = X.dp)` correctly:

- `0.dp` — flat background surfaces
- `1.dp` — cards that sit on the background
- `2.dp` — bottom navigation bar
- `3.dp` — FABs, mini-player bar
- `6.dp` — modals, bottom sheets
- `8.dp` — dialogs

Do not stack tonal elevation arbitrarily. A `tonalElevation` value above `12.dp` for a non-modal surface is almost always wrong.

### 3.3 Shape System

Echo Music uses rounded shapes throughout. Always use `MaterialTheme.shapes.*`:

- `shapes.extraSmall` (4dp) — small chips, tags
- `shapes.small` (8dp) — small cards, text field containers
- `shapes.medium` (12dp) — standard cards, dialog content areas
- `shapes.large` (16dp) — large cards, modal bottom sheets
- `shapes.extraLarge` (28dp) — hero cards, album art containers, prominent FABs
- `CircleShape` — icon buttons, avatar thumbnails, round playback controls

**Never use a raw `RoundedCornerShape(Xdp)` value.** Always use a `MaterialTheme.shapes.*` token unless a specific radius is architecturally justified and documented inline with a `// Echo: intentional override because…` comment.

### 3.4 Components to Always Use from Material3

When a built-in M3 Compose component exists for your use case, use it. Do not write custom versions of:

- `Button`, `OutlinedButton`, `TextButton`, `FilledTonalButton`
- `IconButton`, `FilledIconButton`, `FilledTonalIconButton`
- `Card`, `ElevatedCard`, `OutlinedCard`
- `Chip` (AssistChip, FilterChip, InputChip, SuggestionChip)
- `NavigationBar`, `NavigationBarItem`
- `TopAppBar`, `LargeTopAppBar`, `MediumTopAppBar`
- `ModalBottomSheet`, `BottomSheetScaffold`
- `LinearProgressIndicator`, `CircularProgressIndicator`
- `Scaffold`
- `Snackbar`, `SnackbarHost`
- `Switch`, `Checkbox`, `RadioButton`
- `Slider`
- `TextField`, `OutlinedTextField`
- `ListItem`
- `Divider`, `HorizontalDivider`, `VerticalDivider`

Custom components are only acceptable when no M3 component can express the required behavior (e.g., the canvas animation layer, the waveform visualizer). Even then, their colors, shapes, and spacing must be derived from the theme.

---

## 4. Spacing & Layout System

Echo Music uses an **8dp base grid**. All margins, paddings, and gaps must be multiples of 4dp, with 8dp as the standard step.

### 4.1 The Spacing Scale

```
4dp   — micro gap (icon-to-label, tight chip pairs)
8dp   — small internal padding (chip horizontal padding, list item divider gap)
12dp  — medium internal padding (card internal padding, small list item vertical padding)
16dp  — standard padding (screen horizontal padding, standard list item padding)
20dp  — slightly expanded padding (section headers, search bar inner padding)
24dp  — large spacing (between sections, hero card padding)
32dp  — extra-large gap (between major sections on a screen)
48dp  — section breathing room (e.g., space above bottom navigation bar)
```

**Never use arbitrary values like `7.dp`, `13.dp`, `22.dp`, or `37.dp`.**

### 4.2 Screen-Level Horizontal Padding

The canonical horizontal padding for all content lists and sections is `16.dp`. This creates a consistent left-right gutter across every screen. Do not deviate without a strong reason (e.g., full-bleed album art headers may use `0.dp` intentionally).

### 4.3 List & Grid Item Rules

- Vertical list items: minimum touch target height of `48.dp`
- Horizontal padding inside a list item: `16.dp` left, `16.dp` right
- Vertical padding inside a list item: `12.dp` top, `12.dp` bottom (for compact items), `16.dp` for standard items
- Grid items (e.g., album art grids): use `8.dp` gap between columns and rows
- Section headers above lists: `16.dp` horizontal padding, `8.dp` top margin from the previous section's last item, `4.dp` bottom margin before the first item

### 4.4 Bottom Player Bar Clearance

The persistent mini-player bar at the bottom has a known height. Every `LazyColumn`, `LazyVerticalGrid`, and scrollable content must add `contentPadding` at the bottom equal to the mini-player height + bottom navigation height + `16.dp` breathing room. Use the `LocalPlayerAwareWindowInsets` or equivalent insets utility already in the project. **Never let content hide behind the player bar.**

### 4.5 System Insets

All screens must consume window insets using `WindowInsets` APIs. Use `Scaffold` with `contentWindowInsets` properly set. Edge-to-edge content is expected — do not add manual `statusBarHeight` offsets. Use `Modifier.statusBarsPadding()`, `Modifier.navigationBarsPadding()`, or `Modifier.safeContentPadding()` as appropriate.

---

## 5. Typography System

Always use `MaterialTheme.typography.*`. Never use a raw `TextStyle(fontSize = X.sp)` without a corresponding M3 type role.

### 5.1 Type Scale Mapping for Echo Music

| Style | Usage in Echo Music |
|---|---|
| `displayLarge` | Not used (too large for mobile music UI) |
| `displayMedium` | Not used |
| `displaySmall` | Full-screen lyric hero text only |
| `headlineLarge` | Screen titles in `LargeTopAppBar` |
| `headlineMedium` | Section headers on the Home screen |
| `headlineSmall` | Album/playlist title on the Now Playing screen |
| `titleLarge` | Primary song title in the player and list headers |
| `titleMedium` | Artist names in cards, playlist names |
| `titleSmall` | Section sub-labels, chip labels |
| `bodyLarge` | Description text, settings item primary text |
| `bodyMedium` | Secondary description, metadata |
| `bodySmall` | Timestamps, play counts, bitrate labels |
| `labelLarge` | Button labels, tab labels |
| `labelMedium` | Badge text, notification counters |
| `labelSmall` | Fine print, legal text, version numbers |

### 5.2 Text Truncation

- Song titles: `maxLines = 1`, `overflow = TextOverflow.Ellipsis`
- Artist names: `maxLines = 1`, `overflow = TextOverflow.Ellipsis`
- Descriptions: `maxLines = 2` or `3`, `overflow = TextOverflow.Ellipsis`
- Settings descriptions: `maxLines = 3`, `overflow = TextOverflow.Ellipsis`

**Never allow text to overflow its container without truncation.** This breaks the layout and makes the UI look broken.

### 5.3 Text Colors

- Primary text on surface: `colorScheme.onSurface`
- Secondary/supporting text: `colorScheme.onSurfaceVariant`
- Disabled text: `colorScheme.onSurface.copy(alpha = 0.38f)`
- Active/highlighted text (e.g., current lyric line): `colorScheme.primary`

---

## 6. Color & Theming System

### 6.1 Dynamic Color (Material You)

On Android 12+, the app extracts a color scheme from the user's wallpaper. **All components must remain visually coherent across any possible dynamic color palette.** This means:

- Do not rely on a specific hue being primary. A user's wallpaper could make `colorScheme.primary` green, orange, purple, or any color.
- Do not use absolute contrast ratios based on a fixed dark palette. Let the M3 color system handle contrast.
- Test new UI with at least three different dynamic color seeds before considering it done.

### 6.2 Fallback / Static Theme

When dynamic color is unavailable (Android < 12 or FOSS build), the app uses a predefined M3 color scheme. This scheme is the single source of truth for all theme colors. It is defined in `app/src/main/java/com/maxrave/echo/ui/theme/`. Agents must not add alternate color definitions elsewhere.

### 6.3 Album Art Color Extraction

The Now Playing screen and canvas animations may extract palette colors from the current album art. These extracted colors are used for ambient glow, background gradients, and lyric highlights. Rules:

- Extracted colors must be applied with alpha clamping (never fully opaque) to avoid clashing with readable text
- Extracted colors must never override `colorScheme` tokens globally — apply them locally with `CompositionLocalProvider`
- Always provide a fallback to `colorScheme.primary` if extraction fails or returns null

### 6.4 Gradient Usage

Gradients are used in Echo Music for: scrim overlays on album art, ambient background effects in the player, and section header fades. Rules:

- Use `Brush.verticalGradient` or `Brush.radialGradient` from Compose — no custom canvas painting for standard gradients
- Gradient colors must be derived from the theme or extracted palette, never hardcoded
- Gradient opacity at the opaque end must not exceed `0.85f` over content — always maintain readability

---

## 7. Component-by-Component Rules

### 7.1 Song List Item

The song list item is the most repeated component in the app. It must always have:

- Thumbnail: `48.dp × 48.dp`, `shapes.small` corner radius, loaded via Coil with placeholder and error states
- Primary text: song title, `titleMedium`, `onSurface`, single line, ellipsis
- Secondary text: artist name, `bodyMedium`, `onSurfaceVariant`, single line, ellipsis
- Trailing: icon button (overflow menu trigger), `48.dp` touch target, `onSurfaceVariant` tint
- Vertical padding: `12.dp` top and bottom
- Horizontal padding: `16.dp` left and right
- Selected state: `primaryContainer` background, `onPrimaryContainer` text

### 7.2 Album / Playlist Card

- Width: determined by grid column or horizontal scroll container — **never fixed width hardcoded**
- Album art: square aspect ratio, `shapes.medium` corners
- Title: `titleSmall`, below the art, single line, ellipsis, `onSurface`
- Subtitle: `bodySmall`, `onSurfaceVariant`, single line
- Card container: `ElevatedCard` with `tonalElevation = 1.dp`

### 7.3 Bottom Navigation Bar

- Use `NavigationBar` from Material3
- Exactly the tabs that currently exist — do not add new top-level navigation destinations without a product decision logged in the issue tracker
- Icons: outlined (inactive), filled (active)
- Labels: always visible — do not hide labels
- Indicator pill: uses `primaryContainer` — do not override

### 7.4 Now Playing / Full Player Screen

This is the most visually prominent screen in the app. Any change here must be reviewed with extreme care:

- Album art: should occupy a large portion of the screen, aspect ratio locked to 1:1, `shapes.extraLarge` corners
- Song title: `headlineSmall`, centered or left-aligned per existing layout
- Artist name: `titleMedium`, `onSurfaceVariant`
- Playback controls: centered, icon buttons with `48.dp` minimum touch target; play/pause button is larger (`64.dp`) and uses `FilledIconButton` with `primary` color
- Progress slider: use `Slider` from M3, do not build a custom one unless the existing one is replaced with an improved custom implementation that still respects theme colors
- Background: ambient gradient derived from album art colors (see Section 6.3)
- The layout must not shift or reflow when the song changes — transitions should be animated, not abrupt

### 7.5 Mini Player Bar

- Always anchored at the bottom, above the navigation bar
- Height: fixed, consistent — do not let content make it taller
- Album art: `40.dp` circle or small rounded square, consistent with existing style
- Song title: single line, `titleSmall`, `onSurface`
- Controls: play/pause + skip next only — do not add more controls without product sign-off
- Tappable: the entire bar (excluding control buttons) opens the full player
- Elevation: `tonalElevation = 3.dp`, visually distinct from the content behind it

### 7.6 Settings Screen

- Use `ListItem` for every setting row
- Use `Switch` for boolean toggles — never a `Checkbox` for on/off
- Group settings under section headers using `HorizontalDivider` + a label in `labelMedium`/`titleSmall` style
- Destructive actions (e.g., clear cache, log out) must use `colorScheme.error` for the text label and be placed at the bottom of their respective group
- Preference changes must take effect immediately or show a clear "restart required" snackbar

### 7.7 Search Bar & Search Screen

- Use the M3 `SearchBar` or `DockedSearchBar` component — do not build a custom text field for search
- Search suggestions: `ListItem` style, consistent with song list items
- Empty state: centered illustration + `bodyLarge` message, `onSurfaceVariant` color — never just a blank screen
- Loading state: `LinearProgressIndicator` below the search bar

### 7.8 Dialogs & Bottom Sheets

- Use `AlertDialog` for confirmations (destructive actions, errors)
- Use `ModalBottomSheet` for contextual actions (song options menu, quality selection, share options)
- Bottom sheet drag handle: always visible (`showDragHandle = true`)
- Dialog buttons: follow M3 button hierarchy — confirm action uses `TextButton` or `Button`, cancel uses `TextButton`
- Never use a `Dialog` to replace what should be a `ModalBottomSheet`, and vice versa

---

## 8. Screen-by-Screen Guidelines

### 8.1 Home Screen

- The hero/featured section at the top uses full-bleed or edge-extending layout — horizontal padding is dropped for visual impact
- Section headers ("Quick Picks", "Recommended for You", etc.) use `16.dp` horizontal padding, `headlineMedium` or `titleLarge` typography
- Horizontal scroll rows: cards are `160.dp` wide (or proportional), `8.dp` gap between cards, `16.dp` leading/trailing padding on the scroll container
- Do not add new sections without considering scroll depth — the home screen must not require excessive scrolling before showing meaningful content

### 8.2 Library Screen

- List view and grid view must both be supported — do not remove either
- Sort/filter controls sit below the top app bar, not inside it
- Empty states (empty library, no downloads) must show an illustration and a clear CTA

### 8.3 Explore / Browse Screen

- Genre/mood chips: use `SuggestionChip` or `FilterChip` in a `FlowRow` or `LazyRow`
- Charts section: ranked list with position number, consistent with existing design
- Do not introduce new layout patterns on this screen without mirroring the existing card/list vocabulary

### 8.4 Download Manager Screen

- List items show: title, artist, download progress (if in progress), file size, status badge
- Status badge colors: in-progress uses `tertiary`, complete uses `primary`, failed uses `error` — all derived from `colorScheme`
- Swipe-to-delete must show a red background (`colorScheme.error`) with a delete icon

### 8.5 Echo Find (Song Recognition) Screen

- The listening animation (pulse ring) must use `colorScheme.primary` with animated alpha
- Status text must be centered, `titleMedium`
- Results appear as a standard song list item — do not use a special card style that diverges from the rest of the app
- Error/timeout state: clear message + retry button, consistent M3 button style

---

## 9. Animation & Motion Rules

Echo Music uses Compose animations throughout. All motion must feel natural and not janky.

### 9.1 Transition Durations

| Category | Duration |
|---|---|
| Micro interactions (icon state change, ripple) | `100ms – 150ms` |
| Component state transitions (expand/collapse) | `200ms – 300ms` |
| Screen-level transitions | `300ms – 400ms` |
| Hero transitions (full player open/close) | `350ms – 500ms` |

### 9.2 Easing

- **Emphasis in:** `FastOutLinearInEasing` — things leaving the screen
- **Emphasis out:** `LinearOutSlowInEasing` — things entering the screen
- **Standard:** `FastOutSlowInEasing` — most UI transitions
- **Decelerate (settle into place):** `DecelerateInterpolator` equivalent

Use `tween()`, `spring()`, or `keyframes()` from Compose — never use raw `Handler.postDelayed` for UI animations.

### 9.3 Album Art Transitions

When the track changes, the album art must crossfade, not pop/snap. Use Coil's `CrossFade` transition or a manual `AnimatedContent` with a `fadeIn + fadeOut` content transform. Duration: `300ms`.

### 9.4 No Janky Animations

If a new animation cannot run at 60fps on a mid-range device, it must either be optimized or removed. Canvas-heavy animations (the canvas animation feature) must use `rememberCoroutineScope` and render on the `drawWithContent` layer — not inside a `LaunchedEffect` that triggers recomposition on every frame.

---

## 10. Adding a New Feature — Checklist

Before submitting any PR that adds a UI-visible feature, the agent must verify every item below. This is not optional.

**Design Conformance**
- [ ] All new colors are from `MaterialTheme.colorScheme.*` — no hardcoded hex values
- [ ] All new shapes use `MaterialTheme.shapes.*` or `CircleShape` — no raw `RoundedCornerShape(Xdp)`
- [ ] All new text uses `MaterialTheme.typography.*` style roles — no raw `TextStyle(fontSize = X.sp)`
- [ ] All spacing values are multiples of 4dp — no arbitrary pixel values
- [ ] The feature uses M3 components where applicable (see Section 3.4)

**Layout & Spacing**
- [ ] Screen-level horizontal padding is `16.dp` on both sides
- [ ] List items meet the `48.dp` minimum touch target
- [ ] Content does not get hidden behind the mini-player bar or bottom nav bar
- [ ] Window insets are respected — no content hidden behind status bar or gesture nav bar

**Consistency**
- [ ] New list items match the style of existing `SongListItem` / `AlbumCard` components
- [ ] New bottom sheets use `ModalBottomSheet` with a drag handle
- [ ] New dialogs use `AlertDialog` — not custom `Dialog` with a `Column`
- [ ] New settings rows use `ListItem` + `Switch`/`Slider`/`RadioButton` as appropriate

**Dark Mode & Dynamic Color**
- [ ] The feature is visually correct in dark mode
- [ ] The feature is visually correct in light mode
- [ ] The feature renders correctly across at least 3 different Material You color seeds

**Animation**
- [ ] State changes are animated, not instant snaps (except for data-only updates)
- [ ] Transition durations fall within the ranges in Section 9.1
- [ ] No frame drops caused by the new animation (test on a mid-range device or emulator)

**Functional**
- [ ] Empty state is handled and shown (no blank screens)
- [ ] Error state is handled and shown (no silent failures)
- [ ] Loading state shows a progress indicator (`CircularProgressIndicator` or `LinearProgressIndicator`)
- [ ] The feature degrades gracefully when offline

**Architecture**
- [ ] Business logic is in the ViewModel or UseCase — not in a Composable
- [ ] New data is fetched through the Repository layer — not directly from the network in a Composable
- [ ] New preferences are stored via the existing `DataStore` mechanism
- [ ] New database entities follow existing Room entity conventions

---

## 11. Removing a Feature — Checklist

Before submitting any PR that removes a UI-visible feature:

- [ ] The removed feature's UI entry points are fully removed — no orphaned menu items, settings rows, or navigation destinations that lead nowhere
- [ ] Any settings values associated with the feature are either migrated or deprecated gracefully — do not leave dangling `DataStore` keys
- [ ] The screen or section that hosted the feature does not leave a visual gap (empty space, broken grid layout, misaligned components)
- [ ] If a bottom navigation tab is removed, the tab bar still looks balanced and intentional — update icon selection and label layout accordingly
- [ ] Removing the feature does not expose a crash path in remaining code (e.g., a ViewModel that referenced the removed component)
- [ ] The `CHANGELOG.md` and `FEATURES.md` are updated to reflect the removal

---

## 12. What Agents Must Never Do

These are hard prohibitions. Any PR violating these will be closed without review.

1. **Never use hardcoded color values** anywhere in `ui/` packages. Not even `Color.White`, `Color.Black`, or `Color.Transparent` where a theme token exists.

2. **Never place business logic in a `@Composable` function.** No API calls, no database queries, no heavy computations inside a Composable body.

3. **Never use `Thread.sleep()` or `Handler.postDelayed()` for UI timing.** Use Compose `LaunchedEffect` + `delay()`.

4. **Never create a new navigation destination without a route defined in the navigation graph.** No ad-hoc `Intent`-based navigation between Compose screens.

5. **Never set `fillMaxSize()` on a component that doesn't need it.** This is a common source of layout inflation bugs.

6. **Never use `mutableStateOf()` directly inside a `@Composable` without `remember {}`.**

7. **Never bypass the Repository layer.** ViewModels must not hold references to Ktor clients, Room DAOs, or Retrofit services directly.

8. **Never break the mini-player bar.** It must be visible on every scrollable screen. It is the most critical persistent UI element in the app.

9. **Never add a dependency without updating `build.gradle.kts` at the module level and verifying no version conflict with the existing dependency graph.**

10. **Never introduce a new typeface.** Echo Music uses the system font via Material Design 3. Do not add Google Fonts or bundled custom fonts unless this decision has been explicitly made in a GitHub Discussion with maintainer approval.

11. **Never show a raw exception message to the user.** All error states must use friendly, localized strings from `res/strings.xml`.

12. **Never use `@SuppressLint` or `@Suppress` annotations to paper over a real problem.** Fix the root cause.

---

## 13. Architecture Conventions Agents Must Follow

### 13.1 State Management

- All UI state lives in `StateFlow<YourScreenState>` inside the ViewModel
- Use `collectAsStateWithLifecycle()` in Composables — not `collectAsState()` (lifecycle-unsafe)
- State classes must be `data class` with all fields having sensible defaults

```kotlin
data class HomeScreenState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null
)
```

### 13.2 ViewModel Events (Side Effects)

Use `SharedFlow<UiEvent>` for one-shot events (navigation, snackbar, toast):

```kotlin
private val _events = MutableSharedFlow<UiEvent>()
val events: SharedFlow<UiEvent> = _events.asSharedFlow()
```

Collect events in the Composable using `LaunchedEffect(Unit)`.

### 13.3 Repository Contracts

Every new data source must have a corresponding repository interface in the domain layer. Implementation lives in the data layer. This separation is mandatory for testability.

### 13.4 Dependency Injection

All new ViewModels, Repositories, and Services must be registered in the appropriate Koin module file. Do not use manual instantiation (`MyViewModel()`) anywhere in the UI code.

### 13.5 Resource Naming

- String resources: `snake_case`, prefixed by feature (`player_skip_next`, `settings_cache_clear`)
- Drawable resources: `ic_` prefix for icons, `bg_` for backgrounds, `img_` for illustrations
- Dimension resources: use Kotlin constants in `Dimens.kt` instead of `dimens.xml` where the team has established this pattern

---

## 14. Code Style Enforcement

Echo Music follows the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

- Maximum line length: **120 characters**
- Use trailing commas in multi-line function parameters and lists
- Composable functions are named in `PascalCase` with no prefix (`fun SongListItem(...)` not `fun RenderSongListItem(...)`)
- Preview functions are named `@Preview fun SongListItemPreview()` and live in the same file as the composable
- Every public ViewModel function must have a KDoc comment explaining what it does and what state it modifies
- `TODO:` comments must include the author's GitHub handle and a linked issue: `// TODO(@username): fix this - see #123`

**Run before every commit:**

```bash
./gradlew ktlintCheck
./gradlew lint
```

Both must pass with zero errors. Warnings must be reviewed and either fixed or suppressed with justification.

---

*This document is the single source of truth for UI/UX consistency in Echo Music. It supersedes any informal convention, comment in code, or prior PR discussion. When in doubt, open a GitHub Discussion before making a change that isn't clearly covered here.*

*Last updated: June 2026 — Echo Music v4.2.2+*