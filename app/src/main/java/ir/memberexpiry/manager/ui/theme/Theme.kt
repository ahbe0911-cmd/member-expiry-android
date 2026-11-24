package ir.memberexpiry.manager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.memberexpiry.manager.R

private val Light = lightColorScheme(
    primary = Color(0xFF076B57), onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F1E5), onPrimaryContainer = Color(0xFF004638),
    secondary = Color(0xFF4D635B), secondaryContainer = Color(0xFFDCEBE4),
    tertiary = Color(0xFF855400), tertiaryContainer = Color(0xFFFFE7BA),
    background = Color(0xFFF3F7F5), onBackground = Color(0xFF172C26),
    surface = Color.White, onSurface = Color(0xFF172C26), onSurfaceVariant = Color(0xFF43564F),
    surfaceVariant = Color(0xFFE1EAE5), outline = Color(0xFF74877D)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF7ED9BC), onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005140), onPrimaryContainer = Color(0xFFB0F1D9),
    secondary = Color(0xFFB5CBBF), secondaryContainer = Color(0xFF33483D),
    tertiary = Color(0xFFFFC76D), tertiaryContainer = Color(0xFF614000),
    background = Color(0xFF111C18), surface = Color(0xFF182720),
    onBackground = Color(0xFFE2EEE7), onSurface = Color(0xFFE2EEE7),
    surfaceVariant = Color(0xFF2A3B33), onSurfaceVariant = Color(0xFFC2D0C7)
)
private val Family = FontFamily(Font(R.font.vazirmatn_regular, FontWeight.Normal), Font(R.font.vazirmatn_bold, FontWeight.Bold))
private val Default = Typography()
private val PersianTypography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = Family), displayMedium = Default.displayMedium.copy(fontFamily = Family), displaySmall = Default.displaySmall.copy(fontFamily = Family),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Family), headlineMedium = Default.headlineMedium.copy(fontFamily = Family), headlineSmall = Default.headlineSmall.copy(fontFamily = Family),
    titleLarge = Default.titleLarge.copy(fontFamily = Family, fontWeight = FontWeight.Bold), titleMedium = Default.titleMedium.copy(fontFamily = Family, fontWeight = FontWeight.Bold), titleSmall = Default.titleSmall.copy(fontFamily = Family),
    bodyLarge = Default.bodyLarge.copy(fontFamily = Family), bodyMedium = Default.bodyMedium.copy(fontFamily = Family), bodySmall = Default.bodySmall.copy(fontFamily = Family),
    labelLarge = Default.labelLarge.copy(fontFamily = Family), labelMedium = Default.labelMedium.copy(fontFamily = Family), labelSmall = Default.labelSmall.copy(fontFamily = Family)
)
@Composable
fun MemberExpiryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, typography = PersianTypography, content = content)
}
