package iad1tya.echo.music.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import iad1tya.echo.music.data.EchoBrainRepository
import iad1tya.echo.music.engine.EchoBrainEngine
import iad1tya.echo.music.engine.brain.*
import iad1tya.echo.music.ui.component.EchoBrainBadge
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import iad1tya.echo.music.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

// ============================================================================
// 🧠 ECHO BRAIN CONTROL CENTER 
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoBrainScreen(
    navController: NavController,
    engine: EchoBrainEngine,
    repository: EchoBrainRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userBrain by remember { mutableStateOf<UserBrain?>(null) }
    var persona by remember { mutableStateOf<EchoBrainPersona?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }
    val isEnabled by engine.isEnabled.collectAsState()

    // SAF launcher: create a file to export brain JSON into
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val success = context.contentResolver.openOutputStream(uri)?.use { out ->
                engine.neuroEngine.exportBrainToStream(out)
            } ?: false
            Toast.makeText(
                context,
                if (success) "Profile exported successfully" else "Export failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // SAF launcher: pick a JSON file to import brain from
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val success = context.contentResolver.openInputStream(uri)?.use { inp ->
                engine.neuroEngine.importBrainFromStream(inp)
            } ?: false
            if (success) {
                userBrain = engine.getBrainSnapshot()
                userBrain?.let { persona = engine.neuroEngine.getPersona(it) }
            }
            Toast.makeText(
                context,
                if (success) "Profile imported successfully" else "Import failed — invalid file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Load Brain
    LaunchedEffect(Unit) {
        userBrain = engine.getBrainSnapshot()
        userBrain?.let { persona = engine.neuroEngine.getPersona(it) }
        delay(300) // Smooth entrance
        isLoaded = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NeuralNetworkBackground()
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Echo Brain",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Your neural interest map",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                userBrain = engine.getBrainSnapshot()
                                userBrain?.let { persona = engine.neuroEngine.getPersona(it) }
                            }
                        }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (userBrain == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading neural matrix...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = isLoaded,
                    enter = fadeIn() + slideInVertically { it / 6 }
                ) {
                    val brain = userBrain!!
                    val topTopics = remember(brain.topicAffinities) {
                        brain.topicAffinities.entries
                            .sortedByDescending { it.value }
                            .take(6)
                    }
                    val topArtists = remember(brain.artistScores) {
                        brain.artistScores.entries
                            .sortedByDescending { it.value }
                            .take(5)
                    }
                    val engagementLabel = remember(brain.consecutiveSkips) {
                        when {
                            brain.consecutiveSkips < 5 -> "Engaged"
                            brain.consecutiveSkips < 15 -> "Exploring"
                            brain.consecutiveSkips < 25 -> "Drifting"
                            else -> "Restless"
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        

                        
                        Material3SettingsGroup(
                            items = listOf(
                                Material3SettingsItem(
                                    title = { Text("Enable Echo Brain") },
                                    description = { Text("Automatically inject AI recommendations based on your listening habits") },
                                    trailingContent = {
                                        Switch(
                                            checked = isEnabled,
                                            onCheckedChange = { engine.isEnabled.value = it }
                                        )
                                    },
                                    onClick = { engine.isEnabled.value = !isEnabled }
                                )
                            )
                        )

                        PersonaHeroCard(brain = brain, persona = persona)

                        ControlCenterProgressCard(
                            interactions = brain.totalInteractions,
                            persona = persona ?: EchoBrainPersona.INITIATE,
                            engagementLabel = engagementLabel
                        )

                        SimpleMetricsGrid(
                            interactions = brain.totalInteractions,
                            topics = brain.topicAffinities.size,
                            channels = brain.artistScores.size,
                            blocked = brain.blockedTopics.size + brain.blockedArtists.size
                        )

                        SectionHeader(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "Interests",
                            subtitle = "Your neural interest map"
                        )
                        NeuralBubbleCloud(brain = brain)
                        Spacer(Modifier.height(8.dp))
                        
                        if (topTopics.isEmpty()) {
                            EmptyStateCard(message = "No interests yet. Keep listening to build your profile.")
                        } else {
                            CompactTopicList(topTopics)
                        }

                        SectionHeader(
                            icon = Icons.Outlined.LibraryMusic,
                            title = "Top Artists",
                            subtitle = "Your artist affinity map"
                        )
                        if (topArtists.isEmpty()) {
                            EmptyStateCard(message = "No artist affinity yet.")
                        } else {
                            CompactArtistList(topArtists)
                        }

                        SectionHeader(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "Algorithm Insights",
                            subtitle = "How the engine sees your listening habits"
                        )
                        AdvancedRadarChart(brain = brain)
                        Spacer(Modifier.height(8.dp))

                        MaintenanceSection(
                            onReset = { showResetDialog = true },
                            onExport = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                exportLauncher.launch("flow_brain_$timestamp.json")
                            },
                            onImport = {
                                importLauncher.launch(arrayOf("application/json", "text/plain"))
                            }
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = { Text("Reset Neural Profile", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will permanently delete all learned preferences, topic affinities, and artist scores. The engine will start learning from scratch.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            engine.neuroEngine.resetBrain()
                            userBrain = engine.getBrainSnapshot()
                            userBrain?.let { persona = engine.neuroEngine.getPersona(it) }
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, null, Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Erase Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================================================================
// SECTION COMPONENTS
// ============================================================================

@Composable
private fun PersonaHeroCard(
    brain: UserBrain,
    persona: EchoBrainPersona?
) {
    val displayPersona = persona ?: EchoBrainPersona.INITIATE
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Text(
                text = displayPersona.icon,
                fontSize = 140.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 30.dp)
                    .alpha(0.15f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Active Learning",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = displayPersona.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = displayPersona.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                
                Spacer(Modifier.height(24.dp))
                
                val progress = (brain.totalInteractions / 500f).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${brain.totalInteractions} interactions • Level ${(brain.totalInteractions / 100) + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlCenterProgressCard(
    interactions: Int,
    persona: EchoBrainPersona,
    engagementLabel: String
) {
    val progress = (interactions / 100f).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Profile Growth",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    engagementLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$interactions interactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    persona.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SimpleMetricsGrid(
    interactions: Int,
    topics: Int,
    channels: Int,
    blocked: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricTile(modifier = Modifier.weight(1f), title = "Interactions", value = interactions.toString())
            MetricTile(modifier = Modifier.weight(1f), title = "Genres", value = topics.toString())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricTile(modifier = Modifier.weight(1f), title = "Artists", value = channels.toString())
            MetricTile(modifier = Modifier.weight(1f), title = "Blocked", value = blocked.toString())
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CompactTopicList(topTopics: List<Map.Entry<String, Double>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        topTopics.forEach { (topic, score) ->
            SimpleRankRow(
                leading = topic.replaceFirstChar { it.uppercase() },
                trailing = String.format(Locale.US, "%.2f", score),
                progress = score.toFloat().coerceIn(0f, 1f)
            )
        }
    }
}

@Composable
private fun CompactArtistList(topArtists: List<Map.Entry<String, Double>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        topArtists.forEach { (artistId, score) ->
            SimpleRankRow(
                leading = artistId.replaceFirstChar { it.uppercase() },
                trailing = String.format(Locale.US, "%.2f", score),
                progress = score.toFloat().coerceIn(0f, 1f)
            )
        }
    }
}

@Composable
private fun SimpleRankRow(
    leading: String,
    trailing: String,
    progress: Float
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(leading, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// ANIMATED NEURAL BACKGROUND
// ============================================================================

@Composable
private fun NeuralNetworkBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "neural")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.15f)
    ) {
        val center = Offset(size.width * 0.7f, size.height * 0.2f)
        
        rotate(rotation, center) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = pulse), Color.Transparent),
                    center = center,
                    radius = size.width * 0.5f
                ),
                center = center,
                radius = size.width * 0.5f
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiaryColor.copy(alpha = pulse * 0.7f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                    radius = size.width * 0.4f
                ),
                center = Offset(size.width * 0.2f, size.height * 0.8f),
                radius = size.width * 0.4f
            )
        }
    }
}

// ============================================================================
// NEURAL BUBBLE CLOUD
// ============================================================================

private class BubbleState(
    val topic: String,
    val score: Double,
    x: Float,
    y: Float,
    val radius: Float,
    val color: Color
) {
    var x by mutableStateOf(x)
    var y by mutableStateOf(y)
    var velocityX: Float = 0f
    var velocityY: Float = 0f
}

@Composable
private fun NeuralBubbleCloud(brain: UserBrain) {
    val topics = brain.globalVector.topics.entries
        .sortedByDescending { it.value }
        .take(12)
    
    if (topics.isEmpty()) {
        EmptyStateCard("No genres tracked yet. Start listening!")
        return
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val density = LocalDensity.current
    
    val bubbles = remember(topics, density) {
        topics.mapIndexed { index, entry ->
            val colorIndex = index % 3
            val color = when (colorIndex) {
                0 -> primaryColor
                1 -> secondaryColor
                else -> tertiaryColor
            }
            val baseRadiusDp = 40f + (entry.value.toFloat() * 80f)
            val baseRadius = with(density) { baseRadiusDp.dp.toPx() }
            
            BubbleState(
                topic = entry.key,
                score = entry.value,
                x = 0f, 
                y = 0f,
                radius = baseRadius,
                color = color
            )
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val centerX = width / 2f
            val centerY = height / 2f
            
            LaunchedEffect(width, height) {
                bubbles.forEach { bubble ->
                    if (bubble.x == 0f) {
                        bubble.x = centerX + Random.nextFloat() * 100f - 50f
                        bubble.y = centerY + Random.nextFloat() * 100f - 50f
                    }
                }
            }
            
            LaunchedEffect(Unit) {
                delay(400)
                var settleCounter = 0
                while (true) {
                    val isSettled = settleCounter > 60
                    if (isSettled) {
                        delay(100)
                    }
                    
                    withFrameNanos {
                        val repulsionStrength = 1500f
                        val centerPullStrength = 0.02f
                        val damping = 0.95f
                        val maxSpeed = 3f
                        
                        for (i in bubbles.indices) {
                            val b1 = bubbles[i]
                            val dxCenter = centerX - b1.x
                            val dyCenter = centerY - b1.y
                            b1.velocityX += dxCenter * centerPullStrength * 0.1f
                            b1.velocityY += dyCenter * centerPullStrength * 0.1f
                            
                            for (j in bubbles.indices) {
                                if (i == j) continue
                                val b2 = bubbles[j]
                                val dx = b1.x - b2.x
                                val dy = b1.y - b2.y
                                val distSq = dx*dx + dy*dy
                                val minDist = b1.radius + b2.radius + 10f 
                                
                                if (distSq < minDist * minDist && distSq > 0.1f) {
                                    val dist = sqrt(distSq)
                                    val overlap = minDist - dist
                                    val force = overlap * 0.5f 
                                    
                                    val fx = (dx / dist) * force
                                    val fy = (dy / dist) * force
                                    
                                    b1.velocityX += fx
                                    b1.velocityY += fy
                                }
                            }
                        }
                        
                        var totalVelocity = 0f
                        bubbles.forEach { b ->
                            b.velocityX *= damping
                            b.velocityY *= damping
                            
                            b.velocityX = b.velocityX.coerceIn(-maxSpeed, maxSpeed)
                            b.velocityY = b.velocityY.coerceIn(-maxSpeed, maxSpeed)
                            
                            b.x += b.velocityX
                            b.y += b.velocityY
                            
                            val padding = b.radius
                            if (b.x < padding) { b.x = padding; b.velocityX *= -0.5f }
                            if (b.x > width - padding) { b.x = width - padding; b.velocityX *= -0.5f }
                            if (b.y < padding) { b.y = padding; b.velocityY *= -0.5f }
                            if (b.y > height - padding) { b.y = height - padding; b.velocityY *= -0.5f }
                            
                            totalVelocity += abs(b.velocityX) + abs(b.velocityY)
                        }
                        
                        if (totalVelocity < 0.5f) {
                            settleCounter++
                        } else {
                            settleCounter = 0
                        }
                    }
                }
            }
            
            val connectionThresholdPx = remember(density) { with(density) { 200.dp.toPx() } }

            val accessibilityDescription = remember(bubbles) {
                val topTopics = bubbles.sortedByDescending { it.score }.take(5)
                "Interest bubble visualization showing ${bubbles.size} topics. " +
                    "Top interests: ${topTopics.joinToString(", ") { it.topic }}"
            }
            
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = accessibilityDescription }
            ) {
                bubbles.forEach { bubble ->
                    drawCircle(
                        color = bubble.color.copy(alpha = 0.2f),
                        radius = bubble.radius * 1.2f,
                        center = Offset(bubble.x, bubble.y)
                    )
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                bubble.color.copy(alpha = 0.8f),
                                bubble.color.copy(alpha = 0.4f)
                            ),
                            center = Offset(bubble.x - bubble.radius * 0.3f, bubble.y - bubble.radius * 0.3f),
                            radius = bubble.radius * 1.5f
                        ),
                        radius = bubble.radius,
                        center = Offset(bubble.x, bubble.y)
                    )
                    
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = bubble.radius * 0.3f,
                        center = Offset(bubble.x - bubble.radius * 0.3f, bubble.y - bubble.radius * 0.3f)
                    )
                }
                
                for (i in bubbles.indices) {
                    for (j in i + 1 until bubbles.size) {
                        val b1 = bubbles[i]
                        val b2 = bubbles[j]
                        val dx = b1.x - b2.x
                        val dy = b1.y - b2.y
                        val dist = sqrt(dx*dx + dy*dy)
                        
                        if (dist < connectionThresholdPx) {
                            val alpha = (1f - dist / connectionThresholdPx) * 0.15f
                            drawLine(
                                color = b1.color.copy(alpha = alpha),
                                start = Offset(b1.x, b1.y),
                                end = Offset(b2.x, b2.y),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
            }
            
            bubbles.forEach { bubble ->
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (bubble.x - bubble.radius).toDp() },
                            y = with(density) { (bubble.y - bubble.radius).toDp() }
                        )
                        .size(with(density) { (bubble.radius * 2).toDp() }),
                    contentAlignment = Alignment.Center
                ) {
                    val minRadiusPx = with(density) { 40.dp.toPx() }
                    if (bubble.radius > minRadiusPx) {
                        Text(
                            bubble.topic.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = (10 + (bubble.score * 4)).sp,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                )
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// ADVANCED RADAR CHART
// ============================================================================

@Composable
private fun AdvancedRadarChart(brain: UserBrain) {
    val currentVector = getCurrentContextVector(brain)
    val personalityVector = brain.globalVector
    
    val labels = listOf("Pacing", "Complexity", "Duration", "Live", "Breadth")
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.onSurface
    
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "radar"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        val accessibilityDescription = remember(personalityVector) {
            "Radar chart showing cognitive fingerprint."
        }
        
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = accessibilityDescription }
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = minOf(size.width, size.height) / 2 * 0.85f
                val angleStep = (2 * Math.PI / labels.size).toFloat()
                
                for (i in 4 downTo 1) {
                    val ringRadius = radius * (i / 4f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.02f * i),
                                Color.Transparent
                            ),
                            center = center,
                            radius = ringRadius
                        ),
                        radius = ringRadius,
                        center = center
                    )
                    drawCircle(
                        color = surfaceColor.copy(alpha = 0.1f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 1f)
                    )
                }
                
                for (i in labels.indices) {
                    val angle = i * angleStep - (Math.PI / 2).toFloat()
                    val endPoint = Offset(
                        center.x + radius * cos(angle),
                        center.y + radius * sin(angle)
                    )
                    drawLine(
                        color = surfaceColor.copy(alpha = 0.15f),
                        start = center,
                        end = endPoint,
                        strokeWidth = 1f
                    )
                }
                
                val contextValues = listOf(
                    currentVector.pacing,
                    currentVector.complexity,
                    currentVector.duration,
                    currentVector.isLive,
                    (currentVector.topics.size / 30.0).coerceAtMost(1.0)
                ).map { it * animatedProgress }
                
                drawRadarPolygonAdvanced(
                    center = center,
                    radius = radius,
                    values = contextValues,
                    color = primaryColor,
                    angleStep = angleStep
                )
                
                val personalityValues = listOf(
                    personalityVector.pacing,
                    personalityVector.complexity,
                    personalityVector.duration,
                    personalityVector.isLive,
                    (personalityVector.topics.size / 50.0).coerceAtMost(1.0)
                ).map { it * animatedProgress }
                
                drawRadarPolygonAdvanced(
                    center = center,
                    radius = radius,
                    values = personalityValues,
                    color = tertiaryColor,
                    angleStep = angleStep
                )
                
                contextValues.forEachIndexed { i, value ->
                    val angle = i * angleStep - (Math.PI / 2).toFloat()
                    val point = Offset(
                        center.x + (radius * value.toFloat()) * cos(angle),
                        center.y + (radius * value.toFloat()) * sin(angle)
                    )
                    drawCircle(primaryColor, 6f, point)
                    drawCircle(Color.White, 3f, point)
                }
            }
            
            val density = LocalDensity.current
            val labelAngleStep = (2 * Math.PI / labels.size).toFloat()
            
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWidth = constraints.maxWidth.toFloat()
                val boxHeight = constraints.maxHeight.toFloat()
                val chartRadius = minOf(boxWidth, boxHeight) / 2 * 0.85f
                val labelRadius = chartRadius * 1.15f
                val centerX = boxWidth / 2
                val centerY = boxHeight / 2
                
                labels.forEachIndexed { index, label ->
                    val angle = index * labelAngleStep - (Math.PI / 2).toFloat()
                    val labelX = centerX + labelRadius * cos(angle)
                    val labelY = centerY + labelRadius * sin(angle)
                    
                    Surface(
                        modifier = Modifier
                            .offset(
                                x = with(density) { (labelX - 30f).toDp() },
                                y = with(density) { (labelY - 12f).toDp() }
                            ),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendChip("Now", primaryColor)
                LegendChip("Personality", tertiaryColor)
            }
        }
    }
}

