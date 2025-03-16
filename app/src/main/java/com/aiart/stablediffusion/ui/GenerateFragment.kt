package com.aiart.stablediffusion.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aiart.stablediffusion.R
import com.aiart.stablediffusion.ui.model.StableDiffusionModel
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class GenerateFragment : Fragment() {

    private lateinit var editTextPrompt: TextInputEditText
    private lateinit var sliderInferenceSteps: Slider
    private lateinit var sliderGuidanceScale: Slider
    private lateinit var textViewInferenceSteps: TextView
    private lateinit var textViewGuidanceScale: TextView
    private lateinit var imageViewPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonGenerate: Button
    
    private var model: StableDiffusionModel = StableDiffusionModel()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_generate, container, false)
        
        editTextPrompt = root.findViewById(R.id.editTextPrompt)
        sliderInferenceSteps = root.findViewById(R.id.sliderInferenceSteps)
        sliderGuidanceScale = root.findViewById(R.id.sliderGuidanceScale)
        textViewInferenceSteps = root.findViewById(R.id.textViewInferenceSteps)
        textViewGuidanceScale = root.findViewById(R.id.textViewGuidanceScale)
        imageViewPreview = root.findViewById(R.id.imageViewPreview)
        progressBar = root.findViewById(R.id.progressBar)
        buttonGenerate = root.findViewById(R.id.buttonGenerate)
        
        setupSliders()
        setupGenerateButton()
        
        return root
    }
    
    private fun setupSliders() {
        // Set initial values
        sliderInferenceSteps.value = 20f
        sliderGuidanceScale.value = 7.5f
        
        // Update text when sliders change
        sliderInferenceSteps.addOnChangeListener { _, value, _ ->
            textViewInferenceSteps.text = getString(R.string.inference_steps_20)
                .replace("20", value.toInt().toString())
        }
        
        sliderGuidanceScale.addOnChangeListener { _, value, _ ->
            val formattedValue = String.format(Locale.US, "%.1f", value)
            textViewGuidanceScale.text = getString(R.string.guidance_scale_7_5)
                .replace("7.5", formattedValue)
        }
    }
    
    private fun setupGenerateButton() {
        buttonGenerate.setOnClickListener {
            val prompt = editTextPrompt.text.toString().trim()
            
            if (prompt.isEmpty()) {
                Toast.makeText(context, "Please enter a prompt", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            startImageGeneration(prompt)
        }
    }
    
    private fun startImageGeneration(prompt: String) {
        // Show generating state
        progressBar.visibility = View.VISIBLE
        buttonGenerate.isEnabled = false
        
        // Get parameters
        val steps = sliderInferenceSteps.value.toInt()
        val guidanceScale = sliderGuidanceScale.value
        
        // Generate image using the model
        model.generateImage(
            prompt = prompt,
            steps = steps,
            guidanceScale = guidanceScale,
            onProgress = { progress ->
                // Update progress if needed
            },
            onComplete = { bitmap ->
                activity?.runOnUiThread {
                    imageViewPreview.setImageBitmap(bitmap)
                    progressBar.visibility = View.INVISIBLE
                    buttonGenerate.isEnabled = true
                }
            },
            onError = { error ->
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.INVISIBLE
                    buttonGenerate.isEnabled = true
                }
            }
        )
    }
}