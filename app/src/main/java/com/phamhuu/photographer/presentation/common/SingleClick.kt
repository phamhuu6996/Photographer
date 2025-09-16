import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Modifier cho phép click đúng 1 lần cho tới khi Composable bị hủy.
 * Có thể chain với các modifier khác.
 */

fun Modifier.singleShotClick(onClick: () -> Unit): Modifier = composed {
    var clicked by remember { mutableStateOf(false) }

    // Trả về 1 Modifier (không trả về factory)
    Modifier.clickable(enabled = !clicked) {
        if (!clicked) {
            clicked = true
            onClick()
        }
    }
}