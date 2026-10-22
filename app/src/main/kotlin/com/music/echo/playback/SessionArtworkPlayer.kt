package echo.music.iad1tya.playback

import android.content.Context
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import echo.music.iad1tya.utils.AutomotiveArtwork.withAutomotiveArtworkUri

/**
 * The player exposed through the [androidx.media3.session.MediaSession].
 *
 * Android Auto and the instrument cluster read the now-playing cover from the
 * legacy session metadata (album-art / display-icon URI plus an optional
 * bitmap), which Media3 builds from [getMediaMetadata]. The items carry
 * `https://` cover URLs, which those surfaces do not load, so the cover showed
 * up only where the bitmap happened to be used. This swaps the URI for the
 * local [echo.music.iad1tya.utils.AutomotiveArtworkProvider] one at the
 * session boundary only: the app UI keeps using the ExoPlayer directly and is
 * unaffected.
 */
class SessionArtworkPlayer(
  private val context: Context,
  player: Player,
) : ForwardingPlayer(player) {

  override fun getMediaMetadata(): MediaMetadata =
    super.getMediaMetadata().withAutomotiveArtworkUri(context)

  override fun getCurrentMediaItem(): MediaItem? =
    super.getCurrentMediaItem()?.withAutomotiveArtworkUri(context)
}
