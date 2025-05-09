import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.opengl.Matrix
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.Float3
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

class FilamentViewModel(
    private val filamentHelper: FilamentHelper
) : ViewModel(
) {

    fun initModels(
        lifecycle: Lifecycle,
        surfaceView: SurfaceView,
        context: Context,
        modelPath: List<String>
    ) {
        filamentHelper.initModels(
            lifecycle,
            surfaceView,
            context,
            modelPath
        )
    }

    fun extractGlassesTransform(matrixList: List<FloatArray>?) {
        filamentHelper.extractGlassesTransform(matrixList)
    }
}

