package com.lorran.lupanutri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorran.lupanutri.domain.Band

// ---- Tokens de cor ----
private val Folha = Color(0xFF14513A)
private val FolhaClara = Color(0xFF8FD3AA)
private val Creme = Color(0xFFF7F4EC)
private val Lima = Color(0xFFB8E07A)

private val LightColors = lightColorScheme(
    primary = Folha,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE8D5),
    onPrimaryContainer = Color(0xFF0B2E20),
    secondary = Color(0xFF5B6B2E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EEC4),
    onSecondaryContainer = Color(0xFF1C2408),
    tertiary = Color(0xFF8A4B14),
    background = Creme,
    onBackground = Color(0xFF1B1F1C),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF1B1F1C),
    surfaceVariant = Color(0xFFECE7DA),
    onSurfaceVariant = Color(0xFF4A524C),
    surfaceContainer = Color(0xFFF0ECE1),
    surfaceContainerHigh = Color(0xFFEAE5D8),
    outline = Color(0xFF7A827C),
    outlineVariant = Color(0xFFD6D0C2),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = FolhaClara,
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF1E5A40),
    onPrimaryContainer = Color(0xFFCFE8D5),
    secondary = Lima,
    onSecondary = Color(0xFF243300),
    secondaryContainer = Color(0xFF384A12),
    onSecondaryContainer = Color(0xFFE3EEC4),
    tertiary = Color(0xFFF0B97E),
    background = Color(0xFF101512),
    onBackground = Color(0xFFE3E8E3),
    surface = Color(0xFF151B17),
    onSurface = Color(0xFFE3E8E3),
    surfaceVariant = Color(0xFF232C26),
    onSurfaceVariant = Color(0xFFB9C4BC),
    surfaceContainer = Color(0xFF1B231E),
    surfaceContainerHigh = Color(0xFF232C26),
    outline = Color(0xFF85908A),
    outlineVariant = Color(0xFF38423C),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

// ---- Espaçamento (múltiplos de 4 dp) ----
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// ---- Tipografia ----
private val Base = FontFamily.Default
private val LupaType = Typography(
    displaySmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 44.sp),
    headlineSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
)

private val LupaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

// ---- Cores das faixas de nota ----
data class BandColors(val main: Color, val container: Color, val onContainer: Color)

@Composable
@ReadOnlyComposable
fun bandColors(band: Band?): BandColors {
    val dark = isSystemInDarkTheme()
    return when (band) {
        Band.OTIMO -> if (dark) BandColors(Color(0xFF5FD38F), Color(0xFF123B25), Color(0xFFBDF2D1))
        else BandColors(Color(0xFF1E8E4E), Color(0xFFD5F2E0), Color(0xFF0A3A1F))
        Band.BOM -> if (dark) BandColors(Color(0xFFC3DE5A), Color(0xFF2E3A0C), Color(0xFFE6F3B5))
        else BandColors(Color(0xFF7A9A12), Color(0xFFEAF3C8), Color(0xFF2C3906))
        Band.RUIM -> if (dark) BandColors(Color(0xFFF5A35A), Color(0xFF45280B), Color(0xFFFFDDBE))
        else BandColors(Color(0xFFD9701A), Color(0xFFFFE4CC), Color(0xFF4F2600))
        Band.EVITE -> if (dark) BandColors(Color(0xFFFF8A80), Color(0xFF4A1512), Color(0xFFFFD9D5))
        else BandColors(Color(0xFFC8352B), Color(0xFFFADAD6), Color(0xFF4F0D08))
        null -> if (dark) BandColors(Color(0xFF85908A), Color(0xFF232C26), Color(0xFFD5DDD7))
        else BandColors(Color(0xFF7A827C), Color(0xFFECE7DA), Color(0xFF2E3530))
    }
}

@Composable
fun LupaTheme(content: @Composable () -> Unit) {
    val colors: ColorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = LupaType, shapes = LupaShapes, content = content)
}
