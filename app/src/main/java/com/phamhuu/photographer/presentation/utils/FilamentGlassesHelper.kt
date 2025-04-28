import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.opengl.Matrix
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.constraintlayout.widget.ConstraintSet
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
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.Mat4
import com.google.android.filament.utils.Quaternion
import com.google.android.filament.utils.cross
import com.google.android.filament.utils.normalize
import com.google.android.material.math.MathUtils
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
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

    fun computeRoll(leftEye: NormalizedLandmark, rightEye: NormalizedLandmark): Float {
        val deltaY = rightEye.y() - leftEye.y()
        val deltaX = rightEye.x() - leftEye.x()
        val angleRad = atan2(deltaY, deltaX)
        return Math.toDegrees(angleRad.toDouble()).toFloat()
    }

    fun computePitch(forehead: NormalizedLandmark, chin: NormalizedLandmark): Float {
        // Tạo vector giữa trán và cằm
        val vectorPitch = Float3(
            (chin.x() - forehead.x() -0.5f) * 2,
            (chin.y() - forehead.y() -0.5f) * 2,
            (chin.y() - forehead.y() -0.5f) * 2,
        )

        // Trục Y chuẩn (chiều xuống là -1)
        val up = Float3(0f, 1f, 0f)

        // Tính cosTheta từ dot product và độ dài của các vector
        val cosThetaPitch = (vectorPitch.x * up.x + vectorPitch.y * up.y + vectorPitch.z * up.z) /
                (vectorPitch.length() * up.length())

        // Tính góc Pitch (ngửa/cúi) theo radian và đổi ra độ
        val pitchRadian = acos(cosThetaPitch.toDouble())
        return Math.toDegrees(pitchRadian).toFloat()
    }

    fun computeYaw(nose: NormalizedLandmark, forehead: NormalizedLandmark): Float {
        // Tạo vector giữa mũi và giữa 2 mắt
        val vectorYaw = Float3(
            (nose.x() - forehead.x() -0.5f) * 2,
            (nose.y() - forehead.y() -0.5f) * 2,
            (nose.z() - forehead.z() -0.5f) * 2,
        )

        // Trục Z chuẩn (chiều ra vào là -1)
        val forward = Float3(0f, 0f, 1f)

        // Tính cosTheta từ dot product và độ dài của các vector
        val cosThetaYaw = (vectorYaw.x * forward.x + vectorYaw.y * forward.y + vectorYaw.z * forward.z) /
                (vectorYaw.length() * forward.length())

        // Tính góc Yaw (quay trái/phải) theo radian và đổi ra độ
        val yawRadian = Math.acos(cosThetaYaw.toDouble())
        return Math.toDegrees(yawRadian).toFloat()
    }



    fun extractGlassesTransform(result: FaceLandmarkerHelper.ResultBundle?) {
        val face = result?.result?.faceLandmarks()?.firstOrNull() ?: return
        val leftEye = face.getOrNull(33)        ?: return
        val rightEye = face.getOrNull(263)        ?: return
        val chin = face.getOrNull(152)        ?: return
        val forehead = face.getOrNull(10)        ?: return
        val nose = face.getOrNull(1)        ?: return

        // Kiểm tra nếu có đủ dữ liệu điểm landmarks
        val centerX = (leftEye.x() + rightEye.x()) / 2f
        val centerY = (leftEye.y() + rightEye.y()) / 2f
        val centerZ = (leftEye.z() + rightEye.z()) / 2f

            val x = (centerX - 0.5f) * 2f
            val y = (centerY - 0.5f) * 2f
            val z = -3f

            val dx = rightEye.x() - leftEye.x()
            val dy = rightEye.y() - leftEye.y()
            val scale = sqrt(dx * dx + dy * dy)

        // 2. Tính hướng nhìn (forward vector)
        val forward = normalize(Float3(
            chin.x() - forehead.x(),
            chin.y() - forehead.y(),
            chin.z() - forehead.z()
        ))


        // 3. Trục ngang (right vector)
        val horizontal = normalize(Float3(
            rightEye.x() - leftEye.x(),
            rightEye.y() - leftEye.y(),
            rightEye.z() - leftEye.z()
        ))


        // 4. Trục lên (up vector) = right × forward
        val up = normalize(cross(horizontal, forward))

        // Tính góc quay X: Quay quanh trục X
        val angleX = computeYaw(nose, forehead)

        // Tính góc quay Y: Quay quanh trục Y
        val angleY = computePitch(forehead, chin)

        // Tính góc quay Z: Quay quanh trục Z
        val angleZ = computeRoll(        leftEye, rightEye
        )

        for ((index, model) in modelInstances.withIndex()) {
            val transformManager = engine.transformManager
            val transformInstance = transformManager.getInstance(model.rootEntity)

            println("llslsls$angleX")
            println("llslsls$angleY")

            val matrix = FloatArray(16)
            Matrix.setIdentityM(matrix, 0)
            Matrix.translateM(matrix, 0, x, y, z)
            Matrix.rotateM(matrix, 0, 0f, 1f, 0f, 0f)
            Matrix.rotateM(matrix, 0, angleY.toFloat(), 0f, 1f, 0f)
            Matrix.rotateM(matrix, 0, -(angleZ), 0f, 0f, 1f)
            Matrix.scaleM(matrix, 0, scale, scale, scale)
            transformManager.setTransform(transformInstance, matrix)
        }
    }


    /**
     * Cập nhật vị trí và tỷ lệ cho kính.
     */
//    fun updateModelPositionsAndScales(data: List<Pair<FloatArray, Float>>) {
//        val transformManager = engine.transformManager
//
//        for ((index, model) in modelInstances.withIndex()) {
//            if (index >= data.size) break
//
//            val (position, scale) = data[index]
//            val transformInstance = transformManager.getInstance(model.rootEntity)
//
//            val matrix = FloatArray(16)
//            Matrix.setIdentityM(matrix, 0)
//            Matrix.translateM(matrix, 0, position[0], position[1], position[2])
//            Matrix.scaleM(matrix, 0, scale, scale, scale)
//
//            transformManager.setTransform(transformInstance, matrix)
//        }
//    }

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

    private fun Float3.length(): Float {
        return Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }
}

