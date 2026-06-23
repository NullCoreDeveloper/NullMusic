# Echo Music v5.2.2 Changelog

## Bug Fixes
- Fixed an unstyled background strip appearing at the bottom of the Commits and Changelog screens.
- Fixed the loading animation pill being slightly off-center before the song's total duration was fetched.
- Fixed invisible play/pause button in the mini player on dynamic backgrounds in light mode.

## Improvements

### Service Uptime Screen
- The screen now checks all YouLyPlus fallback servers if the primary one is offline, ensuring accurate status reporting.
- Improved the overall layout and added a custom offline message when JioSaavn reaches its daily limits.
- Hidden the mini player from this screen.

### Player
- Added a loading animation and "Loading" label to the codec indicator pill when the player is buffering.

### Appearance Settings
- Hidden the "Hide player thumbnail", "Thumbnail corner radius", and "Crop album art" options when Player Background Style is set to Apple Music.