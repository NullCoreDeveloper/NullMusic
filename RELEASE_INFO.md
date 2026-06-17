# Echo Music v5.2.0

- Re-enabled Lossless quality options with improved stream resolution fallback logic.
- Fixed stream mismatch issues where changing audio quality mid-playback could play the wrong track or carry over stale audio streams. Audio quality changes now apply seamlessly starting from the next track.
- Fixed missing album art for downloaded tracks that were saved without a thumbnail link.
- Fixed an issue where high-resolution album artwork from JioSaavn streams was not correctly mapped to the player and database.
- Fixed missing artwork for local media files on Android 10 and newer due to scoped storage changes.
- Fixed network buffering timeouts during playback of completely downloaded tracks.
- Fixed a crash (FileNotFoundException) that occurred when attempting to play or crossfade into local media files that have been deleted from the device.
