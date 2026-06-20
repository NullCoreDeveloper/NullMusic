package iad1tya.echo.music.engine

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import iad1tya.echo.music.data.EchoBrainRepository
import iad1tya.echo.music.extensions.metadata
import iad1tya.echo.music.extensions.toMediaItem
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.models.QueueItemSource
import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import iad1tya.echo.music.engine.brain.FlowNeuroEngine
import iad1tya.echo.music.engine.brain.InteractionType
import iad1tya.echo.music.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EchoBrainEngine @Inject constructor(
    private val repository: EchoBrainRepository,
    val neuroEngine: FlowNeuroEngine
) {
    private var trackingJob: Job? = null
    private var currentTrackId: String? = null
    private var currentTrackMeta: MediaMetadata? = null
    private var trackStartTime: Long = 0
    private var hasTriggeredAiQueue = false
    
    val isEnabled = MutableStateFlow(true) // This will be bound to datastore
    
    private var playerConnection: PlayerConnection? = null
    private var engineScope: CoroutineScope? = null

    fun initialize(connection: PlayerConnection, scope: CoroutineScope) {
        if (playerConnection != null) return
        playerConnection = connection
        engineScope = scope
        
        scope.launch {
            connection.playbackState.collectLatest { state ->
                handlePlaybackState(state)
            }
        }
        scope.launch {
            connection.currentMediaItemIndex.collectLatest { index ->
                handleMediaItemTransition(connection.player.currentMediaItem)
            }
        }
    }

    private fun handlePlaybackState(state: Int) {
        val conn = playerConnection ?: return
        if (!isEnabled.value) return
        
        when (state) {
            Player.STATE_READY -> {
                if (conn.player.playWhenReady && trackingJob?.isActive != true) {
                    startTracking()
                }
            }
            else -> trackingJob?.cancel()
        }
    }

    private fun handleMediaItemTransition(mediaItem: MediaItem?) {
        val conn = playerConnection ?: return
        val scope = engineScope ?: return
        
        // Log previous track if we were tracking it
        currentTrackId?.let { trackId ->
            val durationPlayed = System.currentTimeMillis() - trackStartTime
            val skipped = durationPlayed < 15000L
            val engaged = durationPlayed >= 30000L
            
            val trackMeta = currentTrackMeta
            
            scope.launch {
                repository.logPlayEvent(trackId, trackStartTime, durationPlayed, skipped, engaged)
                if (skipped) {
                    repository.logActivity("Negative Signal", "Skipped $trackId before 15s")
                }
                
                trackMeta?.let {
                    if (engaged) {
                        neuroEngine.onMediaMetadataInteraction(it, InteractionType.WATCHED, 1.0f)
                    } else if (skipped) {
                        neuroEngine.onMediaMetadataInteraction(it, InteractionType.SKIPPED, 0.1f)
                    }
                }
            }
        }

        // Reset for new track
        trackingJob?.cancel()
        hasTriggeredAiQueue = false
        val newTrackId = mediaItem?.metadata?.id
        currentTrackId = newTrackId
        currentTrackMeta = mediaItem?.metadata
        
        if (newTrackId != null && conn.player.playWhenReady) {
            startTracking()
        }
    }

    private fun startTracking() {
        val scope = engineScope ?: return
        trackStartTime = System.currentTimeMillis()
        trackingJob = scope.launch {
            // Wait 30 seconds before considering the track 'engaged' and fetching suggestions
            delay(30000)
            
            currentTrackId?.let { trackId ->
                if (!hasTriggeredAiQueue) {
                    hasTriggeredAiQueue = true
                    fetchAndInjectSuggestions(trackId)
                }
            }
        }
    }

    private fun fetchAndInjectSuggestions(trackId: String) {
        val scope = engineScope ?: return
        val conn = playerConnection ?: return
        scope.launch {
            repository.logActivity("Analyzing", "Analyzing track $trackId for suggestions")
            
            val nextResult = YouTube.next(WatchEndpoint(videoId = trackId)).getOrNull()
            val candidates = nextResult?.items?.mapNotNull { it.toMediaItem().metadata } ?: emptyList()
            
            if (candidates.isNotEmpty()) {
                val ranked = neuroEngine.rank(candidates, emptySet())
                val suggested = ranked.firstOrNull()
                
                suggested?.let {
                    val newMeta = MediaMetadata(
                        id = it.id,
                        title = it.title,
                        artists = it.artists,
                        duration = it.duration,
                        album = it.album,
                        source = QueueItemSource.ECHO_BRAIN,
                        suggestedBy = "Echo Brain",
                        thumbnailUrl = it.thumbnailUrl
                    )
                    val newItem = newMeta.toMediaItem()
                    conn.player.addMediaItem(newItem)
                    repository.logActivity("Queued", "Added ${suggested.title} to queue")
                }
            }
        }
    }

    suspend fun getBrainSnapshot() = neuroEngine.getBrainSnapshot()

}