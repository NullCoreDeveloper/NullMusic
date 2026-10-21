

package iad1tya.echo.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.media3.common.util.BitmapLoader
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class CoilBitmapLoader(
  private val context: Context,
  private val scope: CoroutineScope,
) : BitmapLoader {

  override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

  /**
   * Preferred decode size for artwork handed to clients.
   *
   * Automotive renders the same item at several sizes (full-screen card, reduced
   * player, dashboard thumbnail) and asks for the biggest one, so keeping this
   * above the largest common request avoids a re-fetch for smaller surfaces.
   */
  private val targetSize = 1024

  /**
   * Media3 updates the legacy metadata once this future completes. A stuck
   * request would leave Android Auto and the cluster on the app icon forever, so
   * fail after a bounded wait and let the caller fall back (or retry on the next
   * transition).
   */
  private val timeoutMs = 6_000L

  private fun createFallbackBitmap(): Bitmap = createBitmap(64, 64)

  private fun Bitmap.copyIfNeeded(): Bitmap {
    return if (isRecycled) {
      createFallbackBitmap()
    } else {
      try {
        copy(Bitmap.Config.ARGB_8888, false) ?: createFallbackBitmap()
      } catch (e: Exception) {
        createFallbackBitmap()
      }
    }
  }

  override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
    scope.future(Dispatchers.IO) {
      try {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        bitmap?.copyIfNeeded() ?: createFallbackBitmap()
      } catch (e: Exception) {
        Timber.tag("CoilBitmapLoader").w(e, "Failed to decode bitmap data")
        createFallbackBitmap()
      }
    }

  override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
    scope.future(Dispatchers.IO) {
      val request =
        ImageRequest.Builder(context)
          .data(uri)
          .size(targetSize)
          .allowHardware(false)
          .build()

      var result =
        withTimeoutOrNull(timeoutMs) { context.imageLoader.execute(request) }
          ?: throw IOException("Timed out loading artwork from $uri")

      if (result is ErrorResult) {
        val cacheRequest =
          request.newBuilder().networkCachePolicy(coil3.request.CachePolicy.READ_ONLY).build()
        result =
          withTimeoutOrNull(timeoutMs) { context.imageLoader.execute(cacheRequest) }
            ?: throw IOException("Timed out loading artwork from $uri")
      }

      when (result) {
        is ErrorResult -> {
          // Surface the failure as an exception instead of a blank 64x64: callers
          // (notably Media3's artwork plumbing) treat a failed future as "no
          // artwork yet" and can retry, while a blank bitmap is cached and
          // displayed as a broken cover for the rest of the session.
          throw result.throwable
        }
        is SuccessResult -> {
          try {
            val bitmap = result.image.toBitmap()
            bitmap.copyIfNeeded()
          } catch (e: Exception) {
            Timber.tag("CoilBitmapLoader").w(e, "Failed to convert image to bitmap")
            createFallbackBitmap()
          }
        }
      }
    }
}
