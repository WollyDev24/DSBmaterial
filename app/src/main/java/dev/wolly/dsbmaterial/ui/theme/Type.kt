package dev.wolly.dsbmaterial.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.wolly.dsbmaterial.R

@Suppress("DEPRECATION")
private val DefaultTypography = Typography()

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
fun buildTypography(
    useCustomFont: Boolean,
    weight: Float,
    width: Float,
    opsz: Float,
    slnt: Float,
    grad: Float,
    rond: Float
): Typography {
    if (!useCustomFont) return DefaultTypography

    val family = FontFamily(
        Font(
            resId = R.font.google_sans_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.toInt()),
                FontVariation.width(width / 100f),
                FontVariation.slant(slnt),
                FontVariation.Setting("opsz", opsz),
                FontVariation.Setting("GRAD", grad),
                FontVariation.Setting("ROND", rond)
            )
        )
    )

    fun base(fontWeight: FontWeight, size: Int, lineHeight: Int, letterSpacing: Float = 0f) = TextStyle(
        fontFamily = family,
        fontWeight = fontWeight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp
    )

    return Typography(
        displayLarge = base(FontWeight.Normal, 57, 64, -0.25f),
        displayMedium = base(FontWeight.Normal, 45, 52, 0f),
        displaySmall = base(FontWeight.Normal, 36, 44, 0f),
        headlineLarge = base(FontWeight.Normal, 32, 40, 0f),
        headlineMedium = base(FontWeight.Normal, 28, 36, 0f),
        headlineSmall = base(FontWeight.Normal, 24, 32, 0f),
        titleLarge = base(FontWeight.Normal, 22, 28, 0f),
        titleMedium = base(FontWeight.Medium, 16, 24, 0.15f),
        titleSmall = base(FontWeight.Medium, 14, 20, 0.1f),
        bodyLarge = base(FontWeight.Normal, 16, 24, 0.5f),
        bodyMedium = base(FontWeight.Normal, 14, 20, 0.25f),
        bodySmall = base(FontWeight.Normal, 12, 16, 0.4f),
        labelLarge = base(FontWeight.Medium, 14, 20, 0.1f),
        labelMedium = base(FontWeight.Medium, 12, 16, 0.5f),
        labelSmall = base(FontWeight.Medium, 11, 16, 0.5f)
    )
}
