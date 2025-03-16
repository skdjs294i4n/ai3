package com.aiart.stablediffusion.ui

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aiart.stablediffusion.R
import com.aiart.stablediffusion.ml.ModelManager
import com.aiart.stablediffusion.utils.ImageUtils
import com.aiart.stablediffusion.utils.ModelFileInfo
import com.aiart.stablediffusion.viewmodel.GenerationViewModel

class SettingsFragment : Fragment() {

    private lateinit var viewModel: GenerationViewModel
    
    // Settings UI elements
    private lateinit var imageSizeRadioGroup: RadioGroup
    private lateinit var modelSizeRadioGroup: RadioGroup
    private lateinit var seedSeekBar: SeekBar
    private lateinit var seedValueText: TextView
    private lateinit var resetModelButton: Button
    private lateinit var clearCacheButton: Button
    private lateinit var memoryUsageText: TextView
    private lateinit var modelInfoText: TextView
    private lateinit var customModelButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity()).get(GenerationViewModel::class.java)
        
        // Initialize UI elements
        imageSizeRadioGroup = view.findViewById(R.id.image_size_radio_group)
        modelSizeRadioGroup = view.findViewById(R.id.model_size_radio_group)
        seedSeekBar = view.findViewById(R.id.seed_seekbar)
        seedValueText = view.findViewById(R.id.seed_value)
        resetModelButton = view.findViewById(R.id.reset_model_button)
        clearCacheButton = view.findViewById(R.id.clear_cache_button)
        memoryUsageText = view.findViewById(R.id.memory_usage_text)
        modelInfoText = view.findViewById(R.id.model_info_text)
        customModelButton = view.findViewById(R.id.custom_model_button)
        
        // Set initial values
        setInitialValues()
        
        // Set up listeners
        setupListeners()
        
        // Update memory usage info
        updateMemoryUsage()
    }
    
    private fun setInitialValues() {
        // Image size selection
        val radioButtonId = when(viewModel.getImageSize()) {
            256 -> R.id.size_256
            384 -> R.id.size_384
            512 -> R.id.size_512
            else -> R.id.size_256
        }
        imageSizeRadioGroup.check(radioButtonId)
        
        // Model size selection
        val modelRadioButtonId = when(viewModel.getModelSize()) {
            "small" -> R.id.model_small
            "medium" -> R.id.model_medium
            "large" -> R.id.model_large
            else -> R.id.model_small
        }
        modelSizeRadioGroup.check(modelRadioButtonId)
        
        // Seed value
        val seedValue = viewModel.getSeed()
        seedSeekBar.max = 100000
        seedSeekBar.progress = if (seedValue <= 0) 0 else seedValue
        updateSeedValueText()
        
        // Model info
        updateModelInfo()
    }
    
    private fun setupListeners() {
        // Image size selection
        imageSizeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val size = when(checkedId) {
                R.id.size_256 -> 256
                R.id.size_384 -> 384
                R.id.size_512 -> 512
                else -> 256
            }
            viewModel.setImageSize(size)
            updateMemoryUsage()
        }
        
        // Model size selection
        modelSizeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val modelSize = when(checkedId) {
                R.id.model_small -> "small"
                R.id.model_medium -> "medium"
                R.id.model_large -> "large"
                else -> "small"
            }
            viewModel.setModelSize(modelSize)
            updateMemoryUsage()
            
            // Show warning that model needs to be reloaded
            Toast.makeText(context, "Please reset model to apply changes", Toast.LENGTH_SHORT).show()
        }
        
        // Seed selection
        seedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.setSeed(progress)
                updateSeedValueText()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Reset model button
        resetModelButton.setOnClickListener {
            viewModel.unloadModel()
            viewModel.loadModel(requireContext())
            Toast.makeText(context, "Model is being reloaded", Toast.LENGTH_SHORT).show()
            updateModelInfo()
        }
        
        // Clear cache button
        clearCacheButton.setOnClickListener {
            viewModel.clearCache(requireContext())
            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
            updateMemoryUsage()
        }
        
        // Custom model button
        customModelButton.setOnClickListener {
            showCustomModelSelection()
        }
    }
    
    private fun showCustomModelSelection() {
        // First load the available models through the viewModel
        viewModel.loadAvailableCustomModels(requireContext())
        
        // Get the model files from the viewModel
        val modelFiles = viewModel.getModelFiles()
        
        if (modelFiles.isEmpty()) {
            Toast.makeText(context, getString(R.string.no_custom_models_found), Toast.LENGTH_LONG).show()
            return
        }
        
        // Prepare the display options with model format and size info
        val options = mutableListOf(getString(R.string.use_default_model))
        modelFiles.forEach { modelFile ->
            options.add("${modelFile.name} (${modelFile.getFormattedSize()})")
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_custom_model_button))
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    // User selected "Use default model"
                    viewModel.setCustomModelPath(null)
                    Toast.makeText(context, getString(R.string.use_default_model), Toast.LENGTH_SHORT).show()
                } else {
                    // User selected a custom model
                    val selectedModel = modelFiles[which - 1]
                    
                    // For Android 10+, we need to load the model through its URI
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Load the model using the viewModel to handle caching and loading
                        viewModel.loadModelFromUri(
                            requireContext(), 
                            selectedModel.uri, 
                            selectedModel
                        )
                        
                        Toast.makeText(
                            context,
                            getString(R.string.model_loading_success) + ": ${selectedModel.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // For older Android versions, we can use the path directly
                        viewModel.setCustomModelPath(selectedModel.path)
                        Toast.makeText(
                            context,
                            getString(R.string.model_loading_success) + ": ${selectedModel.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                
                // Show message to reload the model
                Toast.makeText(
                    context,
                    getString(R.string.model_reload_required),
                    Toast.LENGTH_SHORT
                ).show()
                updateModelInfo()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun updateSeedValueText() {
        val seed = viewModel.getSeed()
        seedValueText.text = if (seed <= 0) "Random" else seed.toString()
    }
    
    private fun updateMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val total = runtime.maxMemory() / (1024 * 1024)
        val percent = (used.toDouble() / total.toDouble() * 100).toInt()
        
        memoryUsageText.text = "Memory: ${used}MB / ${total}MB (${percent}%)"
    }
    
    private fun updateModelInfo() {
        val modelStatus = if (viewModel.isModelLoaded()) "Model loaded" else "Model not loaded"
        val modelSize = viewModel.getModelSize()
        val imageSize = viewModel.getImageSize()
        val customModelPath = viewModel.getCustomModelPath()
        
        val customModelInfo = if (customModelPath != null) {
            "\nCustom model: ${customModelPath.substring(customModelPath.lastIndexOf('/') + 1)}"
        } else {
            ""
        }
        
        modelInfoText.text = "$modelStatus\nModel size: $modelSize\nImage size: ${imageSize}x${imageSize}$customModelInfo"
    }
    
    override fun onResume() {
        super.onResume()
        updateMemoryUsage()
        updateModelInfo()
    }
}
