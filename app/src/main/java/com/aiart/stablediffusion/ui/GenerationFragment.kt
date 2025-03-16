package com.aiart.stablediffusion.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aiart.stablediffusion.R
import com.aiart.stablediffusion.utils.ImageUtils
import com.aiart.stablediffusion.viewmodel.GenerationViewModel

class GenerationFragment : Fragment() {

    private lateinit var viewModel: GenerationViewModel
    private lateinit var promptInput: EditText
    private lateinit var generateButton: Button
    private lateinit var resultImage: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var saveButton: Button
    private lateinit var shareButton: Button
    private lateinit var stepsSeekBar: SeekBar
    private lateinit var stepsValueText: TextView
    private lateinit var guidanceScaleSeekBar: SeekBar
    private lateinit var guidanceScaleValueText: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_generation, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this).get(GenerationViewModel::class.java)
        
        // Initialize UI elements
        promptInput = view.findViewById(R.id.prompt_input)
        generateButton = view.findViewById(R.id.generate_button)
        resultImage = view.findViewById(R.id.result_image)
        progressBar = view.findViewById(R.id.progress_bar)
        statusText = view.findViewById(R.id.status_text)
        saveButton = view.findViewById(R.id.save_button)
        shareButton = view.findViewById(R.id.share_button)
        stepsSeekBar = view.findViewById(R.id.steps_seekbar)
        stepsValueText = view.findViewById(R.id.steps_value)
        guidanceScaleSeekBar = view.findViewById(R.id.guidance_scale_seekbar)
        guidanceScaleValueText = view.findViewById(R.id.guidance_scale_value)
        
        // Set up UI interactions
        setupSeekBars()
        
        generateButton.setOnClickListener {
            val prompt = promptInput.text.toString().trim()
            if (prompt.isNotEmpty()) {
                startGeneration(prompt)
            } else {
                Toast.makeText(context, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }
        
        saveButton.setOnClickListener {
            viewModel.currentBitmap.value?.let { bitmap ->
                context?.let { ctx ->
                    ImageUtils.saveBitmapToGallery(ctx, bitmap, "SD_${System.currentTimeMillis()}")
                    Toast.makeText(ctx, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
            }
        }
        
        shareButton.setOnClickListener {
            viewModel.currentBitmap.value?.let { bitmap ->
                context?.let { ctx ->
                    val uri = ImageUtils.getUriFromBitmap(ctx, bitmap)
                    ImageUtils.shareImage(ctx, uri)
                }
            } ?: run {
                Toast.makeText(context, "No image to share", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe view model changes
        viewModel.isGenerating.observe(viewLifecycleOwner) { isGenerating ->
            progressBar.visibility = if (isGenerating) View.VISIBLE else View.GONE
            generateButton.isEnabled = !isGenerating
            statusText.visibility = if (isGenerating) View.VISIBLE else View.GONE
        }
        
        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            statusText.text = message
        }
        
        viewModel.currentBitmap.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                resultImage.setImageBitmap(bitmap)
                saveButton.isEnabled = true
                shareButton.isEnabled = true
            } else {
                saveButton.isEnabled = false
                shareButton.isEnabled = false
            }
        }
        
        viewModel.generationError.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
        
        // Initialize the model on first load
        if (!viewModel.isModelLoaded()) {
            loadModel()
        }
    }
    
    private fun setupSeekBars() {
        // Configure Steps SeekBar
        stepsSeekBar.max = 45
        stepsSeekBar.progress = viewModel.getSteps() - 5
        updateStepsValueText()
        
        stepsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.setSteps(progress + 5) // Min 5, Max 50
                updateStepsValueText()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Configure Guidance Scale SeekBar
        guidanceScaleSeekBar.max = 15
        guidanceScaleSeekBar.progress = ((viewModel.getGuidanceScale() - 1.0) * 2).toInt() // Scale 1.0-8.5 to 0-15
        updateGuidanceScaleValueText()
        
        guidanceScaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val guidanceScale = 1.0 + progress / 2.0 // Range 1.0-8.5
                viewModel.setGuidanceScale(guidanceScale)
                updateGuidanceScaleValueText()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun updateStepsValueText() {
        stepsValueText.text = "${viewModel.getSteps()}"
    }
    
    private fun updateGuidanceScaleValueText() {
        guidanceScaleValueText.text = String.format("%.1f", viewModel.getGuidanceScale())
    }
    
    private fun loadModel() {
        viewModel.loadModel(requireContext())
        statusText.visibility = View.VISIBLE
        statusText.text = "Loading model..."
        progressBar.visibility = View.VISIBLE
        generateButton.isEnabled = false
    }
    
    private fun startGeneration(prompt: String) {
        viewModel.generateImage(requireContext(), prompt)
    }
}
