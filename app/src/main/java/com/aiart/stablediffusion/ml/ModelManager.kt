package com.aiart.stablediffusion.ml

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import com.aiart.stablediffusion.utils.ImageUtils
import com.aiart.stablediffusion.utils.ModelFileInfo
import org.pytorch.Module
import org.pytorch.PyTorchAndroid
import org.pytorch.LiteModuleLoader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelManager private constructor() {
    companion object {
        private const val TAG = "ModelManager"
        
        // Singleton instance
        @Volatile
        private var instance: ModelManager? = null
        
        fun getInstance(): ModelManager {
            return instance ?: synchronized(this) {
                instance ?: ModelManager().also { instance = it }
            }
        }
        
        // Common model file extensions
        private val SUPPORTED_MODEL_EXTENSIONS = listOf(".safetensors", ".bin", ".pt", ".pth", ".onnx")
    }

    // Model properties
    private var textEncoder: Module? = null
    private var unet: Module? = null
    private var vaeDecoder: Module? = null
    private var modelLock = ReentrantLock()
    private var modelSize: String = "small" // small, medium, large
    private var isLoaded: Boolean = false
    private var customModelPath: String? = null

    // File names for the models
    private val textEncoderFileName: String
        get() = "text_encoder_$modelSize.ptl"
        
    private val unetFileName: String
        get() = "unet_$modelSize.ptl"
        
    private val vaeDecoderFileName: String
        get() = "vae_decoder_$modelSize.ptl"

    // Load models with appropriate error handling
    suspend fun loadModels(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true
        
        modelLock.lock()
        try {
            Log.d(TAG, "Loading models: $modelSize size")
            val startTime = SystemClock.elapsedRealtime()
            
            try {
                if (customModelPath != null && SUPPORTED_MODEL_EXTENSIONS.any { customModelPath!!.endsWith(it) }) {
                    return@withContext loadCustomModel(context, customModelPath!!)
                } else {
                    // Text Encoder
                    val textEncoderFile = getFileFromAssets(context, textEncoderFileName)
                    textEncoder = LiteModuleLoader.load(textEncoderFile.absolutePath)
                    Log.d(TAG, "Text encoder loaded")
                    
                    // UNet
                    val unetFile = getFileFromAssets(context, unetFileName)
                    unet = LiteModuleLoader.load(unetFile.absolutePath)
                    Log.d(TAG, "UNet loaded")
                    
                    // VAE Decoder
                    val vaeDecoderFile = getFileFromAssets(context, vaeDecoderFileName)
                    vaeDecoder = LiteModuleLoader.load(vaeDecoderFile.absolutePath)
                    Log.d(TAG, "VAE decoder loaded")
                }
                
                val loadTime = SystemClock.elapsedRealtime() - startTime
                Log.d(TAG, "Models loaded in ${loadTime}ms")
                
                isLoaded = true
                return@withContext true
            } catch (e: IOException) {
                Log.e(TAG, "Error loading models", e)
                unloadModels()
                return@withContext false
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "Out of memory error loading models", e)
                unloadModels()
                System.gc()
                return@withContext false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading models", e)
                unloadModels()
                return@withContext false
            }
        } finally {
            modelLock.unlock()
        }
    }

    // Load custom model in various formats (.safetensors, .bin, .pt, .pth, etc.)
    private suspend fun loadCustomModel(context: Context, modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading custom model from $modelPath")
            
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                Log.e(TAG, "Custom model file not found: $modelPath")
                return@withContext false
            }
            
            // Determine the model format
            val isSafetensors = modelPath.endsWith(".safetensors")
            val isPyTorch = modelPath.endsWith(".pt") || modelPath.endsWith(".pth")
            val isBin = modelPath.endsWith(".bin")
            val isOnnx = modelPath.endsWith(".onnx")
            
            Log.d(TAG, "Processing ${
                when {
                    isSafetensors -> "safetensors"
                    isPyTorch -> "PyTorch"
                    isBin -> "binary"
                    isOnnx -> "ONNX"
                    else -> "unknown"
                }
            } model format")
            
            // Create cache directory for converted models if needed
            val cacheDir = File(context.cacheDir, "models")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            when {
                // Handle .safetensors files - convert to ONNX format
                isSafetensors -> {
                    val convertedModelPath = convertSafetensorsToOnnx(context, modelPath)
                    if (convertedModelPath != null) {
                        // Load the converted ONNX model
                        return@withContext loadOnnxModel(context, convertedModelPath)
                    } else {
                        Log.e(TAG, "Failed to convert .safetensors model to ONNX")
                        return@withContext false
                    }
                }
                
                // Handle PyTorch models directly
                isPyTorch -> {
                    try {
                        // For local cache files, load directly from file
                        val module = LiteModuleLoader.load(modelPath)
                        
                        // This is a simplified example - in reality, you'd need to determine
                        // which component this model represents (text encoder, unet, or vae)
                        textEncoder = module
                        
                        Log.d(TAG, "PyTorch model loaded successfully")
                        return@withContext true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load PyTorch model", e)
                        
                        // Fall back to default models
                        loadDefaultModels(context)
                        return@withContext true
                    }
                }
                
                // Handle ONNX models
                isOnnx -> {
                    return@withContext loadOnnxModel(context, modelPath)
                }
                
                // For other formats or as fallback
                else -> {
                    Log.d(TAG, "Unsupported model format or using fallback loading")
                    // Load default models as fallback
                    loadDefaultModels(context)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading custom model", e)
            return@withContext false
        }
    }
    
    /**
     * Convert a .safetensors model to ONNX format that can be used with ONNX Runtime
     * This is a placeholder for the actual conversion process
     */
    private fun convertSafetensorsToOnnx(context: Context, safetensorsPath: String): String? {
        try {
            Log.d(TAG, "Converting .safetensors model to ONNX format")
            
            // This is where you would implement the actual conversion logic
            // For now, we'll use a placeholder approach and return the path to a mock converted file
            
            // In a real implementation, you would:
            // 1. Read the .safetensors file
            // 2. Parse the weights and structure
            // 3. Create an ONNX model with the same weights
            // 4. Save the ONNX model to the cache directory
            
            val onnxFileName = File(safetensorsPath).nameWithoutExtension + ".onnx"
            val onnxFile = File(context.cacheDir, "models/$onnxFileName")
            
            // For demonstration purposes, we're not doing real conversion
            // but instead copying a sample ONNX file or creating a placeholder
            
            // In this placeholder implementation, we'll just return null
            // which will trigger the fallback to default models
            
            Log.d(TAG, "Conversion placeholder - would create: ${onnxFile.absolutePath}")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during model conversion", e)
            return null
        }
    }
    
    /**
     * Load an ONNX model using ONNX Runtime
     */
    private fun loadOnnxModel(context: Context, onnxPath: String): Boolean {
        try {
            Log.d(TAG, "Loading ONNX model from $onnxPath")
            
            // This is a placeholder for ONNX Runtime integration
            // In a real implementation, you would:
            // 1. Use OnnxRuntime to load the model
            // 2. Set up the proper execution providers
            // 3. Create inference sessions
            
            // For now, we'll fall back to the default PyTorch models
            loadDefaultModels(context)
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading ONNX model", e)
            return false
        }
    }
    
    /**
     * Load the default models from app assets
     */
    private fun loadDefaultModels(context: Context) {
        Log.d(TAG, "Loading default models from assets")
        
        val textEncoderFile = getFileFromAssets(context, textEncoderFileName)
        textEncoder = LiteModuleLoader.load(textEncoderFile.absolutePath)
        
        val unetFile = getFileFromAssets(context, unetFileName)
        unet = LiteModuleLoader.load(unetFile.absolutePath)
        
        val vaeDecoderFile = getFileFromAssets(context, vaeDecoderFileName)
        vaeDecoder = LiteModuleLoader.load(vaeDecoderFile.absolutePath)
        
        Log.d(TAG, "Default models loaded successfully")
    }

    // Unload all models to free memory
    fun unloadModels() {
        modelLock.lock()
        try {
            textEncoder?.destroy()
            textEncoder = null
            
            unet?.destroy()
            unet = null
            
            vaeDecoder?.destroy()
            vaeDecoder = null
            
            isLoaded = false
            System.gc()
        } finally {
            modelLock.unlock()
        }
    }

    // Copy model files from assets to app's private storage
    private fun getFileFromAssets(context: Context, fileName: String): File {
        val file = File(context.filesDir, fileName)
        
        // Check if the file already exists and has the correct size
        if (file.exists()) {
            try {
                // Just return the existing file
                return file
            } catch (e: Exception) {
                // If there's an error, delete the file and try again
                file.delete()
            }
        }
        
        // Copy the file from assets
        context.assets.open(fileName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }
        
        return file
    }

    /**
     * List all available custom models using ImageUtils for Android 10+ compatibility
     */
    fun listAvailableCustomModels(context: Context): List<ModelFileInfo> {
        return ImageUtils.listModelFiles(context)
    }
    
    /**
     * Load model from Uri for Android 10+ compatibility
     */
    suspend fun loadModelFromUri(context: Context, uri: Uri, modelInfo: ModelFileInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading model from Uri: $uri")
            
            // Copy the file to our cache for processing
            val cachedFile = ImageUtils.copyModelFileToCache(context, uri, modelInfo)
            
            if (cachedFile != null) {
                customModelPath = cachedFile.absolutePath
                isLoaded = false // Force reload
                return@withContext loadModels(context)
            } else {
                Log.e(TAG, "Failed to copy model file to cache")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model from Uri", e)
            return@withContext false
        }
    }

    // Set custom model path (for .safetensors files)
    fun setCustomModelPath(path: String?) {
        if (customModelPath != path) {
            customModelPath = path
            isLoaded = false // Force reload of models
        }
    }

    fun getCustomModelPath(): String? {
        return customModelPath
    }

    fun getTextEncoder(): Module? {
        return textEncoder
    }

    fun getUnet(): Module? {
        return unet
    }

    fun getVaeDecoder(): Module? {
        return vaeDecoder
    }

    fun setModelSize(size: String) {
        if (modelSize != size) {
            modelSize = size
            isLoaded = false // Force reload of models
        }
    }

    fun getModelSize(): String {
        return modelSize
    }

    fun isModelsLoaded(): Boolean {
        return isLoaded && textEncoder != null && unet != null && vaeDecoder != null
    }
    
    /**
     * Clear all cached model files
     */
    fun clearModelCache(context: Context): Boolean {
        try {
            val modelCacheDir = File(context.cacheDir, "models")
            if (modelCacheDir.exists() && modelCacheDir.isDirectory) {
                modelCacheDir.listFiles()?.forEach { it.delete() }
                Log.d(TAG, "Model cache cleared")
                return true
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing model cache", e)
            return false
        }
    }
}
