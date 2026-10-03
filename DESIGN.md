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

## 2. Liquid Glass System (Glassmorphism)

A signature part of NullMusic's design is the **Liquid Glass** effect, which provides high-quality blur, refraction, and translucency to navigation bars, headers, and media players.

### Core Implementation
The Liquid Glass effect is driven by a custom `Modifier.liquidGlass()` extension found in `GlassEffect.kt`. It utilizes a heavily customized RenderEffect pipeline (available on Android 12 / API 31+) over a recorded `Backdrop`.

```kotlin
// Basic Usage
Modifier.liquidGlass(
    config = LocalGlassEffectConfig.current,
    shape = RoundedCornerShape(24.dp),
    applyEdgeEffects = true // Set to false for full-screen surfaces
)
```

### Technical Details & Parameters

1. **Backdrop Resolution Scaling:** To maintain high performance, the glass surface records and processes its backdrop at a lower resolution (down to `33%`), relying on the blur to hide the upscaling. The `glassResolutionScale` function dynamically adjusts this based on the requested blur radius.
2. **API Requirements:** Glass requires Android 12 (`Build.VERSION_CODES.S`). Devices on older versions gracefully fall back to solid/standard transparency by checking `isGlassSupported()`.
3. **Lens Refraction (Edge Effects):** Small pills (like floating bottom bars) read as physical glass by applying edge effects. These include lens refraction (`lensHeight`, `lensAmount`), a specular highlight rim (`Highlight.Default`), and a drop shadow (`Shadow.Default`). **Note:** These are enabled via `applyEdgeEffects = true` and rely on `CornerBasedShape`s. (Using a non-corner-based shape throws `UnsupportedOperationException`).
4. **Full-Screen Blur Tuning:** For large surfaces like the full-screen player background, edge effects should be disabled (`applyEdgeEffects = false`) to avoid a stray band of light. Instead, they use a `PLAYER_BLUR_MULTIPLIER` (4x the standard pill blur) to match the deep-blurred material seen in Apple Music.

### Color and Vibrancy
*   **Vibrancy:** Configurable saturation multiplier (default `1.5x` saturation) applies directly via `colorControls` RenderEffect.
*   **Surface Tint:** Apple's glass is typically a light material on light content and dark on dark. If `surfaceTintColor` is unspecified, it defaults to `0xFFFAFAFA` for light mode and `0xFF121212` for dark mode, layered with a typical `surfaceOpacity` of `0.4f`.
*   **Flat Integration:** Action buttons (like FABs or overflow menus) placed on top of Liquid Glass surfaces must use flat elevations (`elevation = 0.dp`). M3 drop shadows interact poorly with translucent layers and will render as ugly dark blobs beneath the component.

---

## 3. Components in Detail

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
*   Grouped items within cards are a common pattern.

### Navigation & Headers
*   **Top App Bars:** We often use custom implementations or standard `TopAppBar` rather than `LargeTopAppBar`. Headers are sometimes manually placed over scrolling content with custom fade-in animations rather than using standard M3 `Scaffold` scroll behaviors.
*   **Bottom Navigation Bar:** Custom floating tab bars (`ui/component/floatingtabbar/`) are preferred over the standard M3 `NavigationBar`.

---

## 4. Typography & Iconography

*   **Typography:** Always use `MaterialTheme.typography` but respect the app's established font weights and sizes. NullMusic leans towards bold, expressive headers and softer, highly legible body text.
*   **Iconography:** We use a mix of Material Symbols/Icons Extended (`androidx.compose.material.material-icons-extended`) and custom SVG drawables. Check `scripts/compose_svg_drawable.py` and existing drawables before importing new vector assets to avoid duplication.

---

## 5. Extending the Design System

Before adding a brand new UI component, always check `ui/component/` to see if an existing one already implements our conventions.

**Key Rule:** When working on UI, **look at the existing screens** (like the original Listen Together or Settings screens) and copy their specific visual style, spacing, and modifier chains. Do NOT refactor existing screens to match standard Material 3 unless explicitly requested. Our custom aesthetic takes precedence over M3 guidelines.


## Ambient Mode Canvas

Ambient Mode may layer muted Canvas video artwork inside the existing album-art square. Keep the original square size, rounded clipping, and interaction surface unchanged; Canvas is a non-interactive visual layer above the normal album art and follows playback state. The existing glow background remains separate underneath the screen.
