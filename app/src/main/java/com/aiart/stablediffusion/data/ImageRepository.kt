package com.aiart.stablediffusion.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.aiart.stablediffusion.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository for managing generated images
 */
class ImageRepository {
    /**
     * Save a generated image to internal storage
     */
    suspend fun saveImage(context: Context, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        return@withContext ImageUtils.saveToInternalStorage(context, bitmap)
    }

    /**
     * Get all saved images
     */
    suspend fun getSavedImages(context: Context): List<File> = withContext(Dispatchers.IO) {
        return@withContext ImageUtils.listSavedImages(context)
    }

    /**
     * Delete an image
     */
    suspend fun deleteImage(file: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext ImageUtils.deleteImage(file)
    }

    /**
     * Save image to gallery
     */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap, name: String) = withContext(Dispatchers.IO) {
        return@withContext ImageUtils.saveBitmapToGallery(context, bitmap, name)
    }

    /**
     * Load bitmap from file
     */
    suspend fun loadBitmap(file: File): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clear cache directory of temporary images
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "temp_images")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
        }
    }
}
