package com.aiart.stablediffusion.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiart.stablediffusion.R
import com.aiart.stablediffusion.utils.ImageUtils

class GalleryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewNoImages: TextView
    private lateinit var adapter: GalleryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_gallery, container, false)
        
        recyclerView = root.findViewById(R.id.recyclerViewGallery)
        progressBar = root.findViewById(R.id.progressBarGallery)
        textViewNoImages = root.findViewById(R.id.textViewNoImages)
        
        setupRecyclerView()
        loadImages()
        
        return root
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh gallery when returning to this fragment
        loadImages()
    }
    
    private fun setupRecyclerView() {
        adapter = GalleryAdapter { imageId ->
            // Navigate to detail screen with the selected image ID
            val action = GalleryFragmentDirections.actionGalleryToDetail(imageId)
            findNavController().navigate(action)
        }
        
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = adapter
    }
    
    private fun loadImages() {
        progressBar.visibility = View.VISIBLE
        
        // Use ImageUtils to load all saved images
        ImageUtils.getAllSavedImages(requireContext()) { images ->
            activity?.runOnUiThread {
                progressBar.visibility = View.GONE
                
                if (images.isEmpty()) {
                    textViewNoImages.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    textViewNoImages.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.submitList(images)
                }
            }
        }
    }
    
    // Adapter for the gallery RecyclerView
    inner class GalleryAdapter(
        private val onItemClick: (Long) -> Unit
    ) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {
        
        private var items: List<ImageUtils.SavedImage> = emptyList()
        
        fun submitList(newItems: List<ImageUtils.SavedImage>) {
            items = newItems
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery, parent, false)
            return GalleryViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }
        
        override fun getItemCount() = items.size
        
        inner class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.imageViewGalleryItem)
            private val textViewPrompt: TextView = itemView.findViewById(R.id.textViewPrompt)
            
            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(items[position].id)
                    }
                }
            }
            
            fun bind(item: ImageUtils.SavedImage) {
                imageView.setImageBitmap(item.bitmap)
                textViewPrompt.text = item.prompt
            }
        }
    }
}