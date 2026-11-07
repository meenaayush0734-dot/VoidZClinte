package com.voidx.client.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voidx.client.data.minecraft.MinecraftDetector
import com.voidx.client.ui.AppViewModel
import com.voidx.client.ui.components.VoidXLogo
import com.voidx.client.ui.theme.VoidBlue
import com.voidx.client.ui.theme.VoidGreen
import com.voidx.client.ui.theme.VoidGreenDeep
import com.voidx.client.ui.theme.VoidSurface
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(vm: AppViewModel) {
    val state by vm.home.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val minecraft = state.minecraft

    LaunchedEffect(Unit) { vm.refreshHome() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            VoidXLogo(size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("VoidX Client", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Bedrock companion launcher",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { vm.refreshHome() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Re-check Minecraft")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = VoidSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Minecraft Bedrock",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    minecraft == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Checking installation…")
                    }
                    minecraft.installed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = VoidGreen)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Installed · v${minecraft.versionName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            "Android can only launch the version currently installed. " +
                                "Switching versions is not supported.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Not installed", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = { openPlayStore(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Get Minecraft on Google Play") }
                    }
                }
            }
        }

        val enabled = minecraft?.installed == true && !busy
        val pulse by animateFloatAsState(
            targetValue = if (enabled) 1f else 0.97f,
            animationSpec = tween(400),
            label = "playScale",
        )
        Button(
            onClick = { vm.launchMinecraft() },
            enabled = enabled,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VoidGreen,
                contentColor = Color.Black,
                disabledContainerColor = VoidGreenDeep.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .scale(pulse),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Text("PLAY", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }

        AnimatedVisibility(
            visible = state.launchError != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    state.launchError.orEmpty(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp),
                    textAlign = TextAlign.Start,
                )
            }
        }

        // Statistics: only values the launcher actually recorded.
        Text("Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(
            colors = CardDefaults.cardColors(containerColor = VoidSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Launches", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${stats.launchCount}", style = MaterialTheme.typography.titleLarge, color = VoidBlue, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(2f)) {
                        Text("Last played", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stats.lastPlayedAt?.let { formatTime(it) } ?: "Never",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                if (stats.recentLaunches.isNotEmpty()) {
                    Text("Recent launches", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    stats.recentLaunches.reversed().take(5).forEach { ts ->
                        Text("• ${formatTime(ts)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Text("Installed versions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(
            colors = CardDefaults.cardColors(containerColor = VoidSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (minecraft?.installed == true) {
                Column(Modifier.padding(16.dp)) {
                    Text("Minecraft Bedrock ${minecraft.versionName}", fontWeight = FontWeight.Medium)
                    Text(
                        "Package ${MinecraftDetector.MINECRAFT_PACKAGE} · versionCode ${minecraft.versionCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    if (minecraft == null) "Checking…" else "No Minecraft Bedrock installation found.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(72.dp))
    }
}

private fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

private fun openPlayStore(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${MinecraftDetector.MINECRAFT_PACKAGE}"))
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${MinecraftDetector.MINECRAFT_PACKAGE}"))
        )
    }
}
