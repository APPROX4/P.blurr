package com.pblurr.app.domain.usecase

import android.graphics.Bitmap
import com.pblurr.app.domain.detector.PrivateRegionDetector
import com.pblurr.app.domain.model.DetectionMask

class DetectPrivateRegionsUseCase(
    private val detector: PrivateRegionDetector
) {
    suspend operator fun invoke(bitmap: Bitmap): List<DetectionMask> {
        return detector.detect(bitmap)
    }
}
