package com.music.vivi.discord

import android.app.Activity
import com.discord.socialsdk.DiscordSocialSdkInit

/**
 * Helper for Discord Social SDK initialization.
 * Only available in GMS builds that include the Discord Partner SDK.
 */
object DiscordSdkHelper {
    fun setEngineActivity(activity: Activity?) {
        try {
            DiscordSocialSdkInit.setEngineActivity(activity)
        } catch (_: Exception) {
        }
    }
}
