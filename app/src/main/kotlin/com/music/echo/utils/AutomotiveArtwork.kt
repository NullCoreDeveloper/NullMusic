package echo.music.iad1tya.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaConstants
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Artwork delivery for Android Auto / Automotive OS browse trees.
 *
 * The automotive client renders covers at very different sizes depending on the
 * surface: a one-third-width row, an expanded player taking most of the screen,
 * and the dashboard thumbnail. Media3 hands the same [MediaItem] to all of them,
 * and the item only carried an HTTP `artworkUri`. Whether a cover appeared then
 * depended on whether the app happened to be alive and Coil's cache happened to
 * be warm, which is why covers only showed up once the app had been opened and
 * behaved erratically between surfaces.
 *
 * This resolves the artwork while serving the browse request and embeds it as
 * compressed [MediaMetadata.artworkData], which is the artwork payload Media3
 * 1.7.x forwards to controllers, so every automotive surface gets the same
 * image without re-fetching it.
 */
object AutomotiveArtwork {

  private const val TAG = "AutomotiveArtwork"

  /** Largest surface (expanded player); keeps the IPC payload well under the limit. */
  private const val ARTWORK_SIZE_PX = 512

  /** Keep onGetChildren responsive: after this the item is served without art. */
  private const val LOAD_TIMEOUT_MS = 3000L

  private const val JPEG_QUALITY = 85

  /**
   * Attaches artwork to [item] when [artUrl] resolves within [LOAD_TIMEOUT_MS].
   * Never throws and never fails a browse request: artwork is cosmetic.
   */
  suspend fun attach(context: Context, item: MediaItem, artUrl: String?): MediaItem {
    val url = artUrl?.takeIf { it.isNotBlank() } ?: return item
    if (hasArtwork(item)) return item
    val data = loadArtworkData(context, url) ?: return item
    return withArtworkData(item, data)
  }

  /** Batch variant sharing one timeout across all items of a browse page. */
  suspend fun attachAll(context: Context, items: List<MediaItem>, artUrls: List<String?>): List<MediaItem> {
    if (items.isEmpty()) return items
    val pending = items.indices.filter { i ->
      artUrls.getOrNull(i)?.isNotBlank() == true && !hasArtwork(items[i])
    }
    if (pending.isEmpty()) return items
    val loaded =
      coroutineScope {
        pending.map { index ->
          async(Dispatchers.IO) { loadArtworkData(context, artUrls[index].orEmpty()) }
        }.map { it.await() }
      }
    val result = items.toMutableList()
    pending.forEachIndexed { slot, index ->
      val data = loaded.getOrNull(slot) ?: return@forEachIndexed
      result[index] = withArtworkData(result[index], data)
    }
    return result
  }

  /** Size the controller asked for, defaulting to a sensible cover size. */
  fun requestedArtSize(params: androidx.media3.session.MediaLibraryService.LibraryParams?): Size {
    val hint =
      params?.extras?.let {
        if (it.containsKey(MediaConstants.EXTRAS_KEY_MEDIA_ART_SIZE_PIXELS)) {
          it.getInt(MediaConstants.EXTRAS_KEY_MEDIA_ART_SIZE_PIXELS)
        } else {
          null
        }
      }
    return if (hint != null && hint > 0) Size(hint, hint) else Size(ARTWORK_SIZE_PX, ARTWORK_SIZE_PX)
  }

  private fun hasArtwork(item: MediaItem): Boolean = item.mediaMetadata.artworkData != null

  private fun withArtworkData(item: MediaItem, data: ByteArray): MediaItem =
    item.buildUpon()
      .setMediaMetadata(
        item.mediaMetadata.buildUpon()
          .setArtworkData(data)
          .build()
      )
      .build()

  private suspend fun loadArtworkData(context: Context, artUrl: String): ByteArray? =
    withTimeoutOrNull(LOAD_TIMEOUT_MS) {
      try {
        val request =
          ImageRequest.Builder(context)
            .data(Uri.parse(artUrl))
            .size(ARTWORK_SIZE_PX, ARTWORK_SIZE_PX)
            .allowHardware(false)
            .build()
        var result = context.imageLoader.execute(request)
        if (result is ErrorResult) {
          // Retry from Coil's disk cache only: never hold a browse response on the network.
          result = context.imageLoader.execute(request.newBuilder().networkCachePolicy(CachePolicy.READ_ONLY).build())
        }
        when (result) {
          is SuccessResult -> compress(result.image.toBitmap())
          is ErrorResult -> null
        }
      } catch (e: Exception) {
        Timber.tag(TAG).w(e, "Artwork load failed")
        null
      }
    }

  private fun compress(bitmap: Bitmap): ByteArray? =
    try {
      ByteArrayOutputStream().use { out ->
        if (bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
          out.toByteArray()
        } else {
          null
        }
      }
    } catch (e: Exception) {
      Timber.tag(TAG).w(e, "Artwork compression failed")
      null
    }
}