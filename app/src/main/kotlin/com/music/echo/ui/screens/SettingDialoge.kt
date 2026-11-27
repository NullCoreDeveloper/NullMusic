package iad1tya.echo.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.innertube.utils.parseCookieString
import kotlinx.coroutines.launch
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.AccountEmailKey
import iad1tya.echo.music.constants.InnerTubeCookieKey
import iad1tya.echo.music.constants.UseLoginForBrowse
import iad1tya.echo.music.constants.YtmSyncKey
import iad1tya.echo.music.constants.AudioQualityKey
import iad1tya.echo.music.constants.AudioQuality
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDialoge(
  onDismissRequest: () -> Unit,
  onNavigate: (String) -> Unit,
  homeViewModel: HomeViewModel,
) {
  val uriHandler = LocalUriHandler.current
  val (audioQuality) = rememberEnumPreference(AudioQualityKey, defaultValue = AudioQuality.OPUS)
  val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")
  val isLoggedIn =
    remember(innerTubeCookie) {
      innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }

  val (accountEmail, _) = rememberPreference(AccountEmailKey, "")
  val accountName by homeViewModel.accountName.collectAsState()
  val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

  val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
  val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)
  val (dismissDesktopBanner, onDismissDesktopBannerChange) =
    rememberPreference(DismissDesktopReleaseBannerKey, false)

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupRestoreViewModel = androidx.hilt.navigation.compose.hiltViewModel<iad1tya.echo.music.viewmodels.BackupRestoreViewModel>()

    var showYandexDialog by remember { mutableStateOf(false) }
    var isYandexImporting by remember { mutableStateOf(false) }
    var yandexUrl by remember { mutableStateOf("") }
    var yandexProgress by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val onSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header
      // Removed NullMusic text

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Spacer(modifier = Modifier.size(24.dp))
                    
                    Text(
                        text = "NullMusic",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = primaryColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(24.dp))
                }

                // Account Group
                Material3SettingsGroup(
                    title = "Account",
                    compact = true,
                    items = buildList {
                        add(
                            Material3SettingsItem(
                                title = { Text(if (isLoggedIn) accountName else "Anonymous") },
                                description = { Text(if (isLoggedIn) accountEmail.ifEmpty { "Logged In" } else "Not Logged In") },
                                icon = painterResource(R.drawable.account),
                                trailingContent = if (isLoggedIn && !accountImageUrl.isNullOrBlank()) {
                                    {
                                        AsyncImage(
                                            model = accountImageUrl,
                                            contentDescription = "Profile Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    onDismissRequest()
                                    if (isLoggedIn) onNavigate("settings/account") else onNavigate("login") 
                                }
                            )
                        )
                        add(
                            Material3SettingsItem(
                                title = { Text(androidx.compose.ui.res.stringResource(R.string.ai_lyrics_translation)) },
                                customIcon = {
                                    Text(
                                        text = "Ai",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                    )
                                },
                                onClick = {
                                    onDismissRequest()
                                    onNavigate("settings/ai")
                                }
                            )
                        )
                        if (audioQuality == AudioQuality.LOSSLESS) {
                            add(
                                Material3SettingsItem(
                                    title = { Text("Donate for lossless music") },
                                    customIcon = {
                                        Text(
                                            text = "$",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                        )
                                    },
                                    onClick = {
                                        uriHandler.openUri("https://lossless.nullmusic.fun")
                                        onDismissRequest()
                                    }
                                )
                            )
                        }
                    }
                )

                if (isLoggedIn) {
                    Material3SettingsGroup(
                        title = "Preferences",
                        compact = true,
                        items = listOf(
                            Material3SettingsItem(
                                title = { Text("Use Account for Browsing") },
                                icon = painterResource(R.drawable.add_circle),
                                trailingContent = {
                                    Switch(
                                        checked = useLoginForBrowse,
                                        onCheckedChange = {
                                            com.music.innertube.YouTube.useLoginForBrowse = it
                                            onUseLoginForBrowseChange(it)
                                        },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                },
                                onClick = {
                                    val newVal = !useLoginForBrowse
                                    com.music.innertube.YouTube.useLoginForBrowse = newVal
                                    onUseLoginForBrowseChange(newVal)
                                }
                            ),
                            Material3SettingsItem(
                                title = { Text("YouTube Music Sync") },
                                icon = painterResource(R.drawable.cached),
                                trailingContent = {
                                    Switch(
                                        checked = ytmSync,
                                        onCheckedChange = onYtmSyncChange,
                                        modifier = Modifier.scale(0.8f)
                                    )
                                },
                                onClick = { onYtmSyncChange(!ytmSync) }
                            )
                        )
                    )
                }

                Material3SettingsGroup(
                    title = "App",
                    compact = true,
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text("Settings") },
                            icon = painterResource(R.drawable.settings),
                            onClick = {
                                onDismissRequest()
                                onNavigate("settings")
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text("Import from Yandex") },
                            icon = painterResource(R.drawable.download),
                            onClick = { showYandexDialog = true }
                        ),
                        Material3SettingsItem(
                            title = { Text("About") },
                            icon = painterResource(R.drawable.info),
                            trailingContent = { Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                onDismissRequest()
                                onNavigate("settings/about")
                            }
                        )
                    )
                )

                // Footer Links
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSecondaryColor,
                        modifier = Modifier.clickable { uriHandler.openUri("https://nullmusic.fun/p/privacy-policy") }.padding(4.dp)
                    )
                    Text(text = " • ", color = onSecondaryColor, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Terms of Service",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSecondaryColor,
                        modifier = Modifier.clickable { uriHandler.openUri("https://nullmusic.fun/p/toc") }.padding(4.dp)
                    )
                }
            }
        }
    }

    if (showYandexDialog) {
        AlertDialog(
            onDismissRequest = { if (!isYandexImporting) showYandexDialog = false },
            title = { Text("Import from Yandex Music") },
            text = {
                Column {
                    if (isYandexImporting) {
                        Text("Importing... $yandexProgress")
                    } else {
                        Text("Paste the link to your public Yandex Music playlist.")
                        Spacer(modifier = Modifier.padding(8.dp))
                        OutlinedTextField(
                            value = yandexUrl,
                            onValueChange = { yandexUrl = it },
                            label = { Text("Playlist URL") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                if (!isYandexImporting) {
                    TextButton(
                        onClick = {
                            if (yandexUrl.isNotBlank()) {
                                isYandexImporting = true
                                coroutineScope.launch {
                                    backupRestoreViewModel.importFromYandexUrl(context, yandexUrl) { current, total ->
                                        yandexProgress = "$current / $total"
                                    }
                                    isYandexImporting = false
                                    showYandexDialog = false
                                    yandexUrl = ""
                                }
                            }
                        }
                    ) {
                        Text("Import")
                    }
                }
            },
            dismissButton = {
                if (!isYandexImporting) {
                    TextButton(onClick = { showYandexDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun DesktopReleaseBannerCard(
  onClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "DesktopReleaseShine")
  val shineProgress by
    infiniteTransition.animateFloat(
      initialValue = -0.6f,
      targetValue = 1.6f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2600, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "shineProgress",
    )
  val borderAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.25f,
      targetValue = 0.65f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1600, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "borderAlpha",
    )

  val primary = MaterialTheme.colorScheme.primary
  val containerBg = MaterialTheme.colorScheme.surfaceContainerHigh

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = containerBg,
    border = BorderStroke(1.dp, primary.copy(alpha = borderAlpha)),
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth().drawWithContent {
          drawContent()
          val w = size.width
          val h = size.height
          val startX = w * shineProgress
          drawRect(
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    Color.Transparent,
                    primary.copy(alpha = 0.14f),
                    Color.White.copy(alpha = 0.14f),
                    primary.copy(alpha = 0.14f),
                    Color.Transparent,
                  ),
                start = Offset(startX - w * 0.35f, 0f),
                end = Offset(startX + w * 0.35f, h),
              ),
          )
        },
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter = painterResource(R.drawable.new_release),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp),
          )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Desktop Client is Out",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "NEW",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                  ),
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
          Spacer(Modifier.height(2.dp))
          Text(
            text = "Echo Music for your desktop",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(32.dp),
        ) {
          Icon(
            painter = painterResource(R.drawable.close),
            contentDescription = "Dismiss",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}
