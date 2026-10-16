package echo.music.iad1tya.ui.component.musicflow

import android.os.Build
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.media3.exoplayer.offline.Download
import coil3.compose.AsyncImage
import echo.music.iad1tya.R
import echo.music.iad1tya.musicflow.MusicFlowItem
import echo.music.iad1tya.ui.component.Lyrics
import echo.music.iad1tya.ui.component.shimmer.ShimmerHost
import echo.music.iad1tya.ui.component.shimmer.TextPlaceholder
import echo.music.iad1tya.ui.screens.ambient.AmbientCanvasBackground
import echo.music.iad1tya.ui.utils.resize
import kotlin.math.roundToInt

data class MusicFlowPageActions(
  val onTogglePlay: () -> Unit,
  val onLike: () -> Unit,
  val onAddToPlaylist: () -> Unit,
  val onLyrics: () -> Unit,
  val onShare: () -> Unit,
  val onDownload: () -> Unit,
  val onMore: () -> Unit,
  val onArtist: (String) -> Unit,
  val onSeek: (Float) -> Unit,
)

@Composable
fun MusicFlowPage(
  item: MusicFlowItem,
  isCurrentPlayback: Boolean,
  isPlaying: Boolean,
  isLiked: Boolean,
  downloadState: Int?,
  showCanvas: Boolean,
  lyricsAvailable: Boolean,
  showLyrics: Boolean,
  progress: () -> Float,
  actions: MusicFlowPageActions,
  modifier: Modifier = Modifier,
) {
  val song = item.song
  val artistName = remember(song) { song.artists.joinToString(", ") { it.name } }
  val primaryArtistId = song.artists.firstOrNull { it.id != null }?.id
  val showPause = isCurrentPlayback && isPlaying
  val playPauseLabel = stringResource(if (showPause) R.string.pause else R.string.play)
  val currentActions by rememberUpdatedState(actions)
  val currentlyLiked by rememberUpdatedState(isLiked)

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) {
          detectTapGestures(
            onTap = { currentActions.onTogglePlay() },
            onDoubleTap = { if (!currentlyLiked) currentActions.onLike() },
            onLongPress = { currentActions.onMore() },
          )
        }
  ) {
    AsyncImage(
      model = song.thumbnail.resize(240, 240),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier =
        Modifier.fillMaxSize().then(
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(80.dp) else Modifier
        ),
    )
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

    Box(
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.45f),
            0.32f to Color.Transparent,
            0.58f to Color.Transparent,
            0.8f to Color.Black.copy(alpha = 0.8f),
            1f to Color.Black.copy(alpha = 0.95f),
          )
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
      val artworkSize = min(maxWidth, maxHeight * ARTWORK_MAX_HEIGHT_FRACTION)
      val lyricsHeight = maxHeight * LYRICS_HEIGHT_FRACTION
      val density = LocalDensity.current
      var artworkRegionTopPx by remember { mutableIntStateOf(0) }
      Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(TOP_BAR_SPACE))
        Spacer(Modifier.weight(1f))

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(lyricsHeight + artworkSize * (1f - ARTWORK_LYRICS_OVERLAP))
              .onPlaced { artworkRegionTopPx = it.positionInParent().y.roundToInt() }
        ) {
          Box(modifier = Modifier.align(Alignment.BottomCenter).size(artworkSize).fadedEdges()) {
            AsyncImage(
              model = song.thumbnail.resize(1200, 1200),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            if (showCanvas && isCurrentPlayback) {
              AmbientCanvasBackground(modifier = Modifier.fillMaxSize())
            }
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.align(Alignment.Center)
                  .size(72.dp)
                  .clip(CircleShape)
                  .clickable(role = Role.Button, onClickLabel = playPauseLabel, onClick = actions.onTogglePlay)
                  .semantics { contentDescription = playPauseLabel },
            ) {
              if (!showPause) {
                Icon(
                  painter = painterResource(R.drawable.play),
                  contentDescription = null,
                  tint = Color.White.copy(alpha = 0.65f),
                  modifier = Modifier.size(56.dp),
                )
              }
            }
          }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(start = CONTENT_MARGIN, end = CONTENT_MARGIN, top = 16.dp)) {
          Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().basicMarquee(initialDelayMillis = 1500, velocity = 30.dp),
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
          ) {
            Text(
              text = artistName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color.White.copy(alpha = 0.75f),
              maxLines = 1,
              modifier =
                Modifier.weight(1f).basicMarquee(initialDelayMillis = 1500, velocity = 30.dp),
            )
            if (primaryArtistId != null) {
              Spacer(Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.view_artist),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier =
                  Modifier.clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button) { actions.onArtist(primaryArtistId) }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        ) {
          FlowIconButton(
            icon = if (isLiked) R.drawable.favorite else R.drawable.favorite_border,
            label = stringResource(if (isLiked) R.string.music_flow_unlike else R.string.action_like),
            tint = if (isLiked) MaterialTheme.colorScheme.error else Color.White,
            onClick = actions.onLike,
          )
          FlowIconButton(R.drawable.lyrics, stringResource(R.string.lyrics), enabled = lyricsAvailable, onClick = actions.onLyrics)
          FlowIconButton(R.drawable.share, stringResource(R.string.share), onClick = actions.onShare)
          Spacer(Modifier.weight(1f))
          DownloadButton(downloadState, actions.onDownload)
          FlowIconButton(R.drawable.playlist_add, stringResource(R.string.add_to_playlist), onClick = actions.onAddToPlaylist)
          FlowIconButton(R.drawable.more_vert, stringResource(R.string.more_options), onClick = actions.onMore)
        }

        FlowProgress(
          progress = if (isCurrentPlayback) progress else ZeroProgress,
          seekEnabled = isCurrentPlayback,
          onSeek = actions.onSeek,
          modifier = Modifier.padding(horizontal = PROGRESS_SIDE_MARGIN),
        )

        Spacer(Modifier.weight(1f))
      }

      val lyricsBottom = with(density) { artworkRegionTopPx.toDp() } + lyricsHeight
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .padding(top = TOP_BAR_SPACE)
            .height((lyricsBottom - TOP_BAR_SPACE).coerceAtLeast(0.dp))
      ) {
        if (showLyrics) Lyrics(sliderPositionProvider = { null }, showLyrics = true, compact = true)
      }
    }
  }
}

