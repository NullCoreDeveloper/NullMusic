# NullMusic Design Guidelines (Material Design 3)

NullMusic strictly adheres to the **Material Design 3 (Material You)** guidelines. For the official specifications, always refer to [m3.material.io](https://m3.material.io/).

This document is the definitive guide for designing and implementing UI in the NullMusic codebase. All new UI work and refactors must follow these principles.

---

## 1. Color System & Theming

We use a dynamic color system, but apply it in a custom way to achieve a unique look.

### Dynamic Color & Seed
*   **Dynamic First:** Colors must come from `MaterialTheme.colorScheme`, but are often modified (e.g., using alpha transparency) to create glass-like effects.
*   **Translucency:** A core part of the NullMusic look is translucent surfaces. For example, cards often use `surfaceVariant.copy(alpha = 0.3f)` rather than solid M3 container colors.

### Semantic Color Roles
Use the correct semantic color roles as defined by our theme:
*   **Primary (`primary` / `onPrimary`):** Used for the most prominent components across the app, active states, and filled buttons.
*   **Surface (`surface` / `onSurface`):** Backgrounds for the app and solid menus.
*   **Translucent Surfaces:** Custom translucent backgrounds (like `surfaceVariant.copy(alpha = 0.3f)`) are heavily used for cards, segmented buttons, and grouped lists to create a softer, layered aesthetic.

---

## 2. Components in Detail

Do NOT strictly force Material 3 components if they break the app's custom aesthetic. Match the existing components found in the app.

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
    *   *Shape:* In NullMusic, prominent text fields (like Search) are overridden to be fully rounded (`CircleShape`) or `RoundedCornerShape(24.dp)`, rather than the M3 default small radius.
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
*   **Custom Cards:** Unlike standard M3 cards (which use solid `surfaceContainer` colors), NullMusic cards typically use:
    *   *Container:* `surfaceVariant.copy(alpha = 0.3f)`
    *   *Shape:* `RoundedCornerShape(24.dp)` or `28.dp`
    *   *Elevation:* 0.dp (flat, translucent look).
*   Grouped items within cards are a common pattern (similar to iOS Settings).

### Navigation & Headers
*   **Top App Bars:** We often use custom implementations or standard `TopAppBar` rather than `LargeTopAppBar`. Headers are sometimes manually placed over scrolling content with custom fade-in animations rather than using standard M3 `Scaffold` scroll behaviors.
*   **Bottom Navigation Bar:** Custom floating tab bars (`ui/component/floatingtabbar/`) are preferred over standard M3 `NavigationBar`.

---

## 3. Typography

Always use `MaterialTheme.typography` but respect the app's established font weights and sizes, which often lean towards bold, expressive headers and softer body text.

---

## 4. Extending the Design System

Before adding a brand new UI component, always check `ui/component/` to see if an existing one already implements our conventions.

**Key Rule:** When working on UI, **look at the existing screens** (like the original Listen Together or Settings screens) and copy their specific visual style, spacing, and modifier chains. Do NOT refactor existing screens to match standard Material 3 unless explicitly requested. Our custom aesthetic takes precedence over M3 guidelines.
