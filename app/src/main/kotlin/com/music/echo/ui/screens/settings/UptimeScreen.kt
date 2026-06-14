package iad1tya.echo.music.ui.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.R
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import androidx.compose.material3.TopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class ServiceStatus(val name: String, val url: String, var status: Status = Status.CHECKING) {
    enum class Status {
        ONLINE, OFFLINE, CHECKING
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UptimeScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val client = remember { OkHttpClient() }
    val services = remember {
        mutableStateListOf(
            ServiceStatus("YouTube Music", "https://music.youtube.com"),
            ServiceStatus("JioSaavn", "https://saavn.sumit.co/"),
            ServiceStatus("Qobuz", "https://qobuz.kennyy.com.br")
        )
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            services.forEachIndexed { index, service ->
                services[index] = service.copy(status = ServiceStatus.Status.CHECKING)
                
                val isOnline = withContext(Dispatchers.IO) {
                    try {
                        val request = Request.Builder().url(service.url).head().build()
                        client.newCall(request).execute().use { response ->
                            response.isSuccessful || response.isRedirect || response.code in 200..405
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
                services[index] = service.copy(status = if (isOnline) ServiceStatus.Status.ONLINE else ServiceStatus.Status.OFFLINE)
            }
            delay(60_000L) // 1 minute
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.service_uptime)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = LocalPlayerAwareWindowInsets.current
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(services) { service ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = service.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = service.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statusColor = when (service.status) {
                                ServiceStatus.Status.ONLINE -> Color(0xFF4CAF50)
                                ServiceStatus.Status.OFFLINE -> Color(0xFFF44336)
                                ServiceStatus.Status.CHECKING -> MaterialTheme.colorScheme.primary
                            }
                            val statusText = when (service.status) {
                                ServiceStatus.Status.ONLINE -> stringResource(R.string.status_online)
                                ServiceStatus.Status.OFFLINE -> stringResource(R.string.status_offline)
                                ServiceStatus.Status.CHECKING -> stringResource(R.string.status_checking)
                            }
                            
                            AnimatedContent(
                                targetState = service.status,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                },
                                label = "statusIcon"
                            ) { status ->
                                if (status == ServiceStatus.Status.CHECKING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = statusColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(
                                            id = if (status == ServiceStatus.Status.ONLINE) R.drawable.check else R.drawable.error
                                        ),
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelLarge,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
