package com.pblurr.app.presentation.ui.theme

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.pblurr.app.R

/**
 * Fira Sans Italic loaded via Google Fonts provider.
 * Falls back to the system default if the device lacks Google Play Services.
 */
val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

private val firaSansFont = GoogleFont("Fira Sans")

val FiraSansItalicFamily = androidx.compose.ui.text.font.FontFamily(
    Font(
        googleFont     = firaSansFont,
        fontProvider   = googleFontProvider,
        weight         = FontWeight.Bold,
        style          = FontStyle.Italic
    ),
    Font(
        googleFont     = firaSansFont,
        fontProvider   = googleFontProvider,
        weight         = FontWeight.Normal,
        style          = FontStyle.Italic
    )
)
