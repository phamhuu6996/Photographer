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
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.Float3
import com.phamhuu.photographer.data.model.ar.RenderableModel
import com.phamhuu.photographer.data.model.ar.ModelInstance
import com.phamhuu.photographer.data.model.ar.FilamentModelManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

class FilamentHelper {
    private val engine: Engine = Engine.create()
    private var swapChain: SwapChain? = null
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val camera: Camera = engine.createCamera(engine.entityManager.create())
    private val models = mutableListOf<FilamentModelManager>()

    private val choreographer = Choreographer.getInstance()
    private val frameScheduler = FrameCallback()

    private val assetLoader: AssetLoader
    private val resourceLoader: ResourceLoader

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            // không gọi vì đang set singleton
//            choreographer.removeFrameCallback(frameScheduler)
//            destroy()
        }
    }

    init {

        // Set up view & camera
        view.scene = scene
        view.camera = camera
        view.blendMode = View.BlendMode.TRANSLUCENT
        scene.skybox = null

        camera.setProjection(45.0, 1.0, 0.1, 100.0, Camera.Fov.VERTICAL) // aspect được cập nhật sau
        camera.lookAt(0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        addDefaultLight()

        val options = renderer.clearOptions
        options.clear = true
        renderer.clearOptions = options

        // Asset loader
        val materialProvider = UbershaderProvider(engine)
        assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
        resourceLoader = ResourceLoader(engine)
    }

    fun initModels(
        lifecycle: Lifecycle,
        surfaceView: SurfaceView,
    ) {
        setUpSurfaceView(surfaceView)
        listenToLifecycle(lifecycle)
    }

    fun addModel3D(path: String, context: Context): ModelInstance? {
        val glassesBuffer = loadGlbAssetFromAssets(path, context)
        glassesBuffer.let {
            val initialTransform = Float3(0f, 0f, -3f)
            return loadModels(
                listOf(RenderableModel(it, initialTransform))
            )
        }
    }

    private fun listenToLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
    }

    private fun setUpSurfaceView(surfaceView: SurfaceView) {
        view.viewport = Viewport(0, 0, surfaceView.width, surfaceView.height)

        makeTransparentBackground(surfaceView)

        // Wait for surface ready
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                swapChain = engine.createSwapChain(holder.surface)
                choreographer.postFrameCallback(frameScheduler)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                choreographer.removeFrameCallback(frameScheduler)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                view.viewport = Viewport(0, 0, width, height)
                camera.setProjection(
                    45.0,
                    width.toDouble() / height,
                    0.1,
                    100.0,
                    Camera.Fov.VERTICAL
                )
            }
        })
    }

    private fun makeTransparentBackground(surfaceView: SurfaceView) {
        surfaceView.setZOrderOnTop(true)
        surfaceView.setBackgroundColor(Color.TRANSPARENT)
        surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    private fun loadGlbAssetFromAssets(assetPath: String, context: Context): ByteBuffer {
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

    private fun loadModels(models: List<RenderableModel>): ModelInstance? {
        for (model in models) {
            val asset = assetLoader.createAsset(model.buffer) ?: continue
            resourceLoader.loadResources(asset)

            // Giả sử `asset` là FilamentAsset bạn load từ gltf
            val width = asset.boundingBox.halfExtent[0] * 2     // Float3

            val rootEntity = asset.root
            val position = model.initialPosition

            val transformManager = engine.transformManager
            val transformInstance = transformManager.getInstance(rootEntity)
            transformManager.setTransform(transformInstance, createTranslationMatrix(position))

            scene.addEntities(asset.entities)
            val instance = ModelInstance(asset, rootEntity, width)
            return instance
        }
        return null
    }

    fun renderFrame(frameTimeNanos: Long) {
        if (swapChain == null) {
            return
        }
        if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    private fun computeRoll(leftEye: Float3, rightEye: Float3): Float {
        val deltaY = rightEye.y - leftEye.y
        val deltaX = rightEye.x - leftEye.x
        val angleRad = atan2(deltaY, deltaX)
        return Math.toDegrees(angleRad.toDouble()).toFloat()
    }

    fun convertToFilamentCoordinatesFloat3(landmark: Float3): Float3 {
        val x = (landmark.x * 2) - 1
        val y = ((1 - landmark.y) * 2) - 1
        val z = (landmark.z * 2) - 1
        return Float3(x, y, z)
    }

    private fun length(p1: Float3, p2: Float3): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dz = p2.z - p1.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun toVector(p1: Float3, p2: Float3): Float3 {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dz = p2.z - p1.z
        return Float3(dx, dy, dz)
    }


    private fun calculateAngleBetweenVectorAndAxis(
        p1: Float3,
        p2: Float3,
        axis: (Float, Float, Float) -> Float
    ): Float {
        val first = convertToFilamentCoordinatesFloat3(p1)
        val second = convertToFilamentCoordinatesFloat3(p2)

        val length = length(first, second)
        if (length == 0f) return 0f
        val vector = toVector(first, second)
        val dot = axis(vector.x, vector.y, vector.z)
        val cosTheta = dot / length
        val safeCosTheta = cosTheta.coerceIn(-1f, 1f)
        val thetaRadians = acos(safeCosTheta)

        return Math.toDegrees(thetaRadians.toDouble()).toFloat()
    }

    /*
    Tính góc giữa vector d và trục Y (j = (0, 1, 0)):

    Cho hai điểm:
        p1(x1, y1, z1)
        p2(x2, y2, z2)

    Vector d:
        d = (dx, dy, dz) = (x2 - x1, y2 - y1, z2 - z1)

    Trục Y:
        j = (0, 1, 0)

    Tích vô hướng giữa d và j:
        d ⋅ j = dx * 0 + dy * 1 + dz * 0 = dy

    Độ dài của vector d:
        |d| = sqrt(dx² + dy² + dz²)

    Độ dài của vector j:
        |j| = 1

    Công thức cos(θ):
        cos(θ) = dy / |d|

    Góc θ (đơn vị độ):
        θ = arccos(dy / |d|) * (180 / π)
    */
    private fun calculateAngleWithYAxis(p1: Float3, p2: Float3): Float {
        return calculateAngleBetweenVectorAndAxis(p1, p2) { dx, _, _ -> dx }
    }

    /*
    Tính góc giữa vector d và trục Y (i = (0, 1, 0)):

    Cho hai điểm:
        p1(x1, y1, z1)
        p2(x2, y2, z2)

    Vector d:
        d = (dx, dy, dz) = (x2 - x1, y2 - y1, z2 - z1)

    Tích vô hướng với trục X:
        d ⋅ i = dx * 0 + dy * 1 + dz * 0 = dx

    Độ dài của vector d:
        |d| = sqrt(dx² + dy² + dz²)

    Độ dài của vector i:
        |i| = 1

    Công thức cos(θ):
        cos(θ) = dy / |d|

    Góc θ (đơn vị độ):
        θ = arccos(dy / |d|) * (180 / π)
    */
    private fun calculateAngleWithXAxis(p1: Float3, p2: Float3): Float {
        return calculateAngleBetweenVectorAndAxis(p1, p2) { _, dy, _ -> dy }
    }

    private fun centerPoint(first: Float3, second: Float3): Float3 {
        // Kiểm tra nếu có đủ dữ liệu điểm landmarks
        val centerX = (first.x + second.x) / 2f
        val centerY = (first.y + second.y) / 2f
        val centerZ = (first.z + second.z) / 2f
        val center = Float3(centerX, centerY, centerZ)
        return center
    }

    private fun createMatrixCompute(translateM: Float3, angleZ: Float, scale: Float): FloatArray {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)
        Matrix.translateM(matrix, 0, translateM.x, translateM.y, translateM.z)
        //Matrix.rotateM(matrix, 0, -angleX, 1f, 0f, 0f)
        //Matrix.rotateM(matrix, 0, ang
        // leY, 0f, 1f, 0f)
        Matrix.rotateM(matrix, 0, (angleZ), 0f, 0f, 1f)
        Matrix.scaleM(matrix, 0, scale, scale, scale)
        return matrix
    }

    private fun createMatrix(first: Float3, second: Float3, scale: Float): FloatArray {
        return createMatrixCompute(
            translateM = centerPoint(first, second),
            angleZ = computeRoll(first, second),
            scale = scale
        )
    }

    fun extractGlassesTransform() {
        if (models.isEmpty()) {
            return
        }

        for (model in models) {
            val transformManager = engine.transformManager
            val modelInstance = model.modelInstance ?: continue
            val first = model.first ?: continue
            val second = model.second ?: continue
            val transformInstance = transformManager.getInstance(modelInstance.rootEntity)
            val matrix = createMatrix(
                first = first,
                second = second,
                scale = length(first, second) / modelInstance.width * 1.2f
            )
            transformManager.setTransform(transformInstance, matrix)
        }
    }


    fun removeEntities() {
        models.forEach { instance ->
            if (instance.modelInstance != null) {
                scene.removeEntity(instance.modelInstance!!.rootEntity)
                assetLoader.destroyAsset(instance.modelInstance!!.asset)
            }
        }
        models.clear()
    }

    fun addModel(model: FilamentModelManager) {
        models.add(model)
    }

    fun setVisibility(model: FilamentModelManager, visible: Boolean) {
        val modelInstance = model.modelInstance ?: return
        view.setVisibleLayers(modelInstance.rootEntity, if (visible) 1 else 0)
    }


    fun destroy() {

        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)
        engine.destroy()
    }

    private fun createTranslationMatrix(position: Float3): FloatArray {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)
        Matrix.translateM(matrix, 0, position.x, position.y, position.z)
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

