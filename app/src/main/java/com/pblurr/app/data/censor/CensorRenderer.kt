package com.pblurr.app.data.censor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.model.CensorEffect
import com.pblurr.app.domain.model.LogCategory

class CensorRenderer {

    /**
     * Applies specified censor effect (Blur or Pixelate) to the source bitmap.
     */
    fun applyEffect(source: Bitmap, effect: CensorEffect): Bitmap {
        val startTime = System.currentTimeMillis()
        val result = when (effect) {
            is CensorEffect.Blur -> applyBlur(source, effect.intensity)
            is CensorEffect.Pixelate -> applyPixelate(source, effect.blockSize)
        }
        val duration = System.currentTimeMillis() - startTime
        AppLogger.d(LogCategory.RENDERING, "Applied effect ${effect::class.java.simpleName} in ${duration}ms")
        return result
    }

    /**
     * Memory-safe Stack Blur: downscales to a capped resolution before blurring,
     * then upscales the result. This avoids OOM on high-resolution images (e.g. 1440x1440)
     * while producing visually identical output since blur averages out fine detail.
     */
    fun applyBlur(source: Bitmap, intensity: Int): Bitmap {
        val radius = (intensity.coerceIn(1, 100) * 0.4f).toInt().coerceAtLeast(1)
        val origW = source.width
        val origH = source.height

        // Downscale to max 512px on the longer side to cap memory usage
        val maxDim = 512
        val scale = if (origW >= origH) maxDim.toFloat() / origW else maxDim.toFloat() / origH
        val blurW = if (scale < 1f) (origW * scale).toInt().coerceAtLeast(1) else origW
        val blurH = if (scale < 1f) (origH * scale).toInt().coerceAtLeast(1) else origH

        // Downscale, blur the small bitmap, then stretch back to original dimensions
        val small = if (scale < 1f) Bitmap.createScaledBitmap(source, blurW, blurH, true) else
            source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(blurW * blurH)
        small.getPixels(pixels, 0, blurW, 0, 0, blurW, blurH)
        stackBlur(pixels, blurW, blurH, radius)
        small.setPixels(pixels, 0, blurW, 0, 0, blurW, blurH)

        // Upscale result back to original resolution
        return if (scale < 1f) {
            val upscaled = Bitmap.createScaledBitmap(small, origW, origH, true)
            small.recycle()
            upscaled
        } else {
            small
        }
    }

    /**
     * Pixelates the bitmap by downscaling and scaling back up with Nearest-Neighbor filtering.
     */
    fun applyPixelate(source: Bitmap, blockSize: Int): Bitmap {
        val block = blockSize.coerceIn(4, 128)
        val origW = source.width
        val origH = source.height

        val scaledW = (origW / block).coerceAtLeast(1)
        val scaledH = (origH / block).coerceAtLeast(1)

        val smallBitmap = Bitmap.createScaledBitmap(source, scaledW, scaledH, false)
        val pixelated = Bitmap.createScaledBitmap(smallBitmap, origW, origH, false)
        smallBitmap.recycle()

        return pixelated
    }

    /**
     * Composites original bitmap and censored bitmap using alpha mask:
     * final = original * (1 - mask) + censored * mask
     *
     * @param original Original source photo
     * @param censored Full image with censor effect applied
     * @param mask Single-channel alpha mask where 255/White is target region
     */
    fun composite(original: Bitmap, censored: Bitmap, mask: Bitmap): Bitmap {
        val width = original.width
        val height = original.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Draw original un-censored image
        canvas.drawBitmap(original, 0f, 0f, paint)

        // 2. Prepare censored layer clipped strictly by alpha mask
        val maskedCensored = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(maskedCensored)

        // Draw censored bitmap
        layerCanvas.drawBitmap(censored, 0f, 0f, paint)

        // Apply DST_IN with mask bitmap to keep only masked areas
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        layerCanvas.drawBitmap(mask, 0f, 0f, maskPaint)

        // 3. Composite masked censored layer onto original
        canvas.drawBitmap(maskedCensored, 0f, 0f, paint)
        maskedCensored.recycle()

        return result
    }

    private fun stackBlur(pix: IntArray, w: Int, h: Int, radius: Int) {
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))

        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yi = 0
        yw = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                val sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {

                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                val sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                val sir2 = stack[stackpointer % div]

                routsum += sir2[0]
                goutsum += sir2[1]
                boutsum += sir2[2]

                rinsum -= sir2[0]
                ginsum -= sir2[1]
                binsum -= sir2[2]

                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                val sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                // Preserve original Alpha channel
                pix[yi] = pix[yi] and -0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                val sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                val sir2 = stack[stackpointer % div]

                routsum += sir2[0]
                goutsum += sir2[1]
                boutsum += sir2[2]

                rinsum -= sir2[0]
                ginsum -= sir2[1]
                binsum -= sir2[2]

                yi += w
                y++
            }
            x++
        }
    }
}
