# Echo Music v5.1.9

### New Features

- **Pin Playlists** — Added the ability to pin playlists to the top of your library via the long-press menu.
- **Sync Local Playlists to YouTube Music** — Added an option to sync local playlists directly to YouTube Music via the playlist menu.
- **Settings Search Bar** — Added a search bar to the Settings screen for quick filtering of categories and individual sub-settings (e.g., Theme, Cache, Quality).
- **Song Suggestions in Local Playlists** — Added a song suggestions section at the bottom of local playlists, allowing users to discover and add related songs.
- **Tidal Album Canvas Provider** — Replaced the legacy monochrome album canvas with a direct Tidal provider for improved animated visuals, with fallback validation logic for better matching accuracy.
- **Markdown Support in Changelog Screen** — Added full Markdown rendering to the System Update Changelog screen, supporting bold, italics, inline code, and links without third-party dependencies.
- **Easter Egg on About Screen** — Added a Telegram-inspired interactive Easter Egg: tapping the Echo Music logo triggers a 3D flip animation revealing the developer's avatar and name.
- **Bidirectional Audio Fallback Engine** — If an InnerTube Opus stream fails, playback instantly reroutes to JioSaavn 320 kbps (and vice versa), ensuring uninterrupted listening.

---

### UI & Design

- **Recognize Music (Echo Find) Screen** — Redesigned with a Material You pill button, dynamic multi-bar audio wave visualizer, edge-to-edge blurred background, and full Material 3 compliance. The Relisten button is now a pill shape with explicit label text alongside the microphone icon.
- **Recognition Success Screen** — Overhauled into a premium Glassmorphism layout: album art is displayed in its full uncropped square ratio, floating above a blurred high-resolution background with a prominent Floating Action Button for playback.
- **Logout Dialog** — Replaced cramped pill buttons with a clean, vertically-stacked Material 3 button layout to prevent text clipping and improve readability.
- **Online Playlist Screen Header** — Reordered to display song count and duration above the Save / Play / Share action buttons. Added a temporary helper note indicating the "Create or Join" button may need to be tapped twice.
- **Playlist Screen Suggestions List** — Applied rounded edges to the suggestions list at the bottom of the playlist screen, consistent with the playlist songs list styling.
- **Bottom Sheet Menu** — Removed grey backgrounds from header items so they blend seamlessly with the rest of the menu, including playlist menu headers.

---

### Bug Fixes

- **Recognition Screen Crash** — Fixed an `IllegalStateException` during instant playback by correctly dispatching the ExoPlayer command to the main UI thread.
- **Offline Playback** — Fixed an issue where fully downloaded songs (Opus, Saavn 320 kbps, Lossless) failed to play offline due to incorrect file length reading from cache.
- **Apple Music Player Black Screen** — Fixed a bug where downloaded songs displayed a black screen instead of album art when the Apple Music player style was active.
- **Volume Slider Sync** — Fixed an issue where the volume slider in the player menu did not stay in sync with system volume changes.
- **Comments Button** — Removed the comments button from the Listen Together screen.
- **Background Update Downloader** — Improved update downloading by running it as a background service with auto-retry and resolved notification spam.

---

### Build & Infrastructure

- **FFmpeg Optimization** — Switched from `ffmpeg-kit-full` to `ffmpeg-kit-audio` to reduce native library overhead.
- **ABI Filter Fix** — Fixed ABI filters for individual architecture builds (`arm64`, `armeabi`, `x86`, `x86_64`) to prevent unintentional universal APK generation.
- **Removed Unused Dependency** — Removed the unused `aria2c` native dependency from the YouTube downloader to further reduce app size.