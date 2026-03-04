package iad1tya.echo.music.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import iad1tya.echo.music.R
import iad1tya.echo.music.utils.RingtoneHelper
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private const val MAX_CLIP_SEC = 30

private sealed interface RingtonePhase {
    /** Fetching audio from network / cache */
    data class Fetching(val progress: Float = 0f, val status: String = "Starting…") : RingtonePhase
    /** Audio ready — let user trim */
    data class Trimming(val audioFile: File) : RingtonePhase
    /** Transcoding + saving */
    data class Processing(val status: String = "Creating ringtone…") : RingtonePhase
    /** Error occurred */
    data class Error(val message: String) : RingtonePhase
}

/**
 * Full-flow ringtone dialog:
 *   1. Fetches the song audio (with progress bar)
 *   2. Shows trim slider (max 30 s)
 *   3. Transcodes + saves + opens system ringtone picker
 */
@UnstableApi
@Composable
fun RingtoneTrimDialog(
    songId: String,
    title: String,
    artist: String,
    duration: Int,           // seconds; 0 = unknown
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var phase by remember { mutableStateOf<RingtonePhase>(RingtonePhase.Fetching()) }

    // Kick off the download as soon as the dialog appears
    LaunchedEffect(songId) {
        phase = RingtonePhase.Fetching(0f, "Starting…")
        val file = RingtoneHelper.downloadAudio(
            context       = context,
            songId        = songId,
            downloadCache = downloadCache,
            playerCache   = playerCache,
            onProgress    = { prog, status ->
                phase = RingtonePhase.Fetching(prog, status)
            },
        )
        phase = if (file != null) RingtonePhase.Trimming(file)
                else              RingtonePhase.Error("Could not fetch audio. Check your internet connection and try again.")
    }

    // ── Slider state (only meaningful during Trimming phase) ─────────────────
    val durationSec = duration.takeIf { it > 0 } ?: 180
    val initialEnd  = MAX_CLIP_SEC.toFloat().coerceAtMost(durationSec.toFloat())
    var sliderValues by remember { mutableStateOf(0f..initialEnd) }
    var prevValues   by remember { mutableStateOf(0f..initialEnd) }

    fun formatTime(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

    fun coerceRange(new: ClosedFloatingPointRange<Float>): ClosedFloatingPointRange<Float> {
        val span = new.endInclusive - new.start
        if (span <= MAX_CLIP_SEC) return new
        val startMoved = new.start != prevValues.start
        return if (startMoved) {
            val clamped = (new.endInclusive - MAX_CLIP_SEC).coerceAtLeast(0f)
            clamped..new.endInclusive
        } else {
            val clamped = (new.start + MAX_CLIP_SEC).coerceAtMost(durationSec.toFloat())
            new.start..clamped
        }
    }

    val startSec    = sliderValues.start.roundToInt()
    val endSec      = sliderValues.endInclusive.roundToInt().coerceAtLeast(startSec + 1)
    val clipDuration = endSec - startSec
    val isValid     = clipDuration in 1..MAX_CLIP_SEC

    AlertDialog(
        onDismissRequest = {
            if (phase !is RingtonePhase.Processing) onDismiss()
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.notification),
                contentDescription = null,
            )
        },
        title = {
            Text(
                text = when (phase) {
                    is RingtonePhase.Fetching    -> "Preparing Ringtone"
                    is RingtonePhase.Trimming    -> "Set as Ringtone"
                    is RingtonePhase.Processing  -> "Creating Ringtone"
                    is RingtonePhase.Error       -> "Error"
                },
            )
        },
        text = {
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "RingtonePhase",
            ) { currentPhase ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (currentPhase) {
                        // ── Phase 1: Fetching ─────────────────────────────
                        is RingtonePhase.Fetching -> {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (artist.isNotBlank()) {
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (currentPhase.progress > 0f && currentPhase.progress < 1f) {
                                LinearProgressIndicator(
                                    progress = { currentPhase.progress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = currentPhase.status,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // ── Phase 2: Trimming ─────────────────────────────
                        is RingtonePhase.Trimming -> {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (artist.isNotBlank()) {
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Select clip (max $MAX_CLIP_SEC seconds)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            RangeSlider(
                                value = sliderValues,
                                onValueChange = { new ->
                                    val coerced = coerceRange(new)
                                    prevValues   = sliderValues
                                    sliderValues = coerced
                                },
                                valueRange = 0f..durationSec.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = formatTime(startSec),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "${clipDuration}s",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isValid) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = formatTime(endSec),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        // ── Phase 3: Processing ───────────────────────────
                        is RingtonePhase.Processing -> {
                            Spacer(Modifier.height(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = currentPhase.status,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }

                        // ── Error ─────────────────────────────────────────
                        is RingtonePhase.Error -> {
                            Text(
                                text = currentPhase.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (val p = phase) {
                is RingtonePhase.Trimming -> {
                    Button(
                        enabled = isValid,
                        onClick = {
                            scope.launch {
                                phase = RingtonePhase.Processing("Creating ringtone…")
                                val ok = RingtoneHelper.processAndSetRingtone(
                                    context   = context,
                                    audioFile = p.audioFile,
                                    title     = title,
                                    artist    = artist,
                                    startMs   = startSec * 1000L,
                                    endMs     = endSec * 1000L,
                                    onProgress = { status -> phase = RingtonePhase.Processing(status) },
                                )
                                if (ok) onDismiss()
                                else   phase = RingtonePhase.Error("Failed to create ringtone. Try again.")
                            }
                        },
                    ) { Text("Set as Ringtone") }
                }
                is RingtonePhase.Error -> {
                    Button(onClick = onDismiss) { Text("Close") }
                }
                else -> Box(Modifier)   // no confirm button during fetch / processing
            }
        },
        dismissButton = {
            if (phase !is RingtonePhase.Processing) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
