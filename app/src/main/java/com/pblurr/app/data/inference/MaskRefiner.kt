package com.pblurr.app.data.inference

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.model.DetectionMask
import com.pblurr.app.domain.model.LogCategory

class MaskRefiner {

    /**
     * Combines multiple raw detection masks/boxes into a refined, high-resolution alpha mask.
     * Strictly targets intimate regions without censoring entire body or background.
     */
    fun refineMasks(
        targetWidth: Int,
        targetHeight: Int,
        detections: List<DetectionMask>,
        dilationPx: Int = 4,
        featherEdges: Boolean = true
    ): Bitmap {
        val startTime = System.currentTimeMillis()

        val maskBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(maskBitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val maxDim = Math.max(targetWidth, targetHeight)
        val adaptiveScale = (maxDim / 1000f).coerceAtLeast(1.0f)
        val scaledDilation = (dilationPx * adaptiveScale)

        for (detection in detections) {
            val box = detection.boundingBox

            // Filter out invalid boxes or whole-screen box anomalies (> 35% frame dimension)
            if (box.width >= targetWidth * 0.85f && box.height >= targetHeight * 0.85f) continue

            if (detection.maskBitmap != null) {
                val srcRect = android.graphics.Rect(0, 0, detection.maskBitmap.width, detection.maskBitmap.height)
                val dstRect = RectF(
                    (box.left - scaledDilation).coerceIn(0f, targetWidth.toFloat()),
                    (box.top - scaledDilation).coerceIn(0f, targetHeight.toFloat()),
                    (box.right + scaledDilation).coerceIn(0f, targetWidth.toFloat()),
                    (box.bottom + scaledDilation).coerceIn(0f, targetHeight.toFloat())
                )
                canvas.drawBitmap(detection.maskBitmap, srcRect, dstRect, paint)
            } else {
                val path = Path()
                val rect = RectF(
                    (box.left - scaledDilation).coerceIn(0f, targetWidth.toFloat()),
                    (box.top - scaledDilation).coerceIn(0f, targetHeight.toFloat()),
                    (box.right + scaledDilation).coerceIn(0f, targetWidth.toFloat()),
                    (box.bottom + scaledDilation).coerceIn(0f, targetHeight.toFloat())
                )
                
                // Tight anatomical region rounding to avoid rectangular cuts
                val rx = (rect.width() * 0.25f).coerceAtMost(30f * adaptiveScale)
                val ry = (rect.height() * 0.25f).coerceAtMost(30f * adaptiveScale)
                path.addRoundRect(rect, rx, ry, Path.Direction.CW)
                canvas.drawPath(path, paint)
            }
        }

        // Apply feathering / soft edge blur if requested
        val finalMask = if (featherEdges && detections.isNotEmpty()) {
            applySoftFeathering(maskBitmap, (2f * adaptiveScale))
        } else {
            maskBitmap
        }

        val duration = System.currentTimeMillis() - startTime
        AppLogger.d(
            LogCategory.MASK,
            "Refined ${detections.size} detection masks",
            "Target: ${targetWidth}x${targetHeight}, duration: ${duration}ms, dilationPx: $dilationPx (scaled: $scaledDilation)"
        )

        return finalMask
    }

    private fun applySoftFeathering(maskBitmap: Bitmap, blurRadius: Float): Bitmap {
        val width = maskBitmap.width
        val height = maskBitmap.height

        val blurred = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blurred)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            maskFilter = android.graphics.BlurMaskFilter(
                blurRadius.coerceAtLeast(1.0f),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }

        canvas.drawBitmap(maskBitmap, 0f, 0f, paint)
        return blurred
    }
}
