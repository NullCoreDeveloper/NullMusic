package iad1tya.echo.music.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.music.innertube.models.YTItem
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.ThumbnailCornerRadius

@Composable
fun SpeedDialGridItem(
  item: YTItem,
  isPinned: Boolean,
  modifier: Modifier = Modifier,
  isActive: Boolean = false,
  isPlaying: Boolean = false,
) {
  YouTubeGridItem(
    item = item,
    isActive = isActive,
    isPlaying = isPlaying,
    thumbnailRatio = 1f,
    forceCrop = true,
    modifier = modifier,
    badges = {
      if (isPinned) {
        Icon(
          painter = painterResource(R.drawable.ic_push_pin),
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  )
}
