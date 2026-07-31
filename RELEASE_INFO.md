# NullMusic v5.2.84
The Baby Shark Incident™
Last update broke playback for `WEB_REMIX`, which meant every parent trying to play Baby Shark for the 47th time that day got hit with an HTTP 403 instead. Our inbox has never been so full, or so tired. Fixed now — the missing PoToken files are back where they belong.

- Fixed: Missing asset files (`po_token.html` + JS solvers) that broke PoToken generation and caused 403 errors on `WEB_REMIX`. Baby Shark do-do-do-do-do-do... plays again.
- Updated YouTube client definitions (TVHTML5_SIMPLY, VISIONOS, ANDROID_VR_1_65) and fallback validation rules.
- Added stream client tracking in Song Info, refreshed PoToken session handling, and enabled VISIONOS as a fallback client — fewer playback hiccups overall.

Tablet Player, Reimagined
- Trimmed the width of the Settings dialog and Mini Player on tablets — no more oversized floating panels.
- Removed the redundant mini album art on the tablet landscape player (one album art is enough, we promise).
- Redesigned the tablet landscape player into a split-screen layout, Apple Music-style — properly proportioned player, plus lyrics on screen.
- Switched the tablet landscape player to a vertical, center-aligned layout for a cleaner feel.