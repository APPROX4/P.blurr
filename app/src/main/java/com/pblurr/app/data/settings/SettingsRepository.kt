package com.pblurr.app.data.settings

import android.content.Context
import com.pblurr.app.domain.model.CensorEffect
import com.pblurr.app.domain.model.CensorOptions

/**
 * Persists CensorOptions to SharedPreferences so settings survive app restarts.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("pblurr_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DILATION     = "mask_dilation_px"
        private const val KEY_FEATHER      = "feather_edges"
        private const val KEY_EFFECT_TYPE  = "effect_type"   // "Blur" | "Pixelate"
        private const val KEY_BLUR_INT     = "blur_intensity"
        private const val KEY_PIXEL_BLOCK  = "pixel_block_size"

        val DEFAULTS = CensorOptions(
            maskDilationPx      = 4,
            featherEdges        = true,
            effect              = CensorEffect.Pixelate(28)
        )
    }

    fun load(): CensorOptions {
        val effectType = prefs.getString(KEY_EFFECT_TYPE, "Pixelate") ?: "Pixelate"
        val effect = if (effectType == "Blur") {
            CensorEffect.Blur(prefs.getInt(KEY_BLUR_INT, 75))
        } else {
            CensorEffect.Pixelate(prefs.getInt(KEY_PIXEL_BLOCK, 28))
        }
        return CensorOptions(
            maskDilationPx      = prefs.getInt(KEY_DILATION, 4),
            featherEdges        = prefs.getBoolean(KEY_FEATHER, true),
            effect              = effect
        )
    }

    fun save(options: CensorOptions) {
        prefs.edit().apply {
            putInt(KEY_DILATION,    options.maskDilationPx)
            putBoolean(KEY_FEATHER, options.featherEdges)
            when (val e = options.effect) {
                is CensorEffect.Blur     -> { putString(KEY_EFFECT_TYPE, "Blur");     putInt(KEY_BLUR_INT,    e.intensity) }
                is CensorEffect.Pixelate -> { putString(KEY_EFFECT_TYPE, "Pixelate"); putInt(KEY_PIXEL_BLOCK, e.blockSize) }
            }
            apply()
        }
    }

    fun resetToDefaults() {
        save(DEFAULTS)
    }
}
