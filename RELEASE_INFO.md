# Echo Music v5.1.9

### New Features
- **Pin Playlists** — Long-press to pin playlists to the top of your library.
- **Sync to YouTube Music** — Sync local playlists via the playlist menu.
- **Settings Search** — Filter settings categories and sub-settings instantly.
- **Song Suggestions** — Discover and add related songs at the bottom of local playlists.
- **Canvas Provider (Tidal)** — Animated album canvas with improved matching accuracy.
- **Audio Fallback** — Opus failure auto-reroutes to JioSaavn 320 kbps, and vice versa.
- **Markdown in Changelog** — Bold, italics, inline code, and links rendered natively.

### UI & Design
- **Echo Find Screen** — Material You pill button, animated wave visualizer, edge-to-edge blur.
- **Recognition Screen** — Glassmorphism layout with full-square album art and playback FAB.
- **Suggestions & Menus** — Rounded edges on suggestions list; removed grey header backgrounds from bottom sheets.
- **Online Playlist Header** — Count and duration shown above action buttons.
- **Logout Dialog** — Stacked buttons fix text clipping.

### Bug Fixes
- **Crash on Recognition** — Fixed `IllegalStateException` via main thread dispatch.
- **Offline Playback** — Fixed downloaded songs failing due to bad cache length reads.
- **Apple Music Black Screen** — Album art now shows for downloaded songs.
- **Volume Slider** — Stays in sync with system volume.
- **Background Updater** — Runs as background service with auto-retry; fixed notification spam.
- **Comments Button** — Removed from Listen Together screen.

### Build
- **FFmpeg** — Switched to `ffmpeg-kit-audio`; removed unused `aria2c` dependency.
- **ABI Filters** — Fixed per-arch filters to prevent universal APK generation.