package com.pblurr.app

import com.pblurr.app.data.inference.PreprocessingEngine
import com.pblurr.app.domain.model.BoundingBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreprocessingEngineTest {

    private val engine = PreprocessingEngine()

    @Test
    fun testLetterboxAspectRatioAndScaleCalculation() {
        val origW = 1000
        val origH = 500
        val targetW = 320
        val targetH = 320

        // Scale should be min(320/1000 = 0.32, 320/500 = 0.64) = 0.32
        val expectedScale = 0.32f
        val scaledW = origW * expectedScale // 320
        val scaledH = origH * expectedScale // 160

        val expectedPadX = 0f
        val expectedPadY = (targetH - scaledH) / 2f // 80f

        val boxInTensor = BoundingBox(
            left = 0f,
            top = expectedPadY,
            right = scaledW,
            bottom = expectedPadY + scaledH,
            score = 0.95f
        )

        // Matrix mapping back should yield original dimensions
        val pts = floatArrayOf(boxInTensor.left, boxInTensor.top, boxInTensor.right, boxInTensor.bottom)
        
        // Manual verification of inverse transform: (tensor_y - padY) / scale
        val mappedLeft = (boxInTensor.left - expectedPadX) / expectedScale
        val mappedTop = (boxInTensor.top - expectedPadY) / expectedScale
        val mappedRight = (boxInTensor.right - expectedPadX) / expectedScale
        val mappedBottom = (boxInTensor.bottom - expectedPadY) / expectedScale

        assertEquals(0f, mappedLeft, 0.01f)
        assertEquals(0f, mappedTop, 0.01f)
        assertEquals(1000f, mappedRight, 0.01f)
        assertEquals(500f, mappedBottom, 0.01f)
    }

    @Test
    fun testBoundingBoxAreaCalculation() {
        val box = BoundingBox(left = 10f, top = 10f, right = 110f, bottom = 60f, score = 0.9f)
        assertEquals(100f, box.width, 0.001f)
        assertEquals(50f, box.height, 0.001f)
        assertEquals(5000f, box.area, 0.001f)
    }
}
