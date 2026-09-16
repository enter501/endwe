package tw.bailudangruo.endwe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WeatherColorScheme = darkColorScheme(
    primary = Color(0xFF61B7FF),
    onPrimary = Color(0xFF002F4E),
    primaryContainer = Color(0xFF0B4365),
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFFFD166),
    background = Color(0xFF07121F),
    onBackground = Color(0xFFE5F2FF),
    surface = Color(0xFF0D1C2A),
    onSurface = Color(0xFFE5F2FF),
    surfaceVariant = Color(0xFF172A3A),
    onSurfaceVariant = Color(0xFFB9CCDC),
    error = Color(0xFFFF8A80),
)

@Composable
fun EndweTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WeatherColorScheme,
        content = content,
    )
}
