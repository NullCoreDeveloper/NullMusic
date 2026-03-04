package iad1tya.echo.music.pip

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import iad1tya.echo.music.R

@RequiresApi(Build.VERSION_CODES.O)
object PipHelper {

    fun buildPipParams(
        context: Context,
        isPlaying: Boolean,
        isVideo: Boolean = true,
    ): PictureInPictureParams {
        val aspectRatio = if (isVideo) Rational(16, 9) else Rational(1, 1)

        val actions = listOf(
            buildRemoteAction(
                context,
                PipActionReceiver.ACTION_PREVIOUS,
                R.drawable.skip_previous,
                "Previous",
                100
            ),
            if (isPlaying) {
                buildRemoteAction(
                    context,
                    PipActionReceiver.ACTION_PAUSE,
                    R.drawable.pause,
                    "Pause",
                    101
                )
            } else {
                buildRemoteAction(
                    context,
                    PipActionReceiver.ACTION_PLAY,
                    R.drawable.play,
                    "Play",
                    101
                )
            },
            buildRemoteAction(
                context,
                PipActionReceiver.ACTION_NEXT,
                R.drawable.skip_next,
                "Next",
                102
            )
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }

    private fun buildRemoteAction(
        context: Context,
        action: String,
        iconRes: Int,
        title: String,
        requestCode: Int,
    ): RemoteAction {
        val intent = Intent(context, PipActionReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(
            Icon.createWithResource(context, iconRes),
            title,
            title,
            pendingIntent
        )
    }
}
