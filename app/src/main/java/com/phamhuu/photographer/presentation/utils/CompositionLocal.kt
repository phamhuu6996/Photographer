import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

// Tạo CompositionLocal
val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}