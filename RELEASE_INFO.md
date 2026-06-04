# Echo Music v5.1.6 - Beta Release

- Migrated default Listen Together server to Echo Music Server on Hugging Face (wss://iad1tya-echomusic.hf.space/ws). To modify or verify the URL, navigate to Listen Together > Settings > Server URL.
- Configured single Universal GMS release APK outputs (Echo-5.1.6-Universal.apk) and streamlined update checker logic.
- Removed the Local Download (Beta) feature completely.
- Replaced Qobuz with JioSaavn (320kbps) for streaming and downloading.
- Enabled Crossfade support specifically for Saavn (320kbps) streams.
- Fixed random playback pauses by clearing ghost cache entries and removing unnecessary retry delays.
- Removed FLAC codec information display (sample rate, bit depth) from the Now Playing screen.
- Crossfade is now automatically disabled when Qobuz (Lossless) audio quality is selected; the toggle becomes unclickable with an explanatory message.
- Added a new setting in Appearance Settings to show audio codec information centrally below the player timeline.
- Added an option in Appearance Settings to hide the volume slider, specifically available when the Apple Music Inspired UI is active.
- Updated the audio codec display to show codec name, bitrate in kbps, and lossless indicator.
- The playback queue is now unlocked for rearrangement by default.
- The Quick Picks carousel is now always pinned to the top of the Home Screen.
