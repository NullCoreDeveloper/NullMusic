# Echo Music v5.2.1 Changelog

## Bug Fixes
- Fixed Spotify login / import failing with a HTTP 403 API Rate Limit Exceeded error by fetching TOTP secrets directly from raw gist content.
- Fixed a playback crash (NumberFormatException) when attempting to play local music files.

## Enhancements
- Added a toast notification when a song is successfully added to a playlist.
- Added fallback to apply custom playlist cover locally if YouTube upload fails (e.g. 403 error).
- Changed the floating navigation three-dot button to use the dynamic primary color palette instead of a fixed pink color.
- Made the floating navigation toolbar items adapt to the color palette in AMOLED mode instead of being transparent.
