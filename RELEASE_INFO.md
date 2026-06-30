# Echo Music v5.2.3

**Performance**
- Improved playback startup speed by pre-warming JavaScript engine (CipherDeobfuscator) on app launch.
- Reduced ExoPlayer's initial buffer threshold to 750ms for faster audio start.
- Improved app startup time by moving Coil image cache size calculation to a non-blocking background thread.

**Bug Fixes**
- Fixed an issue where sharing YouTube Music links to Echo Music from other apps did not automatically open or play the song/playlist.
- Fixed `share.echomusic.fun` links not routing to the correct song properly.

**Playback Engine**
- Added a self-healing remote configuration system synced with zemer-cipher for seamless YouTube player signature updates without needing a new APK version.
- Integrated SponsorBlock API to automatically skip sponsorships, intros, outros, and other non-music segments during playback (can be toggled in Content Settings).
- Disabled "320 kbps" (Saavn) and "Lossless" (Qobuz) audio and download quality options due to server maintenance; existing users will be automatically migrated to "Opus" or "YouTube Music".

**ListenBrainz**
- Moved ListenBrainz settings to the Account page, making the integration fully accessible and functional.
- Fixed scrobbling by replacing fragile transition triggers with robust playback state tracking, pulling metadata directly from the ExoPlayer instance for newly streamed tracks, and adding app version headers to submission payloads.

**Discord**
- Added Discord Integration to the settings menu.
- Fixed Rich Presence not syncing by implementing automated token extraction via WebView.
- Rich Presence now updates automatically on song change, playback state changes, and setting modifications.

**Last.fm**
- Added Last.fm Integration to the Account settings menu, including scrobbling support and optional likes synchronization.

**Spotify Import**
- Added the ability to import a Spotify playlist by pasting its share link, URI, or ID, so playlists you don't own can now be imported too. ([#645](https://github.com/EchoMusicApp/Echo-Music/pull/645))

**Bug Fixes**
- Fixed local song thumbnails not displaying correctly in the player and song lists. ([#658](https://github.com/EchoMusicApp/Echo-Music/pull/658))
- Fixed a crash when launching the export directory picker on devices without a compatible document picker; the app now falls back to its default export folder. ([#663](https://github.com/EchoMusicApp/Echo-Music/pull/663))
- Fixed a database migration crash caused by a duplicate `provider` column on certain upgrade paths. ([#664](https://github.com/EchoMusicApp/Echo-Music/pull/664))