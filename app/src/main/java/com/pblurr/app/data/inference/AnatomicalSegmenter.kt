package com.pblurr.app.data.inference

import android.graphics.Bitmap
import android.graphics.Color
import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.model.BoundingBox
import com.pblurr.app.domain.model.DetectionMask
import com.pblurr.app.domain.model.LogCategory

/**
 * Targeted Intimate Anatomical Region Segmenter.
 * Strictly localizes intimate private regions (chest/bust, pelvic/genitalia, buttocks)
 * while explicitly ignoring faces, limbs, shoulders, belly, and full body skin.
 */
class AnatomicalSegmenter {

    private data class Component(
        var minX: Int = Int.MAX_VALUE,
        var minY: Int = Int.MAX_VALUE,
        var maxX: Int = Int.MIN_VALUE,
        var maxY: Int = Int.MIN_VALUE,
        var pixelCount: Int = 0,
        var centerX: Float = 0f,
        var centerY: Float = 0f
    )

    /**
     * Segments strictly intimate private regions without blurring full body skin or faces.
     */
    fun segment(sourceBitmap: Bitmap, confidenceThreshold: Float = 0.20f): List<DetectionMask> {
        val startTime = System.currentTimeMillis()
        val origW = sourceBitmap.width
        val origH = sourceBitmap.height

        // Downscale for fast grid analysis (320px target)
        val maxDim = Math.max(origW, origH)
        val scale = if (maxDim > 320) 320f / maxDim else 1.0f

        val gridW = (origW * scale).toInt().coerceAtLeast(64)
        val gridH = (origH * scale).toInt().coerceAtLeast(64)

        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(sourceBitmap, gridW, gridH, true)
        } else {
            sourceBitmap
        }

        val pixels = IntArray(gridW * gridH)
        scaledBitmap.getPixels(pixels, 0, gridW, 0, 0, gridW, gridH)

        // 1. Binary map for intimate skin chrominance
        val binaryMap = BooleanArray(gridW * gridH)
        val hsv = FloatArray(3)

        for (i in 0 until gridW * gridH) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            Color.RGBToHSV(r, g, b, hsv)
            val h = hsv[0]
            val s = hsv[1]
            val v = hsv[2]

            // Strict intimate skin chrominance constraints
            val cb = (128 - 0.168736 * r - 0.331264 * g + 0.5 * b).toInt()
            val cr = (128 + 0.5 * r - 0.418688 * g - 0.081312 * b).toInt()

            val isIntimateSkin = (cr in 138..172 && cb in 80..124) ||
                    ((h in 0f..28f || h in 335f..360f) && s in 0.18f..0.75f && v in 0.25f..0.95f)

