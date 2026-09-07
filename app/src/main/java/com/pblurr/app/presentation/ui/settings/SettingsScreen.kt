package com.pblurr.app.presentation.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pblurr.app.data.update.AppUpdateInfo
import com.pblurr.app.domain.model.CensorOptions
import com.pblurr.app.presentation.ui.VersionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    censorOptions: CensorOptions,
    installedVersion: String = "1.0.1",
    versionStatus: VersionStatus = VersionStatus.NOT_CHECKED,
    updateInfo: AppUpdateInfo? = null,
    isCheckingUpdates: Boolean = false,
    manualUpdateResult: String? = null,
    onUpdateOptions: (CensorOptions) -> Unit,
    onCheckUpdatesManually: () -> Unit = {},
    onClearManualUpdateResult: () -> Unit = {},
    onRestartApp: () -> Unit = {},
    onResetSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    var showManualCheckNoticeDialog by remember { mutableStateOf(false) }
    var showAutoUpdateNoticeDialog by remember { mutableStateOf(false) }

    // Network Notice Dialog for Manual Update Check
    if (showManualCheckNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showManualCheckNoticeDialog = false },
            containerColor = Color(0xFF121215),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
            icon = {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
            },
            title = {
                Text(
                    text = "Check for Updates",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "P.blurr will connect to GitHub (github.com) over the internet to check if a new app version is available.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualCheckNoticeDialog = false
                        onUpdateOptions(censorOptions.copy(hasAcceptedInternetNotice = true))
                        onCheckUpdatesManually()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Proceed & Check", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualCheckNoticeDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Network Notice Dialog for Toggling Auto-Update ON
    if (showAutoUpdateNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showAutoUpdateNoticeDialog = false },
            containerColor = Color(0xFF121215),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
            icon = {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
            },
            title = {
                Text(
                    text = "Network Access Notice",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Enabling auto-check will connect P.blurr to GitHub (github.com) over the internet to check for updates whenever the app launches.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAutoUpdateNoticeDialog = false
                        onUpdateOptions(censorOptions.copy(autoUpdateCheckEnabled = true, hasAcceptedInternetNotice = true))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable Auto-Check", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoUpdateNoticeDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Version Check Result Dialog
    if (manualUpdateResult != null) {
        AlertDialog(
            onDismissRequest = onClearManualUpdateResult,
            containerColor = Color(0xFF121215),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
            title = {
                Text(
                    text = "Version Check Result",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = manualUpdateResult,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                Button(
                    onClick = onClearManualUpdateResult,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(8.dp))
                        Text("Settings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                    }
                },
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Diagnostics & Logging System Section
            SettingsSectionHeader(icon = Icons.Default.BugReport, title = "Diagnostics & Logging System")

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121215)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "System Logging",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Enable or disable internal pipeline diagnostic logging. Disabling completely turns off log recording.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = censorOptions.loggingEnabled,
                            onCheckedChange = { isChecked ->
                                onUpdateOptions(censorOptions.copy(loggingEnabled = isChecked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    if (censorOptions.loggingEnabled) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDebug() }
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Open Diagnostic Log Viewer",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    "View live pipeline timings, model inference logs & copy log text",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. App Updates Section
            SettingsSectionHeader(icon = Icons.Default.SystemUpdate, title = "App Updates")

            var isDetailsExpanded by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121215)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // App Version & Status Tag Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Installed Version",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "v$installedVersion",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                GlowingVersionBadge(status = versionStatus)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Check GitHub repository for the latest release",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Auto-Check for Updates",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                "Check GitHub automatically when app opens",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = censorOptions.autoUpdateCheckEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (censorOptions.hasAcceptedInternetNotice) {
                                        onUpdateOptions(censorOptions.copy(autoUpdateCheckEnabled = true))
                                    } else {
                                        showAutoUpdateNoticeDialog = true
                                    }
                                } else {
                                    onUpdateOptions(censorOptions.copy(autoUpdateCheckEnabled = false))
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (censorOptions.hasAcceptedInternetNotice) {
                                onCheckUpdatesManually()
                            } else {
                                showManualCheckNoticeDialog = true
                            }
                        },
                        enabled = !isCheckingUpdates,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White)
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Checking GitHub...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Check for Updates Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Expandable GitHub Version Information
                    val showExpandedInfo = isDetailsExpanded || versionStatus != VersionStatus.NOT_CHECKED || updateInfo != null

                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { isDetailsExpanded = !isDetailsExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isDetailsExpanded) "Hide GitHub Version Info" else "Expand GitHub Version Info",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                if (isDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showExpandedInfo) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text("GitHub Release Information", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Installed Version:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("v$installedVersion", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Latest GitHub Release:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    if (updateInfo != null) "v${updateInfo.latestVersion}" else if (versionStatus == VersionStatus.LATEST) "v$installedVersion" else "Not Checked Yet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (versionStatus == VersionStatus.OUTDATED) Color(0xFFFF3333) else if (versionStatus == VersionStatus.LATEST) Color(0xFF32CD32) else Color.White.copy(alpha = 0.6f)
                                )
                            }

                            val notes = updateInfo?.releaseNotes
                            if (!notes.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Release Notes:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
                                Text(notes, fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f), modifier = Modifier.padding(top = 2.dp))
                            }

                            if (updateInfo != null && !updateInfo.releaseUrl.isNullOrBlank()) {
                                Spacer(Modifier.height(10.dp))
                                val uriHandler = LocalUriHandler.current
                                Button(
                                    onClick = {
                                        try {
                                            uriHandler.openUri(updateInfo.releaseUrl)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32CD32), contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Download Update (APK)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. App Owner & Developer Information
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

            // 4. Reset to Defaults Button
            OutlinedButton(
                onClick = onResetSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(0xFFFF6B6B).copy(alpha = 0.5f), Color(0xFFFF6B6B).copy(alpha = 0.3f))))
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

@Composable
fun GlowingApproxText(
    modifier: Modifier = Modifier,
    fontSize: Float = 17f
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val infiniteTransition = rememberInfiniteTransition(label = "ApproxGlowTransition")

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

@Composable
fun GlowingVersionBadge(
    status: VersionStatus
) {
    if (status == VersionStatus.NOT_CHECKED) return

    val isLatest = (status == VersionStatus.LATEST)
    val text = if (isLatest) "LATEST" else "OUTDATED"
    val color = if (isLatest) Color(0xFF32CD32) else Color(0xFFFF3333)

    val infiniteTransition = rememberInfiniteTransition(label = "BadgeGlowTransition")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BadgeGlowRadius"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BadgeGlowAlpha"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = glowAlpha * 0.85f)),
        modifier = Modifier.padding(start = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = color,
            style = TextStyle(
                shadow = Shadow(
                    color = color.copy(alpha = glowAlpha),
                    offset = Offset.Zero,
                    blurRadius = glowRadius
                )
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
