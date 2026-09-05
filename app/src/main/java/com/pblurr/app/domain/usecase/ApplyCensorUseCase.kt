package com.pblurr.app.domain.usecase

import android.graphics.Bitmap
import com.pblurr.app.data.censor.CensorRenderer
import com.pblurr.app.domain.model.CensorEffect

class ApplyCensorUseCase(
    private val censorRenderer: CensorRenderer = CensorRenderer()
) {
    operator fun invoke(
        original: Bitmap,
        mask: Bitmap,
        effect: CensorEffect
    ): Bitmap {
        val fullCensored = censorRenderer.applyEffect(original, effect)
        return censorRenderer.composite(original, fullCensored, mask)
    }
}
