package com.pblurr.app.domain.model

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF

/**
 * Bounding box representing a detected object region.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val score: Float,
    val classIndex: Int = 0,
    val label: String = "private_region"
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = width * height

    fun toRectF(): RectF = RectF(left, top, right, bottom)
}

/**
 * Detailed detection mask with pixel-level alpha bitmap representation.
 */
data class DetectionMask(
    val maskBitmap: Bitmap?,
    val confidence: Float,
    val label: String = "private_region",
    val boundingBox: BoundingBox,
    val transformMatrix: Matrix? = null
)

/**
 * Censor visual effect types supported by the rendering engine.
 */
sealed class CensorEffect {
    data class Blur(val intensity: Int = 75) : CensorEffect()
    data class Pixelate(val blockSize: Int = 28) : CensorEffect()
}

/**
 * Configurable parameters for mask refinement and censor rendering.
 */
data class CensorOptions(
    val maskDilationPx: Int = 4,
    val featherEdges: Boolean = true,
    val effect: CensorEffect = CensorEffect.Pixelate(28)
)

/**
 * Comprehensive pipeline outcome holding intermediate and final artifacts.
 */
data class DetectionPipelineResult(
    val originalBitmap: Bitmap,
    val rawDetections: List<DetectionMask>,
    val refinedMask: Bitmap,
    val censoredBitmap: Bitmap,
    val inferenceTimeMs: Long,
    val refinementTimeMs: Long,
    val renderingTimeMs: Long,
    val maskCoveragePercentage: Float,
    val originalWidth: Int,
    val originalHeight: Int
)

/**
 * Structured log metadata for diagnostics.
 */
enum class LogCategory {
    APP,
    IMAGE,
    PREPROCESSING,
    MODEL,
    DETECTION,
    MASK,
    RENDERING,
    EXPORT
}

enum class LogLevel {
    INFO,
    DEBUG,
    WARN,
    ERROR
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val category: LogCategory = LogCategory.APP,
    val tag: String = "P.blurr",
    val message: String,
    val details: String? = null
)