private val TOP_BAR_SPACE = 56.dp
private val CONTENT_MARGIN = 24.dp
private val PROGRESS_SIDE_MARGIN = 36.dp
private const val ARTWORK_MAX_HEIGHT_FRACTION = 0.46f
private const val ARTWORK_LYRICS_OVERLAP = 0.15f
private const val LYRICS_HEIGHT_FRACTION = 0.2f
private const val ARTWORK_EDGE_FADE = 0.18f
private val ZeroProgress: () -> Float = { 0f }

private fun Modifier.fadedEdges(): Modifier =
  graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
      drawContent()
      val vertical = ARTWORK_EDGE_FADE * size.minDimension / size.height
      val horizontal = ARTWORK_EDGE_FADE * size.minDimension / size.width
      drawRect(
        Brush.verticalGradient(0f to Color.Transparent, vertical to Color.Black, 1f - vertical to Color.Black, 1f to Color.Transparent),
        blendMode = BlendMode.DstIn,
      )
      drawRect(
        Brush.horizontalGradient(0f to Color.Transparent, horizontal to Color.Black, 1f - horizontal to Color.Black, 1f to Color.Transparent),
        blendMode = BlendMode.DstIn,
      )
    }

@Composable
private fun DownloadButton(downloadState: Int?, onClick: () -> Unit) {
  when (downloadState) {
    Download.STATE_COMPLETED ->
      FlowIconButton(R.drawable.offline, stringResource(R.string.remove_download), onClick = onClick)
    Download.STATE_QUEUED,
    Download.STATE_DOWNLOADING -> {
      val label = stringResource(R.string.downloading)
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
      ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
      }
    }
    else -> FlowIconButton(R.drawable.download, stringResource(R.string.action_download), onClick = onClick)
  }
}

@Composable
private fun FlowProgress(
  progress: () -> Float,
  seekEnabled: Boolean,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  var dragFraction by remember { mutableStateOf<Float?>(null) }
  val currentOnSeek by rememberUpdatedState(onSeek)
  val fraction = (dragFraction ?: progress()).coerceIn(0f, 1f)
  val seekModifier =
    if (seekEnabled) {
      Modifier.pointerInput(Unit) {
          detectTapGestures { offset -> currentOnSeek((offset.x / size.width).coerceIn(0f, 1f)) }
        }
        .pointerInput(Unit) {
          detectHorizontalDragGestures(
            onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
            onDragEnd = {
              dragFraction?.let(currentOnSeek)
              dragFraction = null
            },
            onDragCancel = { dragFraction = null },
            onHorizontalDrag = { change, _ ->
              change.consume()
              dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
            },
          )
        }
    } else {
      Modifier
    }
  BoxWithConstraints(
    modifier = modifier.fillMaxWidth().height(SEEK_TOUCH_HEIGHT).then(seekModifier),
    contentAlignment = Alignment.CenterStart,
  ) {
    Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
    Box(Modifier.fillMaxWidth(fraction).height(2.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
    Box(
      Modifier.offset(x = (maxWidth - THUMB_SIZE) * fraction).size(THUMB_SIZE).clip(CircleShape).background(Color.White)
    )
  }
}

private val THUMB_SIZE = 8.dp
private val SEEK_TOUCH_HEIGHT = 24.dp

@Composable
private fun FlowIconButton(
  icon: Int,
  label: String,
  tint: Color = Color.White,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.size(48.dp)
        .clip(CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
        .semantics { contentDescription = label },
  ) {
    Icon(
      painter = painterResource(icon),
      contentDescription = null,
      tint = if (enabled) tint else tint.copy(alpha = 0.35f),
      modifier = Modifier.size(26.dp),
    )
  }
}

@Composable
fun MusicFlowPagePlaceholder(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
    ShimmerHost(modifier = Modifier.fillMaxSize()) {
      Spacer(Modifier.height(TOP_BAR_SPACE))
      TextPlaceholder(height = 28.dp, modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(0.8f))
      TextPlaceholder(height = 20.dp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp).fillMaxWidth(0.5f))
      Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp).background(MaterialTheme.colorScheme.onSurface))
      TextPlaceholder(height = 24.dp, modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(0.6f))
      TextPlaceholder(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth(0.4f))
      Spacer(Modifier.height(120.dp))
    }
  }
}
