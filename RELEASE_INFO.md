# Echo Music v5.2.1 Changelog

## New Features
- Export playlists as CSV files directly from the playlist menu.

## Bug Fixes
- Fixed Spotify login/import failing with an HTTP 403 Rate Limit error by fetching TOTP secrets directly from raw gist content.
- Fixed a playback crash (`NumberFormatException`) when playing local music files.
- Fixed a launch crash caused by a missing Room database migration (38→39) and a malformed version name in the build config. (by [@KATIYAR48](https://github.com/KATIYAR48))
- Fixed a crash in Listen Together where broadcast receiver actions were wrapped in unnecessary coroutine scopes. (by [@nitheeshdr](https://github.com/nitheeshdr))
- Fixed karaoke and instrumental Saavn variants incorrectly matching during song lookup by applying a score penalty when the original query doesn't include those terms. (by [@mvanhorn](https://github.com/mvanhorn))
- Fixed the bottom navigation bar shaking or bouncing when switching tabs due to the background pill animation and text expansion.

## Enhancements

### Bottom Navigation
- Moved the music recognition button from the overflow menu into the main bottom navbar (between Search and Library).
- Added a Settings shortcut to the overflow menu in place of the music recognition button.
- Disabled page name labels in the bottom navbar for a cleaner look.
- Fixed the floating navigation button to use the dynamic primary color instead of a fixed pink.
- Made floating navigation toolbar items adapt to the color palette in AMOLED mode instead of being transparent.

### Playlists & UI
- Added a toast notification when a song is successfully added to a playlist.
- Added a fallback to apply a custom playlist cover locally if the YouTube upload fails (e.g. 403 error).
- Hidden the miniplayer on the Settings screen to reduce visual clutter.