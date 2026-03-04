package iad1tya.echo.music.pip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import com.google.common.util.concurrent.MoreExecutors
import iad1tya.echo.music.playback.MusicService

class PipActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_PREVIOUS = "iad1tya.echo.music.pip.ACTION_PREVIOUS"
        const val ACTION_PLAY = "iad1tya.echo.music.pip.ACTION_PLAY"
        const val ACTION_PAUSE = "iad1tya.echo.music.pip.ACTION_PAUSE"
        const val ACTION_NEXT = "iad1tya.echo.music.pip.ACTION_NEXT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                when (intent.action) {
                    ACTION_PREVIOUS -> controller.seekToPreviousMediaItem()
                    ACTION_PLAY -> controller.play()
                    ACTION_PAUSE -> controller.pause()
                    ACTION_NEXT -> controller.seekToNextMediaItem()
                }
            } catch (_: Exception) {}
        }, MoreExecutors.directExecutor())
    }
}
