import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.opengl.Matrix
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.clearCompositionErrors
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt


data class RenderableModel(
    val buffer: ByteBuffer,
    val initialPosition: FloatArray
)

data class ModelInstance(
    val asset: FilamentAsset,
    val rootEntity: Int
)

class FilamentHelper(
    private val context: Context,
    private val surfaceView: SurfaceView,
    private val lifecycle: Lifecycle
) {
    private val engine: Engine
    private lateinit var swapChain: SwapChain
    private val renderer: Renderer
    private val scene: Scene
    private val view: View
    private val camera: Camera
    private val modelInstances = mutableListOf<ModelInstance>()

    private val choreographer = Choreographer.getInstance()
    private val frameScheduler = FrameCallback()

    private val assetLoader: AssetLoader
    private val resourceLoader: ResourceLoader

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            choreographer.postFrameCallback(frameScheduler)
        }

        override fun onPause(owner: LifecycleOwner) {
            choreographer.removeFrameCallback(frameScheduler)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            choreographer.removeFrameCallback(frameScheduler)
            destroy()
        }
    }

    init {
        lifecycle.addObserver(lifecycleObserver)

        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())

        // Set up view & camera
        view.scene = scene
        view.camera = camera

        camera.setProjection(45.0, 1.0, 0.1, 100.0, Camera.Fov.VERTICAL) // aspect được cập nhật sau
        camera.lookAt(0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

        view.viewport = Viewport(0, 0, surfaceView.width, surfaceView.height)

        makeTransparentBackground()
        addDefaultLight()

        // Asset loader
        val materialProvider = UbershaderProvider(engine)
        assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
        resourceLoader = ResourceLoader(engine)

        // Wait for surface ready
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                swapChain = engine.createSwapChain(holder.surface)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                destroy()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                camera.setProjection(45.0, width.toDouble() / height, 0.1, 100.0, Camera.Fov.VERTICAL)
            }
        })
    }

    private fun makeTransparentBackground() {
        surfaceView.setZOrderOnTop(true)
        surfaceView.setBackgroundColor(Color.TRANSPARENT)
        surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT)

        view.blendMode = View.BlendMode.TRANSLUCENT
        scene.skybox = null

        val options = renderer.clearOptions
        options.clear = true
        renderer.clearOptions = options
    }

    fun loadGlbAssetFromAssets(assetPath: String): ByteBuffer {
        val assetManager = context.assets
        assetManager.open(assetPath).use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            return ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }
        }
    }

    fun loadModels(models: List<RenderableModel>) {
        modelInstances.clear()

        for (model in models) {
            val asset = assetLoader.createAsset(model.buffer) ?: continue
            resourceLoader.loadResources(asset)

            val rootEntity = asset.root
            val position = model.initialPosition

            val transformManager = engine.transformManager
            val transformInstance = transformManager.getInstance(rootEntity)
            transformManager.setTransform(transformInstance, createTranslationMatrix(position))

            scene.addEntities(asset.entities)
            modelInstances.add(ModelInstance(asset, rootEntity))
        }
    }

    fun renderFrame(frameTimeNanos: Long) {
        if (renderer.beginFrame(swapChain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun extractGlassesTransform(result: FaceLandmarkerHelper.ResultBundle?): Pair<FloatArray, Float>? {
        val face = result?.result?.faceLandmarks()?.firstOrNull() ?: return null
        val left = face.getOrNull(33)
        val right = face.getOrNull(263)

        // Kiểm tra nếu có đủ dữ liệu điểm landmarks
        if (left != null && right != null) {
            val centerX = (left.x() + right.x()) / 2f
            val centerY = (left.y() + right.y()) / 2f
            val centerZ = (left.z() + right.z()) / 2f

            val x = (centerX - 0.5f) * 2f
            val y = -(centerY - 0.5f) * 2f
            val z = -3f

            val dx = right.x() - left.x()
            val dy = right.y() - left.y()
            val scale = sqrt(dx * dx + dy * dy)

            return floatArrayOf(x, y, z) to scale
        }

        // Nếu không có đủ dữ liệu, trả về null
        return null
    }

    /**
     * Cập nhật vị trí và tỷ lệ cho kính.
     */
    fun updateModelPositionsAndScales(data: List<Pair<FloatArray, Float>>) {
        val transformManager = engine.transformManager

        for ((index, model) in modelInstances.withIndex()) {
            if (index >= data.size) break

            val (position, scale) = data[index]
            val transformInstance = transformManager.getInstance(model.rootEntity)

            val matrix = FloatArray(16)
            Matrix.setIdentityM(matrix, 0)
            Matrix.translateM(matrix, 0, position[0], position[1], position[2])
            Matrix.scaleM(matrix, 0, scale, scale, scale)

            transformManager.setTransform(transformInstance, matrix)
        }
    }

    fun destroy() {
        modelInstances.forEach { instance ->
            assetLoader.destroyAsset(instance.asset)
        }
        modelInstances.clear()

        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)
        engine.destroy()
    }

    private fun createTranslationMatrix(position: FloatArray): FloatArray {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)
        Matrix.translateM(matrix, 0, position[0], position[1], position[2])
        return matrix
    }

    private fun addDefaultLight() {
        val light = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 1.0f, 1.0f)
            .intensity(100_000.0f)
            .direction(0.0f, -0.0f, -1.0f)
            .castShadows(false)
            .build(engine, light)
        scene.addEntity(light)
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            renderFrame(frameTimeNanos)
        }
    }
}