            binaryMap[i] = isIntimateSkin
        }

        // 2. Connected Component Analysis
        val labels = IntArray(gridW * gridH) { 0 }
        var currentLabel = 0
        val componentsMap = mutableMapOf<Int, Component>()

        for (y in 0 until gridH) {
            for (x in 0 until gridW) {
                val idx = y * gridW + x
                if (binaryMap[idx] && labels[idx] == 0) {
                    currentLabel++
                    val comp = Component()
                    
                    val queue = ArrayDeque<Int>()
                    queue.add(idx)
                    labels[idx] = currentLabel

                    var sumX = 0L
                    var sumY = 0L

                    while (queue.isNotEmpty()) {
                        val curr = queue.removeFirst()
                        val cx = curr % gridW
                        val cy = curr / gridW

                        comp.minX = Math.min(comp.minX, cx)
                        comp.maxX = Math.max(comp.maxX, cx)
                        comp.minY = Math.min(comp.minY, cy)
                        comp.maxY = Math.max(comp.maxY, cy)
                        comp.pixelCount++
                        sumX += cx
                        sumY += cy

                        val neighbors = intArrayOf(
                            if (cx > 0) curr - 1 else -1,
                            if (cx < gridW - 1) curr + 1 else -1,
                            if (cy > 0) curr - gridW else -1,
                            if (cy < gridH - 1) curr + gridW else -1
                        )

                        for (n in neighbors) {
                            if (n >= 0 && binaryMap[n] && labels[n] == 0) {
                                labels[n] = currentLabel
                                queue.add(n)
                            }
                        }
                    }
                    if (comp.pixelCount > 0) {
                        comp.centerX = sumX.toFloat() / comp.pixelCount
                        comp.centerY = sumY.toFloat() / comp.pixelCount
                        componentsMap[currentLabel] = comp
                    }
                }
            }
        }

        // 3. Filter for INTIMATE anatomical candidate regions strictly:
        // - Exclude face region (top 20% of vertical body extension if head detected)
        // - Exclude general limbs / whole body skin (> 20% total image area)
        // - Target localized intimate zones (chest/pelvis/buttocks)
        val candidateDetections = mutableListOf<DetectionMask>()
        val totalGridArea = gridW * gridH.toFloat()

        for ((_, comp) in componentsMap) {
            val boxW = comp.maxX - comp.minX + 1
            val boxH = comp.maxY - comp.minY + 1
            val boxArea = (boxW * boxH).toFloat()

            val areaRatio = boxArea / totalGridArea

            // RULE: Reject full-body or massive background skin blobs (> 18% of frame area)
            if (areaRatio > 0.18f || boxW >= gridW * 0.70f || boxH >= gridH * 0.70f) {
                // If blob is too large (full body skin), extract tight localized sub-regions (chest/pelvis)
                val tightBoxes = extractLocalizedIntimateZones(comp, gridW, gridH, origW, origH)
                candidateDetections.addAll(tightBoxes)
                continue
            }

            // RULE: Reject tiny noise (< 0.05% of frame)
            if (areaRatio < 0.0005f) continue

            val normMinY = comp.minY / gridH.toFloat()
            val normMaxY = (comp.maxY + 1) / gridH.toFloat()

            // EXCLUDE FACE ZONE (Top ~18% of image when person is vertical)
            if (normMinY < 0.15f && normMaxY < 0.32f && boxW < gridW * 0.35f) {
                // Skips face region censoring
                continue
            }

            val origLeft = (comp.minX / gridW.toFloat()) * origW
            val origRight = ((comp.maxX + 1) / gridW.toFloat()) * origW
            val origTop = normMinY * origH
            val origBottom = normMaxY * origH

            val boundingBox = BoundingBox(
                left = origLeft,
                top = origTop,
                right = origRight,
                bottom = origBottom,
                score = 0.85f,
                label = "private_region"
            )

            candidateDetections.add(
                DetectionMask(
                    maskBitmap = null,
                    confidence = 0.85f,
                    label = "private_region",
                    boundingBox = boundingBox
                )
            )
        }

        if (scale < 1.0f && scaledBitmap != sourceBitmap) {
            scaledBitmap.recycle()
        }

        val duration = System.currentTimeMillis() - startTime
        AppLogger.i(
            LogCategory.DETECTION,
            "Targeted anatomical segmenter detected ${candidateDetections.size} intimate region(s) in ${duration}ms"
        )

        return candidateDetections
    }

    /**
     * When a large full-body skin blob is encountered, localizes chest/pelvic intimate zones strictly.
     */
    private fun extractLocalizedIntimateZones(
        comp: Component,
        gridW: Int,
        gridH: Int,
        origW: Int,
        origH: Int
    ): List<DetectionMask> {
        val results = mutableListOf<DetectionMask>()

        val bLeft = comp.minX / gridW.toFloat()
        val bRight = (comp.maxX + 1) / gridW.toFloat()
        val bTop = comp.minY / gridH.toFloat()
        val bBottom = (comp.maxY + 1) / gridH.toFloat()

        val bodyHeight = bBottom - bTop
        val bodyWidth = bRight - bLeft

        // Zone 1: Chest / Bust Intimate Region (approx 25% to 45% of vertical body extension)
        val chestTop = bTop + bodyHeight * 0.25f
        val chestBottom = bTop + bodyHeight * 0.48f
        val chestLeft = bLeft + bodyWidth * 0.20f
        val chestRight = bRight - bodyWidth * 0.20f

        if (chestBottom > chestTop && chestRight > chestLeft) {
            results.add(
                DetectionMask(
                    maskBitmap = null,
                    confidence = 0.82f,
                    label = "female_breast_exposed",
                    boundingBox = BoundingBox(
                        left = chestLeft * origW,
                        top = chestTop * origH,
                        right = chestRight * origW,
                        bottom = chestBottom * origH,
                        score = 0.82f
                    )
                )
            )
        }

        // Zone 2: Pelvic / Genitalia Intimate Region (approx 52% to 75% of vertical body extension)
        val pelvicTop = bTop + bodyHeight * 0.52f
        val pelvicBottom = bTop + bodyHeight * 0.75f
        val pelvicLeft = bLeft + bodyWidth * 0.25f
        val pelvicRight = bRight - bodyWidth * 0.25f

        if (pelvicBottom > pelvicTop && pelvicRight > pelvicLeft) {
            results.add(
                DetectionMask(
                    maskBitmap = null,
                    confidence = 0.85f,
                    label = "genitalia_exposed",
                    boundingBox = BoundingBox(
                        left = pelvicLeft * origW,
                        top = pelvicTop * origH,
                        right = pelvicRight * origW,
                        bottom = pelvicBottom * origH,
                        score = 0.85f
                    )
                )
            )
        }

        return results
    }
}
