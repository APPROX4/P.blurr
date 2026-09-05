package com.pblurr.app.presentation.ui.editor.manual

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pblurr.app.presentation.ui.theme.DarkSurface

enum class EditorTool {
    BOX,    // Drag to create custom rectangle blur box
    ROUND,  // Drag to create custom circular/oval blur box
    BRUSH,  // Freehand paint stroke
    ERASER  // Erase mask
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualMaskEditorScreen(
    originalBitmap: Bitmap,
    initialMask: Bitmap,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearAll: () -> Unit,
    onRestoreAiMask: () -> Unit,
    onDone: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    // Working mutable copy of the mask bitmap
    val workingMask = remember(initialMask) {
        initialMask.copy(initialMask.config ?: Bitmap.Config.ARGB_8888, true)
    }

    var selectedTool by remember { mutableStateOf(EditorTool.BOX) }
    var brushRadius by remember { mutableStateOf(35f) }
    var maskRevision by remember { mutableStateOf(0) }

    // Shape Dragging State for Box and Round tools
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }

    // Transformation zoom & pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Mask Touchup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Clear, contentDescription = "Cancel", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onUndo, enabled = canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) Color.White else Color.Gray)
                    }
                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) Color.White else Color.Gray)
                    }
                    IconButton(onClick = onRestoreAiMask) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Restore AI Mask", tint = Color.White)
                    }
                    IconButton(onClick = onClearAll) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Mask", tint = Color(0xFFFF6B6B))
                    }
                    Button(
                        onClick = { onDone(workingMask) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
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
            // Interactive Painting Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .transformable(state = transformState),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    // 1. Background Source Image
                    androidx.compose.foundation.Image(
                        bitmap = originalBitmap.asImageBitmap(),
                        contentDescription = "Original Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. Active Alpha Mask Overlay
                    key(maskRevision) {
                        androidx.compose.foundation.Image(
                            bitmap = workingMask.asImageBitmap(),
                            contentDescription = "Working Mask Overlay",
                            contentScale = ContentScale.Fit,
                            alpha = 0.55f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Touch Gesture Handler & Live Shape Drag Preview Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedTool, brushRadius, scale) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        dragStart = startOffset
                                        dragCurrent = startOffset

                                        if (selectedTool == EditorTool.BRUSH || selectedTool == EditorTool.ERASER) {
                                            val canvasW = size.width.toFloat()
                                            val canvasH = size.height.toFloat()
                                            val imgW = workingMask.width.toFloat()
                                            val imgH = workingMask.height.toFloat()

                                            val bitmapScale = Math.min(canvasW / imgW, canvasH / imgH)
                                            val offsetX = (canvasW - imgW * bitmapScale) / 2f
                                            val offsetY = (canvasH - imgH * bitmapScale) / 2f

                                            val bmpX = (startOffset.x - offsetX) / bitmapScale
                                            val bmpY = (startOffset.y - offsetY) / bitmapScale

                                            if (bmpX in 0f..imgW && bmpY in 0f..imgH) {
                                                val maskCanvas = Canvas(workingMask)
                                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                    style = Paint.Style.FILL
                                                }
                                                if (selectedTool == EditorTool.BRUSH) {
                                                    paint.color = AndroidColor.WHITE
                                                } else {
                                                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                                                }
                                                maskCanvas.drawCircle(bmpX, bmpY, brushRadius / scale, paint)
                                                maskRevision++
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val start = dragStart
                                        val curr = dragCurrent
                                        if (start != null && curr != null && (selectedTool == EditorTool.BOX || selectedTool == EditorTool.ROUND)) {
                                            val canvasW = size.width.toFloat()
                                            val canvasH = size.height.toFloat()
                                            val imgW = workingMask.width.toFloat()
                                            val imgH = workingMask.height.toFloat()

                                            val bitmapScale = Math.min(canvasW / imgW, canvasH / imgH)
                                            val offsetX = (canvasW - imgW * bitmapScale) / 2f
                                            val offsetY = (canvasH - imgH * bitmapScale) / 2f

                                            val bmpX1 = (start.x - offsetX) / bitmapScale
                                            val bmpY1 = (start.y - offsetY) / bitmapScale
                                            val bmpX2 = (curr.x - offsetX) / bitmapScale
                                            val bmpY2 = (curr.y - offsetY) / bitmapScale

                                            val left = Math.min(bmpX1, bmpX2).coerceIn(0f, imgW)
                                            val top = Math.min(bmpY1, bmpY2).coerceIn(0f, imgH)
                                            val right = Math.max(bmpX1, bmpX2).coerceIn(0f, imgW)
                                            val bottom = Math.max(bmpY1, bmpY2).coerceIn(0f, imgH)

                                            if (right - left > 2f && bottom - top > 2f) {
                                                val maskCanvas = Canvas(workingMask)
                                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                    color = AndroidColor.WHITE
                                                    style = Paint.Style.FILL
                                                }
                                                val rectF = RectF(left, top, right, bottom)
                                                if (selectedTool == EditorTool.BOX) {
                                                    maskCanvas.drawRect(rectF, paint)
                                                } else {
                                                    maskCanvas.drawOval(rectF, paint)
                                                }
                                                maskRevision++
                                            }
                                        }
                                        dragStart = null
                                        dragCurrent = null
                                    },
                                    onDragCancel = {
                                        dragStart = null
                                        dragCurrent = null
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        dragCurrent = change.position

                                        if (selectedTool == EditorTool.BRUSH || selectedTool == EditorTool.ERASER) {
                                            val canvasW = size.width.toFloat()
                                            val canvasH = size.height.toFloat()
                                            val imgW = workingMask.width.toFloat()
                                            val imgH = workingMask.height.toFloat()

                                            val bitmapScale = Math.min(canvasW / imgW, canvasH / imgH)
                                            val offsetX = (canvasW - imgW * bitmapScale) / 2f
                                            val offsetY = (canvasH - imgH * bitmapScale) / 2f

                                            val touchPos = change.position
                                            val bmpX = (touchPos.x - offsetX) / bitmapScale
                                            val bmpY = (touchPos.y - offsetY) / bitmapScale

                                            if (bmpX in 0f..imgW && bmpY in 0f..imgH) {
                                                val maskCanvas = Canvas(workingMask)
                                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                    style = Paint.Style.FILL
                                                }
                                                if (selectedTool == EditorTool.BRUSH) {
                                                    paint.color = AndroidColor.WHITE
                                                } else {
                                                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                                                }
                                                maskCanvas.drawCircle(bmpX, bmpY, brushRadius / scale, paint)
                                                maskRevision++
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        // Live Visual Shape Drag Overlay (White glowing rect or oval outline)
                        val start = dragStart
                        val curr = dragCurrent
                        if (start != null && curr != null && (selectedTool == EditorTool.BOX || selectedTool == EditorTool.ROUND)) {
                            val left = Math.min(start.x, curr.x)
                            val top = Math.min(start.y, curr.y)
                            val right = Math.max(start.x, curr.x)
                            val bottom = Math.max(start.y, curr.y)
                            val rectSize = Size(right - left, bottom - top)
                            val topLeft = Offset(left, top)

                            if (selectedTool == EditorTool.BOX) {
                                drawRect(
                                    color = Color.White.copy(alpha = 0.20f),
                                    topLeft = topLeft,
                                    size = rectSize
                                )
                                drawRect(
                                    color = Color.White,
                                    topLeft = topLeft,
                                    size = rectSize,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )
                            } else if (selectedTool == EditorTool.ROUND) {
                                drawOval(
                                    color = Color.White.copy(alpha = 0.20f),
                                    topLeft = topLeft,
                                    size = rectSize
                                )
                                drawOval(
                                    color = Color.White,
                                    topLeft = topLeft,
                                    size = rectSize,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Tool Palette & Size Controls (Luxury Dark Glassmorphism)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(DarkSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(16.dp)
            ) {
                // Tool Palette Chips: Box, Round, Brush, Eraser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tool 1: Box Blur
                    FilterChip(
                        selected = selectedTool == EditorTool.BOX,
                        onClick = { selectedTool = EditorTool.BOX },
                        label = { Text("Box", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.CropSquare, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.White.copy(alpha = 0.06f),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Tool 2: Round Blur
                    FilterChip(
                        selected = selectedTool == EditorTool.ROUND,
                        onClick = { selectedTool = EditorTool.ROUND },
                        label = { Text("Round", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.White.copy(alpha = 0.06f),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Tool 3: Brush
                    FilterChip(
                        selected = selectedTool == EditorTool.BRUSH,
                        onClick = { selectedTool = EditorTool.BRUSH },
                        label = { Text("Brush", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.White.copy(alpha = 0.06f),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Tool 4: Eraser
                    FilterChip(
                        selected = selectedTool == EditorTool.ERASER,
                        onClick = { selectedTool = EditorTool.ERASER },
                        label = { Text("Eraser", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B6B),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.06f),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedTool == EditorTool.BRUSH || selectedTool == EditorTool.ERASER) {
                    Spacer(Modifier.height(10.dp))

                    // Brush / Eraser Size Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Brush Size: ${brushRadius.toInt()}px",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.width(95.dp)
                        )
                        Slider(
                            value = brushRadius,
                            onValueChange = { brushRadius = it },
                            valueRange = 5f..120f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
