package com.pblurr.app.presentation.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pblurr.app.R
import com.pblurr.app.presentation.ui.theme.DarkSurface
import com.pblurr.app.presentation.ui.theme.FiraSansItalicFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    censorOptions: com.pblurr.app.domain.model.CensorOptions = com.pblurr.app.domain.model.CensorOptions(),
    updateInfo: com.pblurr.app.data.update.AppUpdateInfo? = null,
    onDismissUpdate: () -> Unit = {},
    onPhotoSelected: (Uri) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    // Permission state logic (Android 13+ READ_MEDIA_IMAGES vs Android 8-12 READ_EXTERNAL_STORAGE)
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onPhotoSelected(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showPermissionDeniedDialog = false
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            showPermissionDeniedDialog = true
        }
    }

    fun handleGalleryClick() {
        val hasPermission = ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            permissionLauncher.launch(requiredPermission)
        }
    }

    // Interactive Pitch-Black White-Glowing Permission Denied Dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            containerColor = Color(0xFF0C0C0E),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.12f))
                ),
                shape = RoundedCornerShape(22.dp)
            ),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Storage Access Required",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "P.blurr requires permission to access photo files on your device so you can select and censor intimate regions.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Your photos are processed 100% locally and never leave your phone.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.50f),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        permissionLauncher.launch(requiredPermission)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPermissionDeniedDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.2f)))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Open App Settings")
                }
            }
        )
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = onDismissUpdate,
            containerColor = Color(0xFF121215),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Update Recommended",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "A new version (v${updateInfo.latestVersion}) of P.blurr is available on GitHub!",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    if (!updateInfo.releaseNotes.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = updateInfo.releaseNotes.take(150) + if (updateInfo.releaseNotes.length > 150) "..." else "",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(updateInfo.releaseUrl)
                        } catch (_: Exception) {}
                        onDismissUpdate()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Download Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpdate) {
                    Text("Dismiss", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.pblurr_logo),
                            contentDescription = "P.blurr Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "P.blurr",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FiraSansItalicFamily,
                            fontStyle = FontStyle.Italic,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    if (censorOptions.loggingEnabled) {
                        IconButton(onClick = onNavigateToDebug) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Debug Log",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Hero section with Custom Half-Clear / Half-Pixelated Blur Image Emblem
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                HalfBlurHalfOkEmblem()

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Intimate Region Censoring",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "100% On-Device AI • Pixel Precision • Zero Uploads",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Primary Action Button: Photo Picker (Luxury White Bloom Glass Card)
            Card(
                onClick = { handleGalleryClick() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Select Photo",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select Photo from Gallery",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "JPEG, PNG, WEBP & HEIC Supported",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Feature / Privacy Cards & Developer Credit
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                PrivacyFeatureItem(
                    icon = Icons.Default.Security,
                    title = "Complete On-Device Privacy",
                    description = "Image processing runs entirely offline without internet dependencies."
                )
                PrivacyFeatureItem(
                    icon = Icons.Default.Security,
                    title = "Mask-Only Censoring",
                    description = "Censors only detected intimate regions—never faces, people, or background."
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Developer / Owner Credit with Glowing APPROX Animation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "P.BLURR • DEVELOPED BY ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    com.pblurr.app.presentation.ui.settings.GlowingApproxText(fontSize = 11f)
                }
            }
        }
    }
}

/**
 * Custom Hero Emblem: Split Half-Clear / Half-Pixelated Blur Image Emblem.
 * - Left half: Sharp photo representation with "OK" clear badge.
 * - Right half: Pixel mosaic grid representation with "BLUR" censored badge.
 * - Vertical dividing line with glowing white accent.
 */
@Composable
fun HalfBlurHalfOkEmblem() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(Color(0xFF141418))
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.15f))
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midX = w / 2f

            // Left Half Path: Clear/Sharp mountain/sun artwork background
            val leftPath = Path().apply {
                addRect(Rect(0f, 0f, midX, h))
            }
            clipPath(leftPath) {
                // Clear Gradient Background
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
                // Draw sharp sun circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = w * 0.12f,
                    center = Offset(w * 0.28f, h * 0.38f)
                )
                // Draw sharp mountain triangle
                val mountainPath = Path().apply {
                    moveTo(w * 0.05f, h * 0.85f)
                    lineTo(w * 0.28f, h * 0.45f)
                    lineTo(w * 0.48f, h * 0.85f)
                    close()
                }
                drawPath(mountainPath, Color.White.copy(alpha = 0.7f))
            }

            // Right Half Path: Pixelated Mosaic Blur Grid
            val rightPath = Path().apply {
                addRect(Rect(midX, 0f, w, h))
            }
            clipPath(rightPath) {
                drawRect(Color(0xFF0F172A))

                // Render 5x5 pixel mosaic blocks on the right side
                val columns = 5
                val rows = 5
                val blockW = (w / 2f) / columns
                val blockH = h / rows

                val grayscalePalette = listOf(
                    Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF64748B),
                    Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B),
                    Color(0xFFCBD5E1), Color(0xFF64748B)
                )

                for (c in 0 until columns) {
                    for (r in 0 until rows) {
                        val colorIdx = (c * 3 + r * 7) % grayscalePalette.size
                        drawRect(
                            color = grayscalePalette[colorIdx],
                            topLeft = Offset(midX + c * blockW, r * blockH),
                            size = Size(blockW - 1f, blockH - 1f)
                        )
                    }
                }
            }

            // Glowing vertical divider line
            drawLine(
                color = Color.White,
                start = Offset(midX, 0f),
                end = Offset(midX, h),
                strokeWidth = 2.5f
            )
        }
    }
}

@Composable
fun PrivacyFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
