# Echo Music v5.2.0

- Added double-tap to play/pause by tapping the center of the album art.

- Fixed an issue where Canvas animations and Artist Backgrounds stopped working by implementing dynamic Apple Music token extraction.
- Fixed an issue where playing a song from the Suggestions tab would occasionally play a different song due to incorrect compound artist name matching.
- Added Lyrics Providers and Other Services (Apple Music API, Echo Find) to the Service Uptime settings screen.
- Improved track matching in the player to correctly handle songs with multiple artists or featured artists.
- Added "Ambient Mode" accessible from the player menu, featuring landscape orientation, split-screen album art and live lyrics, and gesture-based playback controls.
- Added visible lossless music quality indicators to the player UI.
- Fixed an issue where the skeleton loading structure (shimmer effect) was incorrectly fading out and cutting off content across all screens.
- Removed the Podcasts chip from the Home screen and the Uploaded playlist chip from the Library to streamline the UI.
- Added background music recognition via Quick Settings tile and widget, allowing songs to be identified without opening the app.
- Bumped internal database version and added migration to preserve user data.
- Improved Listen Together synchronization by tracking network latency (RTT) via automatic ping/pong requests.
- Fixed an issue where network buffering timeouts caused playback to enter an endless loading loop instead of showing a recovery error state.
- Fixed a crash in music recognition (Shazam) caused by invalid timeout parameters.
- Fixed a rendering issue in the mini-player where invalid crossfade options prevented the thumbnail from displaying correctly.
- Re-enabled Lossless quality options with improved stream resolution fallback logic.
- Fixed stream mismatch issues where changing audio quality mid-playback could play the wrong track or carry over stale audio streams. Audio quality changes now apply seamlessly starting from the next track.
- Fixed missing album art for downloaded tracks that were saved without a thumbnail link.
- Fixed an issue where high-resolution album artwork from JioSaavn streams was not correctly mapped to the player and database.
- Fixed missing artwork for local media files on Android 10 and newer due to scoped storage changes.
- Fixed network buffering timeouts during playback of completely downloaded tracks.
- Fixed a crash (FileNotFoundException) that occurred when attempting to play or crossfade into local media files that have been deleted from the device.
- Added Echo Brain Flow Neuroengine integration to analyze listening habits and inject intelligent recommendations.
- Cleaned up the Home screen by removing Uploaded and Podcast chips.
- Fixed an issue with the skeleton loading structure displaying incorrectly.

### Introducing Echo Brain (Beta)
Echo Brain is our brand-new active learning engine that makes your music queue incredibly smart. Here is how it works:
- **Real-Time Active Learning**: It silently monitors your listening behavior. If you skip a track quickly (before 15 seconds), it logs a Negative Signal. If you listen for more than 30 seconds, it considers you engaged.
- **Dynamic Queue Injection**: Once engaged, Echo Brain fetches tailored recommendations and uses the Flow Neuroengine to rank them based on your exact mood. It seamlessly injects the best-matching song into your queue without you lifting a finger.
- **Algorithm Insights**: Check out the beautiful new Material You settings page under **Settings > Echo Brain (Beta)**. You can view exactly what the engine has learned about you with comprehensive Algorithm Insights.
- **Silent Fallbacks**: Handled edge cases perfectly! If a stream changes its container format mid-song due to fallback (e.g., buffering/quality drops), the engine now silently clears the song cache and automatically restarts playback at your selected quality, totally eliminating intrusive error popups.
