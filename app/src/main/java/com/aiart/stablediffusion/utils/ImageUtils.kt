package com.aiart.stablediffusion.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for image operations optimized for Android 11+
 */
object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val QUALITY = 100
    private const val FOLDER_NAME = "StableDiffusion"

    /**
     * Save bitmap to the Pictures directory in gallery
     * Compatible with Android 11+ without requiring storage permissions
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String = ""): Uri? {
        val name = if (fileName.isBlank()) {
            "AI_IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.png"
        } else {
            if (fileName.endsWith(".jpg", true) || fileName.endsWith(".png", true)) {
                fileName
            } else {
                "$fileName.png"
            }
        }

        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$FOLDER_NAME")
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        return try {
            uri?.let { imageUri ->
                context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, outputStream)
                }
                imageUri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image: ${e.message}")
            null
        }
    }

    /**
     * Convert bitmap to byte array with high quality
     */
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to byte array: ${e.message}")
            ByteArray(0)
        }
    }

    /**
     * Convert byte array to bitmap
     */
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting byte array to bitmap: ${e.message}")
            null
        }
    }

    /**
     * Resize bitmap to specified width and height
     * Optimized to maintain aspect ratio
     */
    fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /**
     * Get bitmap from URI
     */
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bitmap from URI: ${e.message}")
            null
        }
    }

    /**
     * Fix image rotation issues from camera
     */
    fun rotateBitmapIfNeeded(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ExifInterface(inputStream)
                } else {
                    return bitmap // Simplified for newer Android versions
                }

                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    else -> return bitmap
                }

                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating bitmap: ${e.message}")
            return bitmap
        }
    }
}