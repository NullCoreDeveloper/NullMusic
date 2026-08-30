# Echo Music Design Guidelines (Material Design 3)

Echo Music strictly adheres to the **Material Design 3 (Material You)** guidelines. For the official specifications, always refer to [m3.material.io](https://m3.material.io/).

This document is the definitive guide for designing and implementing UI in the Echo Music codebase. All new UI work and refactors must follow these principles.

---

## 1. Color System & Theming

We embrace the Material 3 Dynamic Color system to provide a personalized, expressive experience. 

### Dynamic Color & Seed
*   **Dynamic First:** Colors must *always* come from `MaterialTheme.colorScheme`. Never hardcode hex values (`#FF...`) in individual screens or components.
*   **Seed Color:** The app uses a default seed color (`DefaultThemeColor = 0xFFED5564`) to generate a full tonal palette when system dynamic color is unavailable or disabled. The current spec version is `SPEC_2025` using `PaletteStyle.TonalSpot`.

### Semantic Color Roles
Use the correct semantic color roles as defined by M3:
*   **Primary (`primary` / `onPrimary` / `primaryContainer` / `onPrimaryContainer`):** Used for the most prominent components across the app, such as active states, primary FABs, and filled buttons.
*   **Secondary (`secondary` / `onSecondary` / `secondaryContainer` / `onSecondaryContainer`):** Used for less prominent components, like filter chips, selection controls, and secondary navigation elements.
*   **Tertiary (`tertiary` / `onTertiary` / `tertiaryContainer` / `onTertiaryContainer`):** Used for contrasting accents that need to stand out from primary/secondary elements, but aren't errors (e.g., a special "Now Playing" badge).
*   **Error (`error` / `onError` / `errorContainer` / `onErrorContainer`):** Used for destructive actions (like deleting a playlist) or error states.
*   **Surface (`surface` / `onSurface`):** Backgrounds for components like cards, bottom sheets, and menus.
*   **Surface Variant (`surfaceVariant` / `onSurfaceVariant`):** Differentiated backgrounds for standard components, such as search bars or outlined cards.
*   **Background (`background` / `onBackground`):** The primary app background.
*   **Outline (`outline` / `outlineVariant`):** Used for boundaries, like text field outlines, dividers, or outlined button borders.

---

## 2. Components in Detail

All components must be sourced from `androidx.compose.material3.*`. **Do not** use Material 2 (`androidx.compose.material.*`) components.

### Buttons & FABs
*   **Filled Button:** High emphasis. Used for the primary action on a screen (e.g., "Play All", "Save").
    *   *Color:* `containerColor = primary`, `contentColor = onPrimary`.
    *   *Shape:* Fully rounded (`CircleShape`).
*   **Filled Tonal Button:** Medium emphasis. Used for important actions that shouldn't distract from the primary action.
    *   *Color:* `containerColor = secondaryContainer`, `contentColor = onSecondaryContainer`.
*   **Outlined Button:** Medium-low emphasis. Contains actions that are important but not primary.
    *   *Color:* Transparent container, `contentColor = primary`, `border = outline`.
*   **Text Button:** Low emphasis. Used for secondary actions (e.g., "Cancel" in dialogs, "Learn more").
    *   *Color:* Transparent container, `contentColor = primary`.
*   **Floating Action Button (FAB):** Represents the primary action of a screen.
    *   *Primary FAB:* `containerColor = primaryContainer`, `contentColor = onPrimaryContainer`. Shape is typically `RoundedCornerShape(16.dp)` (Large FAB is `28.dp`).

### Dialogs & Popups
*   **Alert Dialogs:** Used to interrupt the user with urgent information, details, or actions.
    *   *Shape:* `RoundedCornerShape(28.dp)` (Extra Large).
    *   *Background:* `surface` with a tonal elevation of `6.dp` (usually handled automatically by M3 `AlertDialog`).
    *   *Buttons:* Confirm/Positive action **must** be a filled `Button`. Cancel/Dismiss action **must** be a `TextButton`.
*   **Options & Dropdown Menus (Popups):** Used for overflow actions (e.g., three-dot menu on a song).
    *   *Shape:* `RoundedCornerShape(4.dp)` (Extra Small) to `RoundedCornerShape(8.dp)` (Small).
    *   *Background:* `surfaceContainer` (or `surface` with tonal elevation).
    *   *Items:* `DropdownMenuItem`. Text should be `bodyLarge` colored `onSurface`. Leading icons should be `onSurfaceVariant`.
    *   *Animation:* Menus should cascade open from the point of interaction (anchor point).

### Input & Selection Controls
*   **Text Fields:** Use `OutlinedTextField` or `TextField` (Filled).
    *   *Shape:* In Echo Music, prominent text fields (like Search) are overridden to be fully rounded (`CircleShape`) or `RoundedCornerShape(24.dp)`, rather than the M3 default small radius.
    *   *Colors:* `focusedBorderColor = primary`, `unfocusedBorderColor = outline`.
*   **Switches, Checkboxes, Radio Buttons:** 
    *   *Active state:* `primary` or `primaryContainer`.
    *   *Inactive state:* `surfaceVariant` or `outline`.

### Navigation
*   **Bottom Navigation Bar (Mobile):** Use `NavigationBar`.
    *   *Active Item:* Uses a pill-shaped indicator (`secondaryContainer`) behind the icon. 
    *   *Icon Color:* `onSecondaryContainer` (active), `onSurfaceVariant` (inactive).
*   **Navigation Rail (Tablets/Foldables):** Use `NavigationRail`. Follows similar indicator styling as Bottom Nav.
*   **Top App Bar:** Use `TopAppBar`, `MediumTopAppBar`, or `LargeTopAppBar`.
    *   *Scroll Behavior:* Always integrate `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` or `pinnedScrollBehavior()` so the bar reacts to list scrolling.
    *   *Background:* Transitions from `surface` to `surfaceColorAtElevation` upon scrolling.

### Cards & Surfaces
*   **Elevated Card:** Uses tonal elevation (shadows are minimal in M3). 
    *   *Container:* `surfaceContainerLow`.
*   **Filled Card:** Highest visual emphasis for a card without elevation.
    *   *Container:* `surfaceVariant`.
*   **Outlined Card:** 
    *   *Container:* `surface`.
    *   *Border:* `outlineVariant`.
*   *Shape for Cards:* `RoundedCornerShape(12.dp)` (Medium).

---

## 3. Typography

Avoid setting custom font sizes or weights inline. Always use `MaterialTheme.typography`.

*   **Display (`Large`, `Medium`, `Small`):** Huge text, reserved for short, important text or numerals.
*   **Headline (`Large`, `Medium`, `Small`):** Large text for prominent screen titles.
*   **Title (`Large`, `Medium`, `Small`):** Medium-sized text for app bars and medium-emphasis structural text (e.g., Album titles in a grid).
*   **Body (`Large`, `Medium`, `Small`):** Long-form text, descriptions, and lyrics.
*   **Label (`Large`, `Medium`, `Small`):** Small text used for utility, buttons, overlines, and metadata (e.g., song duration, artist name under a track).

---

## 4. Motion & Animation

Motion in M3 is expressive, fluid, and purposeful. It should guide the user's focus and provide feedback.

### Transition Types
*   **Enter/Exit:** When components appear or disappear, use `AnimatedVisibility`. 
    *   *Fade:* Standard for simple elements (`fadeIn()` / `fadeOut()`).
    *   *Slide:* For bottom sheets or navigation transitions.
    *   *Scale:* For FABs or central popups/dialogs (`scaleIn()` / `scaleOut()`).
*   **State Changes:** Use `animateContentSize()` for expanding/collapsing cards or lyrics blocks. Use `updateTransition` for complex multi-property state changes.

### Easing & Durations
Use Compose's built-in easing curves (`androidx.compose.animation.core.*`):
*   **Emphasized (`FastOutExtraSlowIn` / `EmphasizedEasing`):** The standard M3 easing. Starts quickly and ends slowly. Use for major screen transitions or expanding elements.
*   **Standard (`FastOutSlowInEasing`):** For simple, small-scale state changes (e.g., button press ripples, switch toggling).
*   **Durations:**
    *   *Short (50-200ms):* Button presses, fading icons, color changes.
    *   *Medium (200-400ms):* Expanding cards, opening dropdown menus, bottom sheet slides.
    *   *Long (400-500ms+):* Full screen navigation transitions.

---

## 5. Elevation (Tonal vs. Shadow)

Material 3 moves away from shadow-based elevation and towards **tonal elevation**.

*   **Tonal Elevation:** Differentiate overlapping surfaces through color tinting rather than drop shadows. Components like `Surface`, `Card`, and `TopAppBar` have a `tonalElevation` parameter.
*   As `tonalElevation` increases (e.g., from `0.dp` to `3.dp` to `6.dp`), the surface color blends slightly more with the `primary` color, becoming lighter in dark mode and darker in light mode.
*   **Shadow Elevation:** Used sparingly. Reserved for highly elevated, floating components like FABs or Dialogs, and should always be paired with tonal elevation.

---

## 6. Extending the Design System

Before adding a brand new UI component, always check `ui/component/` to see if an existing one already implements our conventions. If you must build a new component:

1.  Consult [m3.material.io](https://m3.material.io/) for the correct structure, state mappings, and interaction patterns.
2.  Apply the pattern consistently across all similar components.
3.  Ensure it uses `MaterialTheme` for all styling (never hardcoded values).
4.  Update this `DESIGN.md` file if a new foundational pattern is established.
