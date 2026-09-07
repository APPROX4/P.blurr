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
        private const val KEY_DILATION       = "mask_dilation_px"
        private const val KEY_FEATHER        = "feather_edges"
        private const val KEY_EFFECT_TYPE    = "effect_type"   // "Blur" | "Pixelate"
        private const val KEY_BLUR_INT       = "blur_intensity"
        private const val KEY_PIXEL_BLOCK    = "pixel_block_size"
        private const val KEY_LOGGING        = "logging_enabled"
        private const val KEY_AUTO_UPDATE    = "auto_update_enabled"
        private const val KEY_STRIP_EXIF     = "strip_exif_enabled"
        private const val KEY_INTERNET_NOTICE = "has_accepted_internet_notice"
        private const val KEY_ONBOARDING     = "has_completed_onboarding"

        val DEFAULTS = CensorOptions(
            maskDilationPx          = 4,
            featherEdges            = true,
            effect                  = CensorEffect.Pixelate(28),
            loggingEnabled          = true,
            autoUpdateCheckEnabled  = false,
            stripExifMetadata       = true,
            hasAcceptedInternetNotice = false,
            hasCompletedOnboarding = false
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
            maskDilationPx            = prefs.getInt(KEY_DILATION, 4),
            featherEdges              = prefs.getBoolean(KEY_FEATHER, true),
            effect                    = effect,
            loggingEnabled            = prefs.getBoolean(KEY_LOGGING, true),
            autoUpdateCheckEnabled    = prefs.getBoolean(KEY_AUTO_UPDATE, false),
            stripExifMetadata         = prefs.getBoolean(KEY_STRIP_EXIF, true),
            hasAcceptedInternetNotice = prefs.getBoolean(KEY_INTERNET_NOTICE, false),
            hasCompletedOnboarding   = prefs.getBoolean(KEY_ONBOARDING, false)
        )
    }

    fun save(options: CensorOptions) {
        prefs.edit().apply {
            putInt(KEY_DILATION,        options.maskDilationPx)
            putBoolean(KEY_FEATHER,     options.featherEdges)
            when (val e = options.effect) {
                is CensorEffect.Blur     -> { putString(KEY_EFFECT_TYPE, "Blur");     putInt(KEY_BLUR_INT,    e.intensity) }
                is CensorEffect.Pixelate -> { putString(KEY_EFFECT_TYPE, "Pixelate"); putInt(KEY_PIXEL_BLOCK, e.blockSize) }
            }
            putBoolean(KEY_LOGGING,         options.loggingEnabled)
            putBoolean(KEY_AUTO_UPDATE,     options.autoUpdateCheckEnabled)
            putBoolean(KEY_STRIP_EXIF,      options.stripExifMetadata)
            putBoolean(KEY_INTERNET_NOTICE, options.hasAcceptedInternetNotice)
            putBoolean(KEY_ONBOARDING,      options.hasCompletedOnboarding)
            apply()
        }
    }

    fun resetToDefaults() {
        save(DEFAULTS)
    }
}
