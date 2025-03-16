package com.aiart.stablediffusion.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.aiart.stablediffusion.R
import com.aiart.stablediffusion.utils.ImageUtils
import com.aiart.stablediffusion.utils.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageDetailFragment : Fragment() {

    private val args: ImageDetailFragmentArgs by navArgs()
    
    private lateinit var imageView: ImageView
    private lateinit var textViewPrompt: TextView
    private lateinit var textViewDate: TextView
    private lateinit var buttonShare: Button
    private lateinit var buttonSave: Button
    
    private var currentImage: ImageUtils.SavedImage? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_image_detail, container, false)
        
        imageView = root.findViewById(R.id.imageViewDetail)
        textViewPrompt = root.findViewById(R.id.textViewPromptDetail)
        textViewDate = root.findViewById(R.id.textViewDateDetail)
        buttonShare = root.findViewById(R.id.buttonShare)
        buttonSave = root.findViewById(R.id.buttonSave)
        
        setupButtons()
        loadImage()
        
        return root
    }
    
    private fun setupButtons() {
        buttonSave.setOnClickListener {
            if (PermissionUtils.checkStoragePermission(requireActivity())) {
                saveImageToGallery()
            } else {
                PermissionUtils.requestStoragePermission(this)
            }
        }
        
        buttonShare.setOnClickListener {
            shareImage()
        }
    }
    
    private fun loadImage() {
        val imageId = args.imageId
        
        ImageUtils.getImageById(requireContext(), imageId) { image ->
            activity?.runOnUiThread {
                if (image != null) {
                    currentImage = image
                    
                    imageView.setImageBitmap(image.bitmap)
                    textViewPrompt.text = image.prompt
                    
                    // Format the date
                    val date = Date(image.timestamp)
                    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    textViewDate.text = dateFormat.format(date)
                } else {
                    Toast.makeText(context, "Image not found", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressed()
                }
            }
        }
    }
    
    private fun saveImageToGallery() {
        currentImage?.let { image ->
            ImageUtils.saveImageToGallery(requireContext(), image.bitmap, image.prompt) { success ->
                activity?.runOnUiThread {
                    if (success) {
                        Toast.makeText(context, R.string.image_saved, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, R.string.unable_to_save, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun shareImage() {
        currentImage?.let { image ->
            val shareIntent = ShareCompat.IntentBuilder.from(requireActivity())
                .setType("image/jpeg")
                .setText(image.prompt)
                .setChooserTitle(R.string.share_image_via)
                .createChooserIntent()
                
            // Create a temporary file to share
            ImageUtils.createShareableImageFile(requireContext(), image.bitmap) { uri ->
                if (uri != null) {
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    startActivity(shareIntent)
                } else {
                    Toast.makeText(context, R.string.unable_to_save, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PermissionUtils.STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageToGallery()
            } else {
                Toast.makeText(
                    context,
                    R.string.storage_permission_required,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}