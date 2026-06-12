# Echo Music v5.1.9

- Added the ability to pin playlists to the top of your library via the long-press menu.
- Added a search bar to the Settings screen to allow quick filtering of settings categories and individual sub-settings (e.g., Theme, Cache, Quality).
- Improved update downloading by running it as a background service with auto-retry and fixed notification spam.
- Removed the comments button from the Listen Together screen.
- Fixed an issue where fully downloaded songs (Opus, Saavn 320 kbps, Lossless) failed to play while offline by accurately reading file length from the cache.
- Reordered the Online Playlist Screen header to display the song count and duration above the Save/Play/Share action buttons.
- Added a temporary helper note on the Online Playlist Screen indicating that the "Create or Join" button might need to be clicked twice.
- Improved bottom sheet menu aesthetics by removing the greyish backgrounds from header items, allowing them to blend perfectly with the menu.
- Added a song suggestions section at the bottom of local playlists, allowing users to easily discover and add related songs.
* Switched from ffmpeg-kit-full to ffmpeg-kit-audio to reduce native library overhead.
* Fixed ABI filters for individual architectural builds (arm64, armeabi, x86, x86_64) to prevent them from building universal APKs.
* Removed unused aria2c native dependency from YouTube downloader to further reduce app size.