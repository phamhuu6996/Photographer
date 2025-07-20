package com.phamhuu.photographer.presentation.utils

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.camera.core.ImageProxy
import com.phamhuu.photographer.enums.ImageFilter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FilterRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var textureId = 0
    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    
    // ✅ Thread-safe filter management
    private val currentFilter = AtomicReference(ImageFilter.NONE)
    private val pendingFilter = AtomicReference<ImageFilter?>(null)
    private val needsShaderUpdate = AtomicBoolean(false)

    private var surfaceWidth = 0
    private var surfaceHeight = 0
    
    // ✅ Image data management
    private val currentImageBuffer = AtomicReference<ByteBuffer?>(null)
    private val imageWidth = AtomicReference(0)
    private val imageHeight = AtomicReference(0)
    private val hasNewImageData = AtomicBoolean(false)
    
    // ✅ Rotation handling
    private val currentRotation = AtomicReference(0)
    private val needsVertexUpdate = AtomicBoolean(false)
    
    // ✅ Initialization state
    private val isRendererReady = AtomicBoolean(false)
    
    // Capture callback
    private var captureCallback: ((Bitmap) -> Unit)? = null
    
    // Vertex shader
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTextureCoord;
        varying vec2 vTextureCoord;
        
        void main() {
            gl_Position = aPosition;
            vTextureCoord = aTextureCoord;
        }
    """
    
    // Default fragment shader
    private val defaultFragmentShaderCode = """
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform sampler2D uTexture;
        
        void main() {
            gl_FragColor = texture2D(uTexture, vTextureCoord);
        }
    """
    
    // ✅ Base vertex data cho full screen quad (sẽ được rotate dynamically)
    private val baseQuadVertices = floatArrayOf(
        // positions    // texture coords
        -1.0f, -1.0f,   0.0f, 1.0f,  // Bottom-left
         1.0f, -1.0f,   1.0f, 1.0f,  // Bottom-right  
         1.0f,  1.0f,   1.0f, 0.0f,  // Top-right
        -1.0f,  1.0f,   0.0f, 0.0f   // Top-left
    )
    
    private val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        println("🔥 FilterRenderer onSurfaceCreated")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // Initialize buffers với base vertices
        updateVertexBuffer(baseQuadVertices)
        
        val ib = ByteBuffer.allocateDirect(indices.size * 2)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(indices)
        indexBuffer?.position(0)
        
        // Create texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        
        println("🔥 Created texture ID: $textureId")
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        // ✅ Create placeholder texture để avoid black screen
//        createPlaceholderTexture()
        
        // Create initial shader program
        createShaderProgramSafe(currentFilter.get())
        
        // ✅ Mark renderer as ready
        isRendererReady.set(true)
        println("🔥 FilterRenderer initialization complete")
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        println("🔥 FilterRenderer onSurfaceChanged: ${width}x${height}")
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // ✅ Handle pending filter changes on GL thread
        if (needsShaderUpdate.compareAndSet(true, false)) {
            pendingFilter.get()?.let { newFilter ->
                try {
                    createShaderProgramSafe(newFilter)
                    currentFilter.set(newFilter)
                    println("🔥 Filter updated to: ${newFilter.displayName}")
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("❌ Filter update failed: ${e.message}")
                    // Fallback to default shader if filter fails
                    createShaderProgramSafe(ImageFilter.NONE)
                    currentFilter.set(ImageFilter.NONE)
                }
                pendingFilter.set(null)
            }
        }
        
        // ✅ Handle rotation changes by updating vertex buffer
        if (needsVertexUpdate.compareAndSet(true, false)) {
            val rotation = currentRotation.get()
            val rotatedVertices = getRotatedVertices(rotation)
            updateVertexBuffer(rotatedVertices)
            println("🔥 Vertex buffer updated for rotation: ${rotation}°")
        }
        
        // ✅ Update texture với real camera data
        if (hasNewImageData.compareAndSet(true, false)) {
            currentImageBuffer.get()?.let { buffer ->
                try {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                    buffer.position(0)
                    
                    GLES20.glTexImage2D(
                        GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                        imageWidth.get(), imageHeight.get(), 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                        buffer
                    )
                    
                    val error = GLES20.glGetError()
                    if (error != GLES20.GL_NO_ERROR) {
                        println("❌ GL Error during texture update: $error")
                    } else {
                        println("🔥 Texture updated with camera data: ${imageWidth.get()}x${imageHeight.get()}")
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("❌ Texture update failed: ${e.message}")
                }
            }
        }
        
        // Skip rendering if no program or texture
        if (program == 0 || textureId == 0) {
            println("❌ Cannot render: program=$program, textureId=$textureId")
            return
        }
        
        // ✅ Always render - either camera data or placeholder
        
        // Use shader program
        GLES20.glUseProgram(program)
        
        // Get attribute locations
        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val textureCoordHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        val textureUniformHandle = GLES20.glGetUniformLocation(program, "uTexture")
        
        // Skip if handles are invalid
        if (positionHandle < 0 || textureCoordHandle < 0 || textureUniformHandle < 0) {
            println("❌ Invalid handles: pos=$positionHandle, tex=$textureCoordHandle, uniform=$textureUniformHandle")
            return
        }
        
        // Enable vertex attributes
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        
        // Set vertex attributes
        vertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 4 * 4, vertexBuffer)
        
        vertexBuffer?.position(2)
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 4 * 4, vertexBuffer)
        
        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureUniformHandle, 0)
        
        // Draw
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        
        // Cleanup
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)
        
        // Handle capture request
        captureCallback?.let { callback ->
            captureCurrentFrameSafe(callback)
            captureCallback = null
        }
        
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            println("❌ OpenGL error: $error")
        }
    }
    
    // ✅ Create placeholder texture to avoid black screen
    private fun createPlaceholderTexture() {
        val width = 640
        val height = 480
        val placeholderBuffer = createTestPattern(width, height)
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
            placeholderBuffer
        )
        
        println("🔥 Placeholder texture created: ${width}x${height}")
    }
    
    // ✅ Rotation-aware vertex coordinates
    private fun getRotatedVertices(rotationDegrees: Int): FloatArray {
        return when (rotationDegrees) {
            90 -> floatArrayOf(
                // positions    // texture coords (rotated 90° CW)
                -1.0f, -1.0f,   1.0f, 1.0f,  // Bottom-left
                 1.0f, -1.0f,   1.0f, 0.0f,  // Bottom-right  
                 1.0f,  1.0f,   0.0f, 0.0f,  // Top-right
                -1.0f,  1.0f,   0.0f, 1.0f   // Top-left
            )
            180 -> floatArrayOf(
                // positions    // texture coords (rotated 180°)
                -1.0f, -1.0f,   1.0f, 0.0f,  // Bottom-left
                 1.0f, -1.0f,   0.0f, 0.0f,  // Bottom-right  
                 1.0f,  1.0f,   0.0f, 1.0f,  // Top-right
                -1.0f,  1.0f,   1.0f, 1.0f   // Top-left
            )
            270 -> floatArrayOf(
                // positions    // texture coords (rotated 270° CW)
                -1.0f, -1.0f,   0.0f, 0.0f,  // Bottom-left
                 1.0f, -1.0f,   0.0f, 1.0f,  // Bottom-right  
                 1.0f,  1.0f,   1.0f, 1.0f,  // Top-right
                -1.0f,  1.0f,   1.0f, 0.0f   // Top-left
            )
            else -> baseQuadVertices // 0° or default
        }
    }
    
    // ✅ Update vertex buffer with new coordinates
    private fun updateVertexBuffer(vertices: FloatArray) {
        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer?.put(vertices)
        vertexBuffer?.position(0)
    }

    // ✅ Fixed ImageProxy processing với rotation handling
    fun updateImage(imageProxy: ImageProxy) {
            val width = imageProxy.width
            val height = imageProxy.height
            
            // ✅ Extract rotation from ImageProxy
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            // ✅ Update rotation if changed
            if (currentRotation.get() != rotationDegrees) {
                currentRotation.set(rotationDegrees)
                needsVertexUpdate.set(true)
                println("🔥 Rotation changed to: ${rotationDegrees}°")
            }
            
            println("🔥 Available planes: ${imageProxy.planes.size}")

            val buffer = imageProxy.planes[0].buffer
            val rgbaBytes = ByteArray(buffer.remaining())
            
            // ✅ Try to process real camera data, fallback to test pattern
            val rgbaBuffer = buffer.get(rgbaBytes)
            
            // Store data atomically
            currentImageBuffer.set(rgbaBuffer)
            imageWidth.set(width)
            imageHeight.set(height)
            hasNewImageData.set(true)
            
            println("🔥 Image data stored: ${width}x${height}, buffer size: ${rgbaBuffer.remaining()}, rotation: ${rotationDegrees}°")
    }
    
    // ✅ Create simple test pattern for debugging
    private fun createTestPattern(width: Int, height: Int): ByteBuffer {
        val rgbaData = ByteArray(width * height * 4)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = (y * width + x) * 4
                
                // Create checker pattern với different colors
                val isChecker = ((x / 50) + (y / 50)) % 2 == 0
                
                if (isChecker) {
                    rgbaData[index] = 255.toByte()     // R - white
                    rgbaData[index + 1] = 255.toByte() // G - white
                    rgbaData[index + 2] = 255.toByte() // B - white
                } else {
                    rgbaData[index] = 100.toByte()     // R - gray
                    rgbaData[index + 1] = 100.toByte() // G - gray
                    rgbaData[index + 2] = 100.toByte() // B - gray
                }
                rgbaData[index + 3] = 255.toByte()     // A - opaque
            }
        }
        
        val buffer = ByteBuffer.allocateDirect(rgbaData.size)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(rgbaData)
        buffer.position(0)
        
        println("🔥 Created test pattern: ${width}x${height}")
        return buffer
    }
    
    // ✅ Thread-safe filter setting
    fun setFilter(filter: ImageFilter) {
        println("🔥 Setting filter: ${filter.displayName}")
        if (currentFilter.get() != filter) {
            pendingFilter.set(filter)
            needsShaderUpdate.set(true)
        }
    }
    
    // ✅ Check if renderer is ready for operations
    fun isReady(): Boolean {
        return isRendererReady.get()
    }
    
    fun captureFilteredImage(callback: (Bitmap) -> Unit) {
        captureCallback = callback
    }
    
    // ✅ Cleanup OpenGL resources
    fun release() {
        try {
            if (program != 0) {
                GLES20.glDeleteProgram(program)
                program = 0
            }
            if (textureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                textureId = 0
            }
            
            // Clear all references
            currentImageBuffer.set(null)
            captureCallback = null
            
            println("🔥 FilterRenderer resources released")
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Error releasing FilterRenderer resources: ${e.message}")
        }
    }

    private fun captureCurrentFrameSafe(callback: (Bitmap) -> Unit) {
        try {
            val pixels = IntArray(surfaceWidth * surfaceHeight)
            val buffer = IntBuffer.wrap(pixels)
            
            GLES20.glReadPixels(
                0, 0, surfaceWidth, surfaceHeight,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer
            )
            
            // Create bitmap and flip vertically
            val bitmap = Bitmap.createBitmap(surfaceWidth, surfaceHeight, Bitmap.Config.ARGB_8888)
            val flippedPixels = IntArray(pixels.size)
            
            for (y in 0 until surfaceHeight) {
                for (x in 0 until surfaceWidth) {
                    val srcIndex = y * surfaceWidth + x
                    val dstIndex = (surfaceHeight - 1 - y) * surfaceWidth + x
                    flippedPixels[dstIndex] = pixels[srcIndex]
                }
            }
            
            bitmap.setPixels(flippedPixels, 0, surfaceWidth, 0, 0, surfaceWidth, surfaceHeight)
            callback(bitmap)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createShaderProgramSafe(filter: ImageFilter) {
        try {
            println("🔥 Creating shader program for filter: ${filter.displayName}")
            
            // Delete old program safely
            if (program != 0) {
                GLES20.glDeleteProgram(program)
                program = 0
            }
            
            val fragmentShader = if (filter.fragmentShader.isNotEmpty()) {
                // Use regular sampler2D (không phải external OES)
                filter.fragmentShader.replace(
                    "uniform samplerExternalOES uTexture;",
                    "uniform sampler2D uTexture;"
                ).replace(
                    "#extension GL_OES_EGL_image_external : require\n",
                    ""
                )
            } else {
                defaultFragmentShaderCode
            }
            
            val vertexShader = loadShaderSafe(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShaderCompiled = loadShaderSafe(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
            
            if (vertexShader == 0 || fragmentShaderCompiled == 0) {
                throw RuntimeException("Failed to load shaders: vertex=$vertexShader, fragment=$fragmentShaderCompiled")
            }
            
            program = GLES20.glCreateProgram()
            if (program == 0) {
                throw RuntimeException("Failed to create program")
            }
            
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShaderCompiled)
            GLES20.glLinkProgram(program)
            
            // Check link status
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val error = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                program = 0
                throw RuntimeException("Error linking program: $error")
            }
            
            // Clean up shaders
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShaderCompiled)
            
            println("🔥 Shader program created successfully for filter: ${filter.displayName}, program ID: $program")
            
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Shader creation failed: ${e.message}")
            // Fallback to default if anything fails
            if (filter != ImageFilter.NONE) {
                createShaderProgramSafe(ImageFilter.NONE)
            }
        }
    }
    
    private fun loadShaderSafe(type: Int, shaderCode: String): Int {
        return try {
            val shader = GLES20.glCreateShader(type)
            if (shader == 0) {
                println("❌ Failed to create shader of type: $type")
                return 0
            }
            
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            
            // Check compilation status
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val error = GLES20.glGetShaderInfoLog(shader)
                println("❌ Shader compilation failed: $error")
                GLES20.glDeleteShader(shader)
                return 0
            }
            
            println("🔥 Shader compiled successfully: type=$type, id=$shader")
            shader
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Shader loading exception: ${e.message}")
            0
        }
    }
} 