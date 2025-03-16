package com.aiart.stablediffusion.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiart.stablediffusion.data.ImageRepository
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the gallery screen
 */
class GalleryViewModel : ViewModel() {
    private val imageRepository = ImageRepository()

    private val _generatedImages = MutableLiveData<List<File>>()
    val generatedImages: LiveData<List<File>> get() = _generatedImages

    /**
     * Load all saved images
     */
    fun loadImages(context: Context) {
        viewModelScope.launch {
            try {
                val images = imageRepository.getSavedImages(context)
                _generatedImages.value = images
            } catch (e: Exception) {
                _generatedImages.value = emptyList()
            }
        }
    }

    /**
     * Delete an image
     */
    fun deleteImage(file: File) {
        viewModelScope.launch {
            val success = imageRepository.deleteImage(file)
            if (success) {
                // Update the list after deletion
                _generatedImages.value = _generatedImages.value?.filter { it.absolutePath != file.absolutePath }
            }
        }
    }

    /**
     * Delete all images
     */
    fun deleteAllImages(context: Context) {
        viewModelScope.launch {
            _generatedImages.value?.forEach { file ->
                imageRepository.deleteImage(file)
            }
            loadImages(context)
        }
    }
}
