package com.aiart.stablediffusion.ml

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.pytorch.IValue
import org.pytorch.Tensor
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Main Stable Diffusion model implementation using PyTorch Mobile
 */
class StableDiffusionModel {
    companion object {
        private const val TAG = "StableDiffusionModel"
    }

    // Inference parameters
    private var steps: Int = 20
    private var guidanceScale: Double = 7.5
    private var imageSize: Int = 256 // Default is 256x256, can be 384 or 512
    private var seed: Int = -1 // -1 means random seed
    
    // Model components
    private val modelManager = ModelManager.getInstance()
    private val textEncoder = TextEncoder()
    private val unet = UNet()
    private val vaeDecoder = VAEDecoder()
    
    /**
     * Load all required models
     */
    suspend fun loadModels(context: Context): Boolean {
        return modelManager.loadModels(context)
    }

    /**
     * Unload all models to free memory
     */
    fun unloadModels() {
        modelManager.unloadModels()
    }

    /**
     * Check if the model is loaded and ready for inference
     */
    fun isModelLoaded(): Boolean {
        return modelManager.isModelsLoaded()
    }

    /**
     * Generate an image from the provided text prompt
     */
    suspend fun generateImage(
        prompt: String, 
        negativePrompt: String = "",
        progressCallback: (Float, String) -> Unit
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (!isModelLoaded()) {
            Log.e(TAG, "Models not loaded")
            return@withContext null
        }

        try {
            Log.d(TAG, "Starting image generation with prompt: $prompt")
            progressCallback(0.0f, "Starting generation...")
            
            val startTime = SystemClock.elapsedRealtime()
            
            // Use provided seed or generate random seed
            val actualSeed = if (seed > 0) seed else Random.nextInt(0, 100000)
            val random = java.util.Random(actualSeed.toLong())
            Log.d(TAG, "Using seed: $actualSeed")
            
            // Generate text embeddings
            progressCallback(0.05f, "Encoding prompt...")
            val textEmbeddings = textEncoder.encode(prompt, negativePrompt)
            Log.d(TAG, "Text embeddings generated")
            
            // Initialize latents
            progressCallback(0.1f, "Initializing noise...")
            
            // Create random noise
            val latentChannels = 4 // Stable Diffusion uses 4 latent channels
            val latentWidth = imageSize / 8
            val latentHeight = imageSize / 8
            val latentSize = latentChannels * latentWidth * latentHeight
            
            val latents = FloatArray(latentSize)
            for (i in 0 until latentSize) {
                // Generate random values using Box-Muller transform for normal distribution
                val u1 = random.nextFloat()
                val u2 = random.nextFloat()
                val r = sqrt(-2.0 * kotlin.math.ln(u1)).toFloat()
                val theta = 2.0 * Math.PI * u2
                latents[i] = r * kotlin.math.cos(theta).toFloat()
            }
            
            // Scale latents
            val scale = 0.18215f
            for (i in 0 until latentSize) {
                latents[i] *= scale
            }
            
            // Prepare for denoising steps
            val timeSteps = getTimeSteps(steps)
            Log.d(TAG, "Starting denoising over $steps steps")
            
            var latentSample = latents.clone()
            
            // Run denoising loop
            for (i in 0 until steps) {
                val step = i + 1
                val timestep = timeSteps[i]
                
                progressCallback(0.1f + 0.8f * (step.toFloat() / steps), 
                                "Denoising: step $step/$steps")
                
                // Denoising step
                latentSample = unet.denoisingStep(
                    latentSample, 
                    timestep, 
                    textEmbeddings, 
                    guidanceScale.toFloat(),
                    latentWidth,
                    latentHeight
                )
                
                Log.d(TAG, "Completed denoising step $step/$steps")
            }
            
            // Decode latents to image
            progressCallback(0.9f, "Decoding image...")
            val resultBitmap = vaeDecoder.decode(latentSample, latentWidth, latentHeight, imageSize)
            
            val totalTime = SystemClock.elapsedRealtime() - startTime
            Log.d(TAG, "Image generation completed in ${totalTime}ms")
            
            progressCallback(1.0f, "Generation complete!")
            
            return@withContext resultBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error during image generation", e)
            progressCallback(0f, "Error: ${e.message}")
            return@withContext null
        }
    }
    
    /**
     * Create timesteps for the diffusion process
     */
    private fun getTimeSteps(numInferenceSteps: Int): IntArray {
        // Stable Diffusion typically uses 1000 steps of diffusion in training
        val maxTrainSteps = 1000
        
        val timeSteps = IntArray(numInferenceSteps)
        val stepSize = maxTrainSteps / numInferenceSteps
        
        for (i in 0 until numInferenceSteps) {
            // We go from 999 down to 0 with decreasing step sizes
            timeSteps[i] = maxTrainSteps - i * stepSize - 1
        }
        
        return timeSteps
    }

    // Getters and setters for inference parameters
    fun setSteps(steps: Int) {
        this.steps = steps.coerceIn(5, 50)
    }

    fun getSteps(): Int {
        return steps
    }

    fun setGuidanceScale(guidanceScale: Double) {
        this.guidanceScale = guidanceScale.coerceIn(1.0, 20.0)
    }

    fun getGuidanceScale(): Double {
        return guidanceScale
    }

    fun setImageSize(size: Int) {
        // Only allow 256, 384 or 512 for size
        this.imageSize = when (size) {
            256, 384, 512 -> size
            else -> 256
        }
    }

    fun getImageSize(): Int {
        return imageSize
    }

    fun setSeed(seed: Int) {
        this.seed = seed
    }

    fun getSeed(): Int {
        return seed
    }

    fun setModelSize(size: String) {
        modelManager.setModelSize(size)
    }

    fun getModelSize(): String {
        return modelManager.getModelSize()
    }
}
