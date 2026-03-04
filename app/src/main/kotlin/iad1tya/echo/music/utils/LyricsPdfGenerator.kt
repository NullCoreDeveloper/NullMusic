package iad1tya.echo.music.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object LyricsPdfGenerator {
    private const val TAG = "LyricsPdfGenerator"

    // A4 size in PostScript points
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val HEADER_HEIGHT = 80f

    suspend fun generateAndShare(
        context: Context,
        title: String,
        artist: String,
        lyrics: String,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val document = PdfDocument()
                var pageNumber = 1
                var yOffset = 0f

                // Text paints
                val titlePaint = TextPaint().apply {
                    color = android.graphics.Color.parseColor("#7C4DFF")
                    textSize = 22f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val artistPaint = TextPaint().apply {
                    color = android.graphics.Color.parseColor("#9E9E9E")
                    textSize = 14f
                    isAntiAlias = true
                }
                val lyricsPaint = TextPaint().apply {
                    color = android.graphics.Color.parseColor("#212121")
                    textSize = 12f
                    isAntiAlias = true
                }
                val footerPaint = TextPaint().apply {
                    color = android.graphics.Color.parseColor("#BDBDBD")
                    textSize = 10f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val headerLinePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#7C4DFF")
                    strokeWidth = 2f
                }

                // Clean lyrics (strip timestamps)
                val cleanLyrics = lyrics.lines().joinToString("\n") { line ->
                    line.replace(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]\\s*"), "").trim()
                }.trim()

                // Build static layout for lyrics text
                val lyricsLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder
                        .obtain(cleanLyrics, 0, cleanLyrics.length, lyricsPaint, CONTENT_WIDTH.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(6f, 1f)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(
                        cleanLyrics,
                        lyricsPaint,
                        CONTENT_WIDTH.toInt(),
                        Layout.Alignment.ALIGN_NORMAL,
                        1f,
                        6f,
                        true,
                    )
                }

                val totalLyricsHeight = lyricsLayout.height.toFloat()
                var lyricsDrawnHeight = 0f

                // Create first page
                var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                var page = document.startPage(pageInfo)
                var canvas = page.canvas

                // Draw header on first page
                canvas.drawText(title, MARGIN, MARGIN + 24f, titlePaint)
                canvas.drawText(artist, MARGIN, MARGIN + 48f, artistPaint)
                canvas.drawLine(MARGIN, MARGIN + 60f, PAGE_WIDTH - MARGIN, MARGIN + 60f, headerLinePaint)

                yOffset = MARGIN + HEADER_HEIGHT
                val availableHeight = PAGE_HEIGHT - MARGIN - 30f // Leave room for footer

                // Draw lyrics with pagination
                canvas.save()
                canvas.translate(MARGIN, yOffset)
                canvas.clipRect(0f, 0f, CONTENT_WIDTH, availableHeight - yOffset)
                lyricsLayout.draw(canvas)
                canvas.restore()

                val firstPageLyricsSpace = availableHeight - yOffset
                lyricsDrawnHeight = firstPageLyricsSpace

                // Draw footer
                canvas.drawText(
                    "Echo Music — Page $pageNumber",
                    PAGE_WIDTH / 2f,
                    PAGE_HEIGHT - 20f,
                    footerPaint
                )
                document.finishPage(page)

                // Additional pages if lyrics overflow
                while (lyricsDrawnHeight < totalLyricsHeight) {
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas

                    val pageSpace = availableHeight - MARGIN

                    canvas.save()
                    canvas.translate(MARGIN, MARGIN)
                    canvas.clipRect(0f, 0f, CONTENT_WIDTH, pageSpace)
                    canvas.translate(0f, -lyricsDrawnHeight)
                    lyricsLayout.draw(canvas)
                    canvas.restore()

                    lyricsDrawnHeight += pageSpace

                    canvas.drawText(
                        "Echo Music — Page $pageNumber",
                        PAGE_WIDTH / 2f,
                        PAGE_HEIGHT - 20f,
                        footerPaint
                    )
                    document.finishPage(page)
                }

                // Save to file
                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim()
                val fileName = "${sanitizedTitle}_lyrics.pdf"

                val pdfUri = savePdf(context, document, fileName)
                document.close()

                if (pdfUri != null) {
                    withContext(Dispatchers.Main) {
                        // Share the PDF
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, pdfUri)
                            putExtra(Intent.EXTRA_SUBJECT, "$title - Lyrics")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share Lyrics PDF")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating PDF: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun savePdf(context: Context, document: PdfDocument, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/EchoMusic")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                contentValues
            )
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            }
            uri
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "EchoMusic"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { document.writeTo(it) }
            FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
        }
    }
}
