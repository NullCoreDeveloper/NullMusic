package iad1tya.echo.music.utils

import android.content.Context
import android.content.res.Configuration
import timber.log.Timber
import java.util.Locale

fun reportException(throwable: Throwable) {
    Timber.e(throwable)
}

@Suppress("DEPRECATION")
fun setAppLocale(context: Context, locale: Locale) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}