package iad1tya.echo.music.playback

import timber.log.Timber

object NativeAudioEngine {
    @Volatile
    private var initialized = false

    fun initializeIfAvailable(): Boolean {
        if (initialized) return true
        return try {
            // Long-term track: wire JNI entry points here when the native module is ready.
            // Keeping this guarded lets us ship the setting now without breaking playback.
            initialized = false
            false
        } catch (e: Throwable) {
            Timber.w(e, "Native audio engine init failed; falling back to standard engine")
            false
        }
    }
}
