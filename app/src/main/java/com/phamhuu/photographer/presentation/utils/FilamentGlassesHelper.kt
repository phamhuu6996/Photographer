import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.view.Choreographer
import android.view.Surface
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View

// Core refactored structure to support multiple 3D objects using CameraX + MediaPipe + Filament

// Data class for each renderable object
data class RenderableObject(
    val name: String,
    val modelFileName: String,
    val attachPoints: List<Int>,
    var transform: FloatArray = FloatArray(16) { if (it % 5 == 0) 1f else 0f } // identity matrix
)

// Helper to compute transform matrix
object TransformUtils {
    fun createTranslationMatrix(centerX: Float, centerY: Float, z: Float = -3f): FloatArray {
        val tx = centerX / 1280f * 2 - 1
        val ty = -(centerY / 720f * 2 - 1)
        return floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            tx, ty, z, 1f
        )
    }
}

// Object manager to load and transform multiple assets
class ObjectManager(
    private val context: Context,
    private val engine: Engine,
    private val scene: Scene
) {
    private val assets = mutableMapOf<String, FilamentAsset>()

    fun loadObject(obj: RenderableObject) {
        val buffer = context.assets.open(obj.modelFileName).use { it.readBytes() }
        val loader = AssetLoader(engine, UbershaderProvider(engine), EntityManager.get())
        val asset = loader.createAssetFromBinary(ByteBuffer.wrap(buffer))
        val resourceLoader = ResourceLoader(engine)
        resourceLoader.loadResources(asset)

        assets[obj.name] = asset
        scene.addEntities(asset.entities)
    }

    fun updateObjectTransform(obj: RenderableObject) {
        assets[obj.name]?.let { asset ->
            engine.transformManager.setTransform(
                engine.transformManager.getInstance(asset.root),
                obj.transform
            )
        }
    }
}

// Singleton renderer that manages everything
object OverlayRenderer {
    private lateinit var engine: Engine
    private lateinit var view: View
    private lateinit var scene: Scene
    private lateinit var camera: Camera
    private lateinit var renderer: Renderer
    private lateinit var swapChain: SwapChain
    private lateinit var objectManager: ObjectManager
    private val objects = mutableListOf<RenderableObject>()

    fun init(context: Context, surface: Surface) {
        engine = Engine.create()
        view = engine.createView()
        scene = engine.createScene()
        camera = engine.createCamera()
        view.scene = scene
        view.camera = camera
        renderer = engine.createRenderer()
        swapChain = engine.createSwapChain(surface)
        objectManager = ObjectManager(context, engine, scene)

        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (renderer.beginFrame(swapChain)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        })
    }

    fun addObject(context: Context, obj: RenderableObject) {
        objects.add(obj)
        objectManager.loadObject(obj)
    }

    fun updateTransforms(landmarks: List<PointF>) {
        for (obj in objects) {
            val points = obj.attachPoints.map { landmarks[it] }
            val centerX = points.map { it.x }.average().toFloat()
            val centerY = points.map { it.y }.average().toFloat()
            obj.transform = TransformUtils.createTranslationMatrix(centerX, centerY)
            objectManager.updateObjectTransform(obj)
        }
    }
}

// In your MainActivity, after setting up camera & MediaPipe:
// Example usage:
// val glasses = RenderableObject("glasses", "glasses_model.glb", listOf(33, 263))
// val hat = RenderableObject("hat", "hat_model.glb", listOf(10))
// OverlayRenderer.addObject(context, glasses)
// OverlayRenderer.addObject(context, hat)
//
// Call updateTransforms(landmarks) every frame
