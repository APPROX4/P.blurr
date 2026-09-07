package com.pblurr.app.presentation.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onCompleteOnboarding: (autoUpdateEnabled: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    var autoUpdateEnabled by remember { mutableStateOf(false) }

    // Media permission state check
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPermissionGranted = granted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C0C12),
                        Color(0xFF050508)
                    )
                )
            )
    ) {
        // Top Bar: Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { onCompleteOnboarding(autoUpdateEnabled) }
            ) {
                Text(
                    text = "Skip Setup",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Horizontal Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, bottom = 110.dp)
        ) { page ->
            when (page) {
                0 -> OnboardingSlide(
                    icon = Icons.Default.Security,
                    iconTint = Color(0xFF00FF66),
                    badgeText = "100% OFFLINE & PRIVATE",
                    title = "Your Photos Stay on Your Device",
                    description = "P.blurr processes all AI detection completely offline on your device using embedded machine learning. No photos, EXIF metadata, or private data are ever uploaded to any cloud server."
                )

                1 -> OnboardingSlideWithAction(
                    icon = Icons.Default.PhotoLibrary,
                    iconTint = Color(0xFF00E5FF),
                    badgeText = "MEDIA ACCESS",
                    title = "Photo Gallery Permission",
                    description = "P.blurr requires access to your photos strictly to select images for AI censoring and save protected photos back into your Gallery. We never store or transmit your media anywhere.",
                    actionContent = {
                        Button(
                            onClick = {
                                if (!isPermissionGranted) {
                                    permissionLauncher.launch(permissionToRequest)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPermissionGranted) Color(0xFF32CD32).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.15f),
                                contentColor = if (isPermissionGranted) Color(0xFF32CD32) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (isPermissionGranted) Color(0xFF32CD32) else Color.White.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = if (isPermissionGranted) Icons.Default.CheckCircle else Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (isPermissionGranted) "Photo Permission Granted" else "Grant Photo Access Permission",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                )

                2 -> OnboardingSlideWithAction(
                    icon = Icons.Default.SystemUpdate,
                    iconTint = Color(0xFFD0BCFF),
                    badgeText = "RELEASE UPDATES",
                    title = "GitHub Version Check",
                    description = "P.blurr can automatically check GitHub over the internet for new releases every time the app opens. No tracking, analytics, or background telemetry is ever collected.",
                    actionContent = {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-Check for Updates",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Check GitHub on startup",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(
                                    checked = autoUpdateEnabled,
                                    onCheckedChange = { autoUpdateEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }
                )

                3 -> OnboardingSlide(
                    icon = Icons.Default.Lock,
                    iconTint = Color(0xFF32CD32),
                    badgeText = "READY TO PROTECT",
                    title = "You're Ready to Use P.blurr",
                    description = "Select any photo to automatically censor sensitive regions, faces, text, and metadata with speed and total privacy."
                )
            }
        }

        // Bottom Controls: Page Indicator + Next / Get Started Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                repeat(4) { idx ->
                    val isSelected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            // Primary Navigation Button
            Button(
                onClick = {
                    if (pagerState.currentPage < 3) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onCompleteOnboarding(autoUpdateEnabled)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = if (pagerState.currentPage == 3) "Get Started" else "Continue",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun OnboardingSlide(
    icon: ImageVector,
    iconTint: Color,
    badgeText: String,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlowingIconHeader(icon = icon, tint = iconTint)

        Spacer(Modifier.height(32.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconTint.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, iconTint.copy(alpha = 0.4f))
        ) {
            Text(
                text = badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                color = iconTint,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = description,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun OnboardingSlideWithAction(
    icon: ImageVector,
    iconTint: Color,
    badgeText: String,
    title: String,
    description: String,
    actionContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlowingIconHeader(icon = icon, tint = iconTint)

        Spacer(Modifier.height(28.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconTint.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, iconTint.copy(alpha = 0.4f))
        ) {
            Text(
                text = badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                color = iconTint,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = description,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        actionContent()
    }
}

@Composable
fun GlowingIconHeader(
    icon: ImageVector,
    tint: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "IconGlow")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowRadius"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .background(tint.copy(alpha = 0.1f), CircleShape)
            .border(1.dp, tint.copy(alpha = 0.4f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(48.dp)
        )
    }
}
