import android.view.SurfaceView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel

class FilamentViewModel(
    private val filamentHelper: FilamentHelper
) : ViewModel(
) {

    fun initModels(
        lifecycle: Lifecycle,
        surfaceView: SurfaceView,
    ) {
        filamentHelper.initModels(
            lifecycle,
            surfaceView,
        )
    }
}

