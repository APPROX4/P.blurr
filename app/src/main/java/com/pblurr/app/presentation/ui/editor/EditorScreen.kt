package com.pblurr.app.presentation.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pblurr.app.domain.model.CensorEffect
import com.pblurr.app.domain.model.CensorOptions
import com.pblurr.app.domain.model.DetectionPipelineResult
import com.pblurr.app.presentation.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    result: DetectionPipelineResult,
    censorOptions: CensorOptions,
    showMaskOverlay: Boolean,
    showRawBoxes: Boolean,
    onUpdateOptions: (CensorOptions) -> Unit,
    onToggleMaskOverlay: () -> Unit,
    onToggleRawBoxes: () -> Unit,
    onOpenManualEditor: () -> Unit,
    onSaveImage: (stripExif: Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Censor Effect, 1: Detection Settings
    var showSaveModalDialog by remember { mutableStateOf(false) }

    // Save Privacy Mode Selection Modal
    if (showSaveModalDialog) {
        AlertDialog(
            onDismissRequest = { showSaveModalDialog = false },
            containerColor = Color(0xFF121215),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
            icon = {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            },
            title = {
                Text(
                    "Export Image Options",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Select how you want to export this censored image to your gallery:",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    // Option 1: Save with Privacy Mode (Remove EXIF)
                    Card(
                        onClick = {
                            showSaveModalDialog = false
                            onSaveImage(true)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Save with Privacy Mode",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Strips all hidden EXIF metadata (GPS location, date, time & camera model)",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Option 2: Save Normal (Preserve EXIF)
                    Card(
                        onClick = {
                            showSaveModalDialog = false
                            onSaveImage(false)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Save Normal (Keep Metadata)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Preserves original photo location, date, time and camera tags",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSaveModalDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
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
                        Text(
                            "Censor Editor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleMaskOverlay) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Toggle Mask Overlay",
                            tint = if (showMaskOverlay) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                    }
                    if (censorOptions.loggingEnabled) {
                        IconButton(onClick = onNavigateToDebug) {
                            Icon(Icons.Default.BugReport, contentDescription = "Diagnostics", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = { showSaveModalDialog = true },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
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
        ) {
            // Main Photo Canvas Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Display final censored image
                Image(
                    bitmap = result.censoredBitmap.asImageBitmap(),
                    contentDescription = "Censored Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Optional Mask Overlay Debug Tint
                if (showMaskOverlay) {
                    Image(
                        bitmap = result.refinedMask.asImageBitmap(),
                        contentDescription = "Mask Overlay",
                        contentScale = ContentScale.Fit,
                        alpha = 0.45f,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Stats badge overlay
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${result.originalWidth}x${result.originalHeight} | Coverage: ${"%.1f".format(result.maskCoveragePercentage)}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom Controls Bar (Luxury Dark Glassmorphism)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(DarkSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(16.dp)
            ) {
                // Tab Selection Bar & Edit Mask Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Effect & Style", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.5f)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Detection Settings", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.5f)) }
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Ultra-Premium Edit Mask Button
                    Button(
                        onClick = onOpenManualEditor,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Mask", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Control Panel Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // Effect Selector (Blur vs Pixelate)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = censorOptions.effect is CensorEffect.Blur,
                                onClick = {
                                    onUpdateOptions(censorOptions.copy(effect = CensorEffect.Blur(65)))
                                },
                                label = { Text("Soft Blur") },
                                leadingIcon = { Icon(Icons.Default.BlurOn, contentDescription = null) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.White.copy(alpha = 0.06f),
                                    labelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = censorOptions.effect is CensorEffect.Pixelate,
                                onClick = {
                                    onUpdateOptions(censorOptions.copy(effect = CensorEffect.Pixelate(18)))
                                },
                                label = { Text("Pixelate") },
                                leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.White.copy(alpha = 0.06f),
                                    labelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Intensity / Block Size Slider
                        when (val effect = censorOptions.effect) {
                            is CensorEffect.Blur -> {
                                Text(
                                    "Blur Intensity: ${effect.intensity}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Slider(
                                    value = effect.intensity.toFloat(),
                                    onValueChange = {
                                        onUpdateOptions(censorOptions.copy(effect = CensorEffect.Blur(it.toInt())))
                                    },
                                    valueRange = 1f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                            is CensorEffect.Pixelate -> {
                                Text(
                                    "Pixel Block Size: ${effect.blockSize}px",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Slider(
                                    value = effect.blockSize.toFloat(),
                                    onValueChange = {
                                        onUpdateOptions(censorOptions.copy(effect = CensorEffect.Pixelate(it.toInt())))
                                    },
                                    valueRange = 4f..60f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    } else {
                        // Mask Refinement Settings

                        Text(
                            "Mask Boundary Dilation: ${censorOptions.maskDilationPx}px",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Slider(
                            value = censorOptions.maskDilationPx.toFloat(),
                            onValueChange = {
                                onUpdateOptions(censorOptions.copy(maskDilationPx = it.toInt()))
                            },
                            valueRange = 0f..20f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    }
}
