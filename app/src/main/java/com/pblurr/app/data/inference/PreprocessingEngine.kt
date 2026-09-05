package com.pblurr.app.data.inference

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.model.BoundingBox
import com.pblurr.app.domain.model.LogCategory
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PreprocessingEngine {

    data class LetterboxResult(
        val letterboxedBitmap: Bitmap,
        val scale: Float,
        val padX: Float,
        val padY: Float,
        val originalWidth: Int,
        val originalHeight: Int,
        val targetWidth: Int,
        val targetHeight: Int,
        val tensorToOriginalMatrix: Matrix,
        val originalToTensorMatrix: Matrix
    )

    /**
     * Resizes and letterboxes bitmap to target tensor dimensions while preserving aspect ratio.
     */
    fun letterbox(source: Bitmap, targetWidth: Int, targetHeight: Int): LetterboxResult {
        val origW = source.width
        val origH = source.height

        val scale = Math.min(
            targetWidth.toFloat() / origW,
            targetHeight.toFloat() / origH
        )

        val scaledW = origW * scale
        val scaledH = origH * scale

        val padX = (targetWidth - scaledW) / 2f
        val padY = (targetHeight - scaledH) / 2f

        val outputBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        canvas.drawColor(Color.BLACK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val srcRect = android.graphics.Rect(0, 0, origW, origH)
        val dstRect = RectF(padX, padY, padX + scaledW, padY + scaledH)
        canvas.drawBitmap(source, srcRect, dstRect, paint)

        // Matrix mapping tensor coords [0..targetWidth, 0..targetHeight] -> original coords [0..origW, 0..origH]
        val tensorToOriginalMatrix = Matrix()
        tensorToOriginalMatrix.postTranslate(-padX, -padY)
        tensorToOriginalMatrix.postScale(1f / scale, 1f / scale)

        // Matrix mapping original coords -> tensor coords
        val originalToTensorMatrix = Matrix()
        originalToTensorMatrix.postScale(scale, scale)
        originalToTensorMatrix.postTranslate(padX, padY)

        AppLogger.d(
            LogCategory.PREPROCESSING,
            "Letterboxed $origW x $origH -> $targetWidth x $targetHeight",
            "scale=$scale, padX=$padX, padY=$padY"
        )

        return LetterboxResult(
            letterboxedBitmap = outputBitmap,
            scale = scale,
            padX = padX,
            padY = padY,
            originalWidth = origW,
            originalHeight = origH,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            tensorToOriginalMatrix = tensorToOriginalMatrix,
            originalToTensorMatrix = originalToTensorMatrix
        )
    }

    /**
     * Converts ARGB_8888 Bitmap to normalized float ByteBuffer [0.0..1.0] for TFLite.
     */
    fun bitmapToByteBuffer(
        bitmap: Bitmap,
        isNormalized: Boolean = true,
        mean: Float = 0f,
        std: Float = 255f
    ): ByteBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val byteBuffer = ByteBuffer.allocateDirect(4 * width * height * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(width * height)
        bitmap.getPixels(intValues, 0, width, 0, 0, width, height)

        var pixel = 0
        for (i in 0 until width) {
            for (j in 0 until height) {
                val valPixel = intValues[pixel++]
                val r = ((valPixel shr 16) and 0xFF)
                val g = ((valPixel shr 8) and 0xFF)
                val b = (valPixel and 0xFF)

                if (isNormalized) {
                    byteBuffer.putFloat((r - mean) / std)
                    byteBuffer.putFloat((g - mean) / std)
                    byteBuffer.putFloat((b - mean) / std)
                } else {
                    byteBuffer.putFloat(r.toFloat())
                    byteBuffer.putFloat(g.toFloat())
                    byteBuffer.putFloat(b.toFloat())
                }
            }
        }
        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Maps tensor bounding box coordinates back to original image space cleanly using matrix transformation.
     */
    fun mapBoxToOriginal(box: BoundingBox, letterboxResult: LetterboxResult): BoundingBox {
        val pts = floatArrayOf(box.left, box.top, box.right, box.bottom)
        letterboxResult.tensorToOriginalMatrix.mapPoints(pts)

        val mappedLeft = pts[0].coerceIn(0f, letterboxResult.originalWidth.toFloat())
        val mappedTop = pts[1].coerceIn(0f, letterboxResult.originalHeight.toFloat())
        val mappedRight = pts[2].coerceIn(mappedLeft, letterboxResult.originalWidth.toFloat())
        val mappedBottom = pts[3].coerceIn(mappedTop, letterboxResult.originalHeight.toFloat())

        return BoundingBox(
            left = mappedLeft,
            top = mappedTop,
            right = mappedRight,
            bottom = mappedBottom,
            score = box.score,
            classIndex = box.classIndex,
            label = box.label
        )
    }
}
