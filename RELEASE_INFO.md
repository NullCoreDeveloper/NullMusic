# Echo Music v5.1.9
### New Features
- **Pin Playlists** — Long-press to pin playlists to the top of your library.
- **Sync to YouTube Music** — Sync local playlists to YouTube Music via playlist menu.
- **Settings Search Bar** — Filter settings categories and sub-settings instantly.
- **Song Suggestions** — Discover and add related songs at the bottom of local playlists.
- **Tidal Canvas Provider** — Animated album canvas via Tidal with better matching accuracy.
- **Markdown in Changelog** — Bold, italics, inline code, and links rendered natively.
- **About Easter Egg** — Tap the logo for a 3D flip revealing the developer's avatar.
- **Audio Fallback Engine** — Opus failure? Auto-reroutes to JioSaavn 320 kbps (and vice versa).

### UI & Design
- **Echo Find Screen** — Material You pill button, animated wave visualizer, edge-to-edge blur.
- **Recognition Success** — Glassmorphism layout with full-square album art and playback FAB.
- **Logout Dialog** — Vertically stacked buttons fix text clipping.
- **Online Playlist Header** — Count/duration shown above action buttons.
- **Suggestions List** — Rounded edges, consistent with songs list.
- **Bottom Sheet Menu** — Removed grey header backgrounds.

### Bug Fixes
- **Crash on Recognition** — Fixed `IllegalStateException` via main thread dispatch.
- **Offline Playback** — Fixed downloaded songs failing due to bad cache length reads.
- **Apple Music Black Screen** — Album art now shows for downloaded songs.
- **Volume Slider** — Now stays in sync with system volume.
- **Comments Button** — Removed from Listen Together screen.
- **Background Updater** — Runs as background service with auto-retry; fixed notification spam.

### Build & Infrastructure
- **FFmpeg** — Switched to `ffmpeg-kit-audio` to cut overhead.
- **ABI Filters** — Fixed per-arch filters to prevent universal APK generation.
- **Removed `aria2c`** — Dropped unused dependency to reduce app size.