private fun DrawScope.drawRadarPolygonAdvanced(
    center: Offset,
    radius: Float,
    values: List<Double>,
    color: Color,
    angleStep: Float
) {
    val path = Path()
    values.forEachIndexed { i, value ->
        val angle = i * angleStep - (Math.PI / 2).toFloat()
        val r = radius * value.toFloat()
        val point = Offset(
            center.x + r * cos(angle),
            center.y + r * sin(angle)
        )
        if (i == 0) path.moveTo(point.x, point.y)
        else path.lineTo(point.x, point.y)
    }
    path.close()
    
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.1f)),
            center = center,
            radius = radius
        )
    )
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Composable
private fun LegendChip(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ============================================================================
// MAINTENANCE SECTION
// ============================================================================

@Composable
private fun MaintenanceSection(
    onReset: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onExport() }.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Export Profile Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Save your neural profile as a JSON file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onImport() }.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FileUpload, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Import Profile Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Restore a neural profile from a JSON backup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onReset() }.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reset Neural Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Erase all learned preferences and start fresh", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Explore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun getCurrentContextVector(brain: UserBrain): ContentVector {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 6..11 -> brain.timeVectors[TimeBucket.WEEKDAY_MORNING] ?: ContentVector()
        in 12..17 -> brain.timeVectors[TimeBucket.WEEKDAY_AFTERNOON] ?: ContentVector()
        in 18..23 -> brain.timeVectors[TimeBucket.WEEKDAY_EVENING] ?: ContentVector()
        else -> brain.timeVectors[TimeBucket.WEEKDAY_NIGHT] ?: ContentVector()
    }
}
