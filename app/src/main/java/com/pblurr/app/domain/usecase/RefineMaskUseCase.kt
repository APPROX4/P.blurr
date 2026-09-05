package com.pblurr.app.domain.usecase

import android.graphics.Bitmap
import com.pblurr.app.data.inference.MaskRefiner
import com.pblurr.app.domain.model.DetectionMask

class RefineMaskUseCase(
    private val maskRefiner: MaskRefiner = MaskRefiner()
) {
    operator fun invoke(
        targetWidth: Int,
        targetHeight: Int,
        detections: List<DetectionMask>,
        dilationPx: Int,
        featherEdges: Boolean
    ): Bitmap {
        return maskRefiner.refineMasks(
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            detections = detections,
            dilationPx = dilationPx,
            featherEdges = featherEdges
        )
    }
}
