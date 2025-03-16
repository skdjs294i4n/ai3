package com.aiart.stablediffusion.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiart.stablediffusion.data.ImageRepository
import com.aiart.stablediffusion.ml.ModelManager
import com.aiart.stablediffusion.ml.StableDiffusionModel
import com.aiart.stablediffusion.utils.ImageUtils
import com.aiart.stablediffusion.utils.ModelFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for the image generation screen
 */
class GenerationViewModel : ViewModel() {
    private val imageRepository = ImageRepository()
    private val model = StableDiffusionModel()
    private val modelManager = ModelManager.getInstance()

    // LiveData for UI state
    private val _isGenerating = MutableLiveData<Boolean>()
    val isGenerating: LiveData<Boolean> get() = _isGenerating

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> get() = _statusMessage

    private val _currentBitmap = MutableLiveData<Bitmap?>()
    val currentBitmap: LiveData<Bitmap?> get() = _currentBitmap

    private val _generationError = MutableLiveData<String>()
    val generationError: LiveData<String> get() = _generationError
    
    private val _modelFiles = MutableLiveData<List<ModelFileInfo>>()
    val modelFiles: LiveData<List<ModelFileInfo>> get() = _modelFiles

    /**
     * Load the ML model
     */
    fun loadModel(context: Context) {
        if (_isGenerating.value == true) return
        
        viewModelScope.launch {
            _isGenerating.value = true
            _statusMessage.value = "Loading model..."
            
            try {
                val success = model.loadModels(context)
                if (!success) {
                    _generationError.value = "Failed to load model. Please check your device storage."
                }
            } catch (e: Exception) {
                _generationError.value = "Error loading model: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Unload the model to free memory
     */
    fun unloadModel() {
        viewModelScope.launch {
            model.unloadModels()
        }
    }

    /**
     * Generate an image from the given prompt
     */
    fun generateImage(context: Context, prompt: String, negativePrompt: String = "") {
        if (_isGenerating.value == true) return
        
        viewModelScope.launch {
            _isGenerating.value = true
            _statusMessage.value = "Starting generation..."
            
            try {
                val bitmap = model.generateImage(prompt, negativePrompt) { progress, message ->
                    // Update progress on the main thread
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = message
                    }
                }
                
                if (bitmap != null) {
                    _currentBitmap.value = bitmap
                    // Save to internal storage
                    try {
                        imageRepository.saveImage(context, bitmap)
                    } catch (e: Exception) {
                        _generationError.value = "Image generated but not saved: ${e.message}"
                    }
                } else {
                    _generationError.value = "Failed to generate image"
                }
            } catch (e: Exception) {
                _generationError.value = "Error generating image: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Clear the image cache and model cache
     */
    fun clearCache(context: Context) {
        viewModelScope.launch {
            imageRepository.clearCache(context)
            modelManager.clearModelCache(context)
        }
    }

    /**
     * Check if the model is loaded
     */
    fun isModelLoaded(): Boolean {
        return model.isModelLoaded()
    }

    /**
     * Get a list of available custom models with Android 10+ compatibility
     */
    fun loadAvailableCustomModels(context: Context) {
        viewModelScope.launch {
            val models = modelManager.listAvailableCustomModels(context)
            _modelFiles.postValue(models)
        }
    }
    
    /**
     * Get the current model files
     */
    fun getModelFiles(): List<ModelFileInfo> {
        return _modelFiles.value ?: emptyList()
    }
    
    /**
     * Load a model from its Uri (for Android 10+ compatibility)
     */
    fun loadModelFromUri(context: Context, uri: Uri, modelInfo: ModelFileInfo) {
        viewModelScope.launch {
            _isGenerating.value = true
            _statusMessage.value = "Loading model..."
            
            try {
                val success = modelManager.loadModelFromUri(context, uri, modelInfo)
                if (!success) {
                    _generationError.value = "Failed to load model from ${modelInfo.name}"
                }
            } catch (e: Exception) {
                _generationError.value = "Error loading model: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Set the custom model path
     */
    fun setCustomModelPath(path: String?) {
        modelManager.setCustomModelPath(path)
    }

    /**
     * Get the current custom model path
     */
    fun getCustomModelPath(): String? {
        return modelManager.getCustomModelPath()
    }

    // Forwarding generation parameters to the model
    fun setSteps(steps: Int) {
        model.setSteps(steps)
    }

    fun getSteps(): Int {
        return model.getSteps()
    }

    fun setGuidanceScale(guidanceScale: Double) {
        model.setGuidanceScale(guidanceScale)
    }

    fun getGuidanceScale(): Double {
        return model.getGuidanceScale()
    }

    fun setImageSize(size: Int) {
        model.setImageSize(size)
    }

    fun getImageSize(): Int {
        return model.getImageSize()
    }

    fun setSeed(seed: Int) {
        model.setSeed(seed)
    }

    fun getSeed(): Int {
        return model.getSeed()
    }

    fun setModelSize(size: String) {
        model.setModelSize(size)
    }

    fun getModelSize(): String {
        return model.getModelSize()
    }
}
