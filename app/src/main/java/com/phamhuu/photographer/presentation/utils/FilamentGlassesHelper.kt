package com.example.filamentglasses

import android.renderscript.Matrix4f
import android.view.Surface
import android.view.SurfaceView
import android.view.Choreographer
import com.google.android.filament.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FilamentGlassesHelper(
    private val surfaceView: SurfaceView,
    private val previewWidth: Int,
    private val previewHeight: Int,
    private val loadMaterialBuffer: () -> ByteBuffer
) {

    private lateinit var engine: Engine
    private lateinit var scene: Scene
    private lateinit var renderer: Renderer
    private lateinit var view: View
    private lateinit var swapChain: SwapChain
    private lateinit var camera: Camera
    private lateinit var material: MaterialInstance

    private var eyeLeftEntity = 0
    private var eyeRightEntity = 0

    fun init() {
        surfaceView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                setupFilament(holder.surface)
            }

            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {}
        })
    }

    private fun setupFilament(surface: Surface) {
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())
        swapChain = engine.createSwapChain(surface)

        material = Material.Builder()
            .payload(loadMaterialBuffer(), loadMaterialBuffer().remaining())
            .build(engine)
            .createInstance()

        // Tạo hai vòng tròn (mắt kính)
        eyeLeftEntity = createRingEntity()
        eyeRightEntity = createRingEntity()
        scene.addEntity(eyeLeftEntity)
        scene.addEntity(eyeRightEntity)

        view.scene = scene
        view.camera = camera

        startRenderLoop()
    }

    private fun startRenderLoop() {
        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (::renderer.isInitialized && ::swapChain.isInitialized && ::view.isInitialized) {
                    if (renderer.beginFrame(swapChain, 1000000L)) {
                        renderer.render(view)
                        renderer.endFrame()
                    }
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        })
    }

    fun updateLandmarks(leftX: Float, leftY: Float, rightX: Float, rightY: Float) {
        val left = normalizeToWorld(leftX, leftY)
        val right = normalizeToWorld(rightX, rightY)
        updateEntityTransform(eyeLeftEntity, left[0], left[1], -2f)
        updateEntityTransform(eyeRightEntity, right[0], right[1], -2f)
    }

    private fun normalizeToWorld(x: Float, y: Float): FloatArray {
        val nx = (x / previewWidth - 0.5f) * 2f
        val ny = -(y / previewHeight - 0.5f) * 2f
        return floatArrayOf(nx, ny)
    }

    private fun updateEntityTransform(entity: Int, x: Float, y: Float, z: Float) {
        val tm = engine.transformManager
        val transform = Matrix4f().apply {
            translation(x, y, z)
        }
        tm.setTransform(tm.getInstance(entity), transform.toFloatArray())
    }

    private fun createRingEntity(): Int {
        val entity = EntityManager.get().create()
        val (vb, ib) = createRingGeometry()

        RenderableManager.Builder(1)
            .geometry(0, RenderableManager.PrimitiveType.LINE_LOOP, vb, ib)
            .material(0, material)
            .build(engine, entity)

        return entity
    }

    private fun createRingGeometry(radius: Float = 0.12f, segments: Int = 32): Pair<VertexBuffer, IndexBuffer> {
        val vertexCount = segments
        val indexCount = segments

        val vb = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(vertexCount)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT2, 0, 8)
            .build(engine)

        val vertexData = ByteBuffer.allocateDirect(vertexCount * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (i in 0 until segments) {
            val angle = (2.0 * Math.PI * i) / segments
            vertexData.put((radius * Math.cos(angle)).toFloat())
            vertexData.put((radius * Math.sin(angle)).toFloat())
        }
        vertexData.rewind()
        vb.setBufferAt(engine, 0, vertexData)

        val ib = IndexBuffer.Builder()
            .indexCount(indexCount)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)

        val indexData = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        for (i in 0 until segments) indexData.put(i.toShort())
        indexData.rewind()
        ib.setBuffer(engine, indexData)

        return Pair(vb, ib)
    }
}
