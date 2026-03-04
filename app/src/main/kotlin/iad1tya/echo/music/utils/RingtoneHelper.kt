package iad1tya.echo.music.utils

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@UnstableApi
object RingtoneHelper {
    private const val TAG = "RingtoneHelper"

    suspend fun setAsRingtone(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        downloadCache: SimpleCache,
        playerCache: SimpleCache,
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Check WRITE_SETTINGS permission
                if (!Settings.System.canWrite(context)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Please grant 'Modify system settings' permission", Toast.LENGTH_LONG).show()
                    }
                    // Open settings to grant permission
                    val intent = android.content.Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return@withContext
                }

                // Find cached audio file
                val audioData = getCachedAudioData(songId, downloadCache, playerCache)
                if (audioData == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Song not cached. Play the song first, then try again.", Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }

                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim()
                val fileName = "${sanitizedTitle}_ringtone.mp3"

                val ringtoneUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveRingtoneQ(context, audioData, fileName, title, artist)
                } else {
                    saveRingtoneLegacy(context, audioData, fileName, title, artist)
                }

                if (ringtoneUri != null) {
                    RingtoneManager.setActualDefaultRingtoneUri(
                        context,
                        RingtoneManager.TYPE_RINGTONE,
                        ringtoneUri
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "\"$title\" set as ringtone", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save ringtone", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting ringtone: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getCachedAudioData(
        songId: String,
        downloadCache: SimpleCache,
        playerCache: SimpleCache,
    ): ByteArray? {
        // Try download cache first, then player cache
        for (cache in listOf(downloadCache, playerCache)) {
            try {
                val cacheSpan = cache.getCachedSpans(songId)
                if (cacheSpan.isNotEmpty()) {
                    val sortedSpans = cacheSpan.sortedBy { it.position }
                    val totalSize = sortedSpans.sumOf { it.length.toInt() }
                    if (totalSize <= 0) continue
                    
                    val data = ByteArray(totalSize)
                    var offset = 0
                    for (span in sortedSpans) {
                        val file = span.file ?: continue
                        val bytes = file.readBytes()
                        System.arraycopy(bytes, 0, data, offset, bytes.size)
                        offset += bytes.size
                    }
                    if (offset > 0) return data
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading cache for $songId: ${e.message}")
            }
        }
        return null
    }

    private fun saveRingtoneQ(
        context: Context,
        audioData: ByteArray,
        fileName: String,
        title: String,
        artist: String,
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.IS_RINGTONE, true)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
            put(MediaStore.Audio.Media.IS_ALARM, false)
            put(MediaStore.Audio.Media.IS_MUSIC, false)
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(audioData)
            }
        }
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveRingtoneLegacy(
        context: Context,
        audioData: ByteArray,
        fileName: String,
        title: String,
        artist: String,
    ): Uri? {
        val ringtoneDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
        if (!ringtoneDir.exists()) ringtoneDir.mkdirs()
        
        val file = File(ringtoneDir, fileName)
        FileOutputStream(file).use { it.write(audioData) }

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DATA, file.absolutePath)
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.SIZE, file.length())
            put(MediaStore.Audio.Media.IS_RINGTONE, true)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
            put(MediaStore.Audio.Media.IS_ALARM, false)
            put(MediaStore.Audio.Media.IS_MUSIC, false)
        }

        return context.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }
}
