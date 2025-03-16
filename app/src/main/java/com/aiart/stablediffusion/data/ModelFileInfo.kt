package com.aiart.stablediffusion.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * Class representing a model file with information about its format, size, and location
 */
@Parcelize
data class ModelFileInfo(
    val name: String,               // Display name of the model
    val filePath: String,           // Full file path
    val type: ModelType,            // Type/format of the model
    val sizeInMb: Float,            // Size in megabytes
    val isBuiltIn: Boolean = false  // Whether this is a built-in model or user-added
) : Parcelable {

    /**
     * Valid model file extensions
     */
    enum class ModelType {
        SAFETENSORS,  // .safetensors format
        CHECKPOINT,   // .ckpt format
        PYTORCH,      // .pt or .pth formats
        BIN,          // .bin format
        ONNX,         // .onnx format
        UNKNOWN;      // Unsupported or unknown format
        
        companion object {
            /**
             * Get model type from file extension
             */
            fun fromFile(file: File): ModelType {
                return when (file.extension.lowercase()) {
                    "safetensors" -> SAFETENSORS
                    "ckpt" -> CHECKPOINT
                    "pt", "pth" -> PYTORCH
                    "bin" -> BIN
                    "onnx" -> ONNX
                    else -> UNKNOWN
                }
            }
            
            /**
             * Get list of supported file extensions
             */
            fun getSupportedExtensions(): List<String> {
                return listOf("safetensors", "ckpt", "pt", "pth", "bin", "onnx")
            }
        }
    }
    
    /**
     * Check if the model file exists
     */
    fun exists(): Boolean {
        return File(filePath).exists()
    }
    
    /**
     * Check if the model format is supported
     */
    fun isFormatSupported(): Boolean {
        return type != ModelType.UNKNOWN
    }
    
    companion object {
        /**
         * Create ModelFileInfo from a file
         */
        fun fromFile(file: File): ModelFileInfo {
            val type = ModelType.fromFile(file)
            val sizeInMb = file.length().toFloat() / (1024 * 1024)
            
            return ModelFileInfo(
                name = file.nameWithoutExtension,
                filePath = file.absolutePath,
                type = type,
                sizeInMb = sizeInMb
            )
        }
        
        /**
         * Find all model files in a directory
         */
        fun findModelsInDirectory(directory: File): List<ModelFileInfo> {
            if (!directory.exists() || !directory.isDirectory) {
                return emptyList()
            }
            
            return directory.listFiles { file ->
                file.isFile && ModelType.getSupportedExtensions().contains(file.extension.lowercase())
            }?.map { fromFile(it) } ?: emptyList()
        }
    }
}