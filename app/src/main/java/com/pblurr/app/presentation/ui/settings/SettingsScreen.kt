package com.pblurr.app.presentation.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pblurr.app.domain.model.CensorOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    censorOptions: CensorOptions,
    onUpdateOptions: (CensorOptions) -> Unit,
    onResetSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Diagnostics Section
            SettingsSectionHeader(icon = Icons.Default.BugReport, title = "Diagnostics & System Logs")

            Card(
                onClick = onNavigateToDebug,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121215)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Open Live Log",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "View pipeline latency & copy diagnostic system logs",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 4. App Owner & Developer Information (With Pure Text Shadow Glow)
            SettingsSectionHeader(icon = Icons.Default.Person, title = "App Developer Information")

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121215)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "App Owner & Developer",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(4.dp))
                        GlowingApproxText()
                    }
                }
            }

            // 5. Reset to Defaults Button
            OutlinedButton(
                onClick = onResetSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF6B6B).copy(alpha = 0.5f)))
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reset All Settings to Defaults", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/**
 * Pure Glyph Text Glow for APPROX:
 * - Slow, ultra-smooth breathing animation (2500ms)
 * - Text stays 100% stationary (no scale/movement)
 * - Pure 360-degree text shadow glow with zero box background artifacts
 */
@Composable
fun GlowingApproxText(
    modifier: Modifier = Modifier,
    fontSize: Float = 17f
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val infiniteTransition = rememberInfiniteTransition(label = "ApproxGlowTransition")

    // Slow, elegant pulse cycle (2500ms)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaGlow"
    )
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadiusGlow"
    )

    Text(
        text = "APPROX",
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = Color.White,
        style = TextStyle(
            shadow = Shadow(
                color = Color.White.copy(alpha = glowAlpha * 0.95f),
                offset = Offset.Zero,
                blurRadius = glowRadius
            )
        ),
        modifier = modifier.clickable {
            try {
                uriHandler.openUri("https://github.com/APPROX4")
            } catch (_: Exception) {}
        }
    )
}
