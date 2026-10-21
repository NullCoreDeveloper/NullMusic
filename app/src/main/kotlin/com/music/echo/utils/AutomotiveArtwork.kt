package echo.music.iad1tya.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
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

  /** Upper bound for all artwork loads of one browse response. */
  private const val BATCH_TIMEOUT_MS = 4000L

  /**
   * Now-playing artwork is not on a browse-response critical path: the player can
   * keep streaming while the cover resolves and update the metadata afterwards.
   * A cold cover fetch can easily exceed the browse budget, so allow a bit more.
   */
  private const val PLAYBACK_LOAD_TIMEOUT_MS = 8000L

  private const val JPEG_QUALITY = 80

  /**
   * Browsers may request a whole library in one call (`pageSize = Int.MAX_VALUE`).
   * Each cover is a load plus a JPEG encode, so an unbounded batch can exhaust the
   * app process on a large library, and the resulting parcel can exceed Media3's
   * 256 KiB browse payload limit, which silently truncates the list.
   */
  private const val MAX_ITEMS_PER_RESPONSE = 60

  /** Total embedded artwork bytes per browse response; keeps the parcel bounded. */
  private const val MAX_TOTAL_BYTES = 160 * 1024

  /** Concurrent decodes, so a cold cache does not fire hundreds of requests at once. */
  private const val MAX_CONCURRENT_LOADS = 4

  /**
   * Decoded-and-compressed covers keyed by source URL. The dashboard/cockpit and
   * the phone UI request the same tracks independently; without this, each
   * surface re-decodes from scratch and a surface that arrives with a cold Coil
   * cache (or after the [LOAD_TIMEOUT_MS] window) got no artwork at all. Once any
   * surface resolves a cover, every later request is served from memory.
   */
  private const val MAX_CACHE_BYTES = 8 * 1024 * 1024

  private val artworkCache =
    object : LruCache<String, ByteArray>(MAX_CACHE_BYTES) {
      override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

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
    // Bounded batch: cap how many items are touched and how many bytes end up in
    // the parcel, so a huge unpaginated request cannot stall or truncate browsing.
    val budgeted = pending.take(MAX_ITEMS_PER_RESPONSE)
    val loaded = arrayOfNulls<ByteArray>(budgeted.size)
    // Each chunk can take up to LOAD_TIMEOUT_MS, so bound the whole batch too or a
    // large page keeps the browse response pending for tens of seconds. Covers that
    // resolved before the deadline are kept; the rest are served URI-only.
    withTimeoutOrNull(BATCH_TIMEOUT_MS) {
      coroutineScope {
        budgeted.indices
          .chunked(MAX_CONCURRENT_LOADS)
          // Await each chunk before starting the next: collecting every chunk's
          // deferreds first would start all loads at once and defeat the cap.
          .forEach { chunk ->
            chunk
              .map { slot ->
                async(Dispatchers.IO) {
                  loaded[slot] = loadArtworkData(context, artUrls[budgeted[slot]].orEmpty())
                }
              }
              .awaitAll()
          }
      }
    }
    val result = items.toMutableList()
    var spent = 0
    budgeted.forEachIndexed { slot, index ->
      val data = loaded[slot] ?: return@forEachIndexed
      if (spent + data.size > MAX_TOTAL_BYTES) return@forEachIndexed
      spent += data.size
      result[index] = withArtworkData(result[index], data)
    }
    return result
  }

  /**
   * Resolves the compressed cover for the item that is currently playing.
   *
   * Unlike [attach] this is not bounded by the browse-response budget: the caller
   * updates the player metadata when the bytes are ready. The result is cached in
   * the same [artworkCache] used by the browse tree, so browsing a list and then
   * playing one of its items usually resolves without another network request.
   *
   * Returns null when the URL is blank, the load fails, or it exceeds
   * [PLAYBACK_LOAD_TIMEOUT_MS]; callers keep the item unchanged in that case.
   */
  suspend fun loadForPlayback(context: Context, artUrl: String?): ByteArray? {
    val url = artUrl?.takeIf { it.isNotBlank() } ?: return null
    return loadArtworkData(context, url, PLAYBACK_LOAD_TIMEOUT_MS)
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

  /**
   * Loads and compresses the cover at [artUrl], consulting the in-memory
   * [artworkCache] first and storing the result on success. Returns null on
   * timeout or failure; callers treat artwork as cosmetic and never fail because
   * of it.
   */
  private suspend fun loadArtworkData(
    context: Context,
    artUrl: String,
    timeoutMs: Long = LOAD_TIMEOUT_MS,
  ): ByteArray? {
    if (artUrl.isBlank()) return null
    artworkCache.get(artUrl)?.let { return it }
    val data =
      withTimeoutOrNull(timeoutMs) {
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
            // Bitmap.compress can take a long time; keep it off the caller's
            // thread (the playback path calls this from Dispatchers.Main).
            is SuccessResult -> withContext(Dispatchers.Default) { compress(result.image.toBitmap()) }
            is ErrorResult -> null
          }
        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "Artwork load failed")
          null
        }
      } ?: return null
    artworkCache.put(artUrl, data)
    return data
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