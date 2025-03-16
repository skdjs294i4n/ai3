package com.aiart.stablediffusion.ml

import android.graphics.Bitmap
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * VAE Decoder component for Stable Diffusion
 * Converts latent representations back to RGB images
 */
class VAEDecoder {
    companion object {
        private const val TAG = "VAEDecoder"
    }

    /**
     * Decode latents to an RGB image
     * 
     * @param latents Latent representation to decode
     * @param width Width of the latent image
     * @param height Height of the latent image
     * @param targetSize Target size for the output image
     * @return Bitmap containing the decoded image
     */
    fun decode(latents: FloatArray, width: Int, height: Int, targetSize: Int): Bitmap {
        val modelManager = ModelManager.getInstance()
        val vaeDecoder = modelManager.getVaeDecoder() 
            ?: throw IllegalStateException("VAE decoder model not loaded")

        try {
            Log.d(TAG, "Decoding latents to image")
            
            // Scale the latents
            // Stable Diffusion's VAE expects scaled latents
            val scaleFactor = 1.0f / 0.18215f
            val scaledLatents = FloatArray(latents.size)
            for (i in latents.indices) {
                scaledLatents[i] = latents[i] * scaleFactor
            }
            
            // Create input tensor for the VAE decoder
            val inputTensor = Tensor.fromBlob(
                scaledLatents,
                longArrayOf(1, 4, height.toLong(), width.toLong())
            )
            
            // Run VAE decoder
            val outputTensor = vaeDecoder.forward(IValue.from(inputTensor)).toTensor()
            
            // Get output and convert to RGB image
            val outputShape = outputTensor.shape()
            val outputWidth = outputShape[3].toInt()
            val outputHeight = outputShape[2].toInt()
            
            val rgbArray = FloatArray(3 * outputWidth * outputHeight)
            outputTensor.getDataAsFloatArray(rgbArray)
            
            // Create bitmap from the RGB values
            val bitmap = createBitmapFromRGB(rgbArray, outputWidth, outputHeight)
            
            // Resize the bitmap to the target size if needed
            return if (outputWidth != targetSize || outputHeight != targetSize) {
                Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding latents", e)
            throw e
        }
    }

    /**
     * Create a bitmap from RGB values
     */
    private fun createBitmapFromRGB(rgbArray: FloatArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        // VAE output is in range [-1, 1], we need to convert to [0, 255]
        var rgbIdx = 0
        var pixelIdx = 0
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Get RGB values - in the tensor they are in CHW format (channel, height, width)
                // Need to extract them in the right order
                val r = ((rgbArray[rgbIdx] + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
                val g = ((rgbArray[rgbIdx + width * height] + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
                val b = ((rgbArray[rgbIdx + 2 * width * height] + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
                
                // ARGB format
                pixels[pixelIdx] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                
                rgbIdx++
                pixelIdx++
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
