# Echo Music v5.2.0 Changelog

## New Features

- Added native Google Assistant integration — say "Play [song name] on Echo Music" to search and play instantly.
- Added **Ambient Mode** accessible from the player menu, featuring landscape orientation, split-screen album art with live lyrics, and gesture-based playback controls.
- Added background music recognition via Quick Settings tile and widget — identify songs without opening the app.
- Added double-tap on album art center to play/pause.
- Added lossless music quality indicators to the player UI.
- Added a listening summary feature for statistics.
- Added **Echo Brain Mixes** dynamic row on the Home Screen — generates fully offline, personalized AI playlists from your listening history.
- Added Groq as an AI provider for lyrics translation under **Settings → AI** (supports Llama, Qwen3, Gemma2, and more). ([#534](https://github.com/EchoMusicApp/Echo-Music/pull/534) by [@nitheeshdr](https://github.com/nitheeshdr))
- Selecting Saavn (320kbps) or Lossless (Qobuz) quality now shows a notice that these streams run on Echo Music's servers, with a direct Donate button.
- Added Unison to the Service Uptime screen under Lyrics Providers.
- Added Lyrics Providers and Other Services (Apple Music API, Echo Find) to the Service Uptime settings screen.

## Echo Brain Improvements

- Re-architected Echo Brain queueing to use "Runway" batch injection (3 tracks at once) for smoother continuous playback.
- Echo Brain suggestions now queue immediately to play next instead of at the bottom of the playlist.
- Removed the 30-second delay — the queue is populated instantly from the first song.
- Fixed an interaction logging gap where songs skipped between 15s and 30s were not being tracked.
- Added an AI indicator icon in the queue for Echo Brain-suggested tracks.
- Added a "Not Interested" option in the queue menu to penalize AI recommendations.
- Integrated Echo Brain Flow Neuroengine to analyze listening habits and inject intelligent recommendations.

## Settings & Navigation

- Moved the "Supported Links" shortcut from About to **Settings → System Updates**.
- Improved Settings search to group results by page, navigate directly to the matching page, and highlight the relevant setting visually.
- Added Echo Brain options to Settings search.
- Grouped search results in settings for faster navigation.
- Redesigned Echo Brain settings page to follow Material 3 aesthetics.
- Moved the Cast button from the top of the player to the song menu, above Ambient Mode.

## Bug Fixes

- Fixed the Cast icon not appearing in the Apple Music theme. ([#533](https://github.com/EchoMusicApp/Echo-Music/pull/533) by [@nitheeshdr](https://github.com/nitheeshdr))
- Fixed Canvas animations and Artist Backgrounds stopping by implementing dynamic Apple Music token extraction.
- Fixed an issue where playing a song from the Suggestions tab would occasionally play a different song due to incorrect compound artist name matching.
- Fixed a crash in music recognition (Shazam) caused by invalid timeout parameters.
- Fixed a rendering issue in the mini-player where invalid crossfade options prevented the thumbnail from displaying.
- Fixed stream mismatch issues where changing audio quality mid-playback could play the wrong track. Audio quality changes now apply cleanly from the next track.
- Fixed network buffering timeouts that caused playback to enter an endless loading loop instead of showing a recovery error.
- Fixed missing album art for downloaded tracks saved without a thumbnail link.
- Fixed missing artwork for local media files on Android 10 and newer due to scoped storage changes.
- Fixed high-resolution album artwork from JioSaavn not mapping correctly to the player and database.
- Fixed a crash (FileNotFoundException) when attempting to play or crossfade into local media files that have been deleted.
- Fixed crashes related to ExoPlayer playback stats reporting and `ForegroundServiceStartNotAllowedException`.
- Fixed an issue where the skeleton/shimmer loading structure was incorrectly fading out and cutting off content.
- Fixed a bug where all user stats, song history, and downloaded songs were erased on app update due to accidental database deletion on startup.
- Fixed Backup and Restore failing with a "backup is from a newer app version" error despite being from the same version.
- Fixed a black screen crash during large local media imports caused by heavy memory usage and synchronous sorting on the main thread.
- Removed debug `println` statements from release builds and hardened thread safety in Listen Together (replacing mutable maps with `ConcurrentHashMap`, marking session fields as `@Volatile`, and improving Spotify auth security). ([#507](https://github.com/EchoMusicApp/Echo-Music/pull/507) by [@devmustafashf1](https://github.com/devmustafashf1))

## Stability & Performance

- Improved Backup and Restore by streaming large files efficiently and moving operations to background threads, preventing OOM crashes and UI freezes.
- Improved Listen Together synchronization by tracking network latency via automatic ping/pong requests.
- Re-enabled Lossless quality options with improved stream resolution fallback logic.
- Improved track matching to correctly handle songs with multiple or featured artists.
- Bumped internal database version with migration to preserve user data.
- Cleaned up the Home screen by removing Uploaded and Podcast chips from the UI.