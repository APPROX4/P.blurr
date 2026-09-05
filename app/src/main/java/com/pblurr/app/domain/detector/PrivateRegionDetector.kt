package com.pblurr.app.domain.detector

import android.graphics.Bitmap
import com.pblurr.app.domain.model.DetectionMask

/**
 * Interface abstraction for intimate private region detection models.
 */
interface PrivateRegionDetector {
    /**
     * Executes region detection and candidate mask extraction on the provided bitmap.
     * @param bitmap Source bitmap (can be high resolution)
     * @return List of detected region masks mapped back to original bitmap coordinates
     */
    suspend fun detect(bitmap: Bitmap): List<DetectionMask>

    /**
     * Releases underlying tensor sessions and native memory allocations.
     */
    fun release()
}
