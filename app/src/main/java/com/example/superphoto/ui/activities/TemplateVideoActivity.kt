package com.example.superphoto.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.superphoto.R
import com.example.superphoto.ui.adapter.SelectedImagesAdapter

class TemplateVideoActivity : AppCompatActivity() {
    
    private lateinit var dataSimulation: ImageView
    private lateinit var addMoreImagesButton: Button
    private lateinit var rcvTemplate: RecyclerView
    private lateinit var imagesCountText: TextView
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter
    private val selectedImages = mutableListOf<Uri>()
    
    // UI Elements
    private lateinit var promptEditText: EditText
    private lateinit var negativePromptEditText: EditText
    private lateinit var duration10s: TextView
    private lateinit var duration15s: TextView
    private lateinit var duration20s: TextView
    private lateinit var generateButton: Button
    
    // Video Preview Elements
    private lateinit var videoPreviewSection: LinearLayout
    private lateinit var loadingContainer: LinearLayout
    private lateinit var videoResultContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var progressPercentage: TextView
    private lateinit var videoPreview: VideoView
    private lateinit var playPauseButton: Button
    private lateinit var downloadVideoButton: Button
    private lateinit var shareVideoButton: Button
    
    // State variables
    private var selectedDuration = 10 // Default 10 seconds
    private var isVideoPlaying = false
    private var isGenerating = false
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                if (data.clipData != null) {
                    // Multiple images selected
                    val clipData = data.clipData!!
                    for (i in 0 until clipData.itemCount) {
                        val imageUri = clipData.getItemAt(i).uri
                        selectedImages.add(imageUri)
                    }
                } else if (data.data != null) {
                    // Single image selected
                    selectedImages.add(data.data!!)
                }
                updateImagesDisplay()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_template_video)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        displayItemImage()
        setupDurationSelection()
    }
    
    private fun initViews() {
        // Image section
        dataSimulation = findViewById(R.id.dataSimulation)
        addMoreImagesButton = findViewById(R.id.addMoreImagesButton)
        rcvTemplate = findViewById(R.id.rcv_template)
        imagesCountText = findViewById(R.id.imagesCountText)
        
        // Input fields
        promptEditText = findViewById(R.id.promptEditText)
        negativePromptEditText = findViewById(R.id.negativePromptEditText)
        
        // Duration selection
        duration10s = findViewById(R.id.duration10s)
        duration15s = findViewById(R.id.duration15s)
        duration20s = findViewById(R.id.duration20s)
        
        // Generate button
        generateButton = findViewById(R.id.generateButton)
        
        // Video preview section
        videoPreviewSection = findViewById(R.id.videoPreviewSection)
        loadingContainer = findViewById(R.id.loadingContainer)
        videoResultContainer = findViewById(R.id.videoResultContainer)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        progressPercentage = findViewById(R.id.progressPercentage)
        videoPreview = findViewById(R.id.videoPreview)
        playPauseButton = findViewById(R.id.playPauseButton)
        downloadVideoButton = findViewById(R.id.downloadVideoButton)
        shareVideoButton = findViewById(R.id.shareVideoButton)
    }
    
    private fun setupRecyclerView() {
        selectedImagesAdapter = SelectedImagesAdapter(selectedImages) { position ->
            // Handle image removal
            selectedImages.removeAt(position)
            selectedImagesAdapter.notifyItemRemoved(position)
            updateImagesCount()
        }
        
        rcvTemplate.apply {
            layoutManager = LinearLayoutManager(this@TemplateVideoActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedImagesAdapter
        }
    }
    
    private fun setupClickListeners() {
        // Image picker
        addMoreImagesButton.setOnClickListener {
            openImagePicker()
        }
        
        // Duration selection
        duration10s.setOnClickListener { selectDuration(10, duration10s) }
        duration15s.setOnClickListener { selectDuration(15, duration15s) }
        duration20s.setOnClickListener { selectDuration(20, duration20s) }
        
        // Generate button
        generateButton.setOnClickListener {
            if (validateInputs()) {
                startVideoGeneration()
            }
        }
        
        // Video controls
        playPauseButton.setOnClickListener {
            toggleVideoPlayback()
        }
        
        downloadVideoButton.setOnClickListener {
            downloadVideo()
        }
        
        shareVideoButton.setOnClickListener {
            shareVideo()
        }
        
        // Upload area click
        dataSimulation.setOnClickListener {
            openImagePicker()
        }
    }
    
    private fun setupDurationSelection() {
        // Set default selection (10s)
        selectDuration(10, duration10s)
    }
    
    private fun selectDuration(duration: Int, selectedView: TextView) {
        selectedDuration = duration
        
        // Reset all duration views
        duration10s.setTextColor(ContextCompat.getColor(this, R.color.ai_text_secondary))
        duration15s.setTextColor(ContextCompat.getColor(this, R.color.ai_text_secondary))
        duration20s.setTextColor(ContextCompat.getColor(this, R.color.ai_text_secondary))
        
        // Highlight selected duration
        selectedView.setTextColor(ContextCompat.getColor(this, R.color.ai_text_primary))
        
        Toast.makeText(this, "Selected duration: ${duration}s", Toast.LENGTH_SHORT).show()
    }
    
    private fun validateInputs(): Boolean {
        // Check if at least one image is selected
        if (selectedImages.isEmpty()) {
            // Check if main image is available (from TemplatesFragment or other sources)
            val templateImageResource = intent.getIntExtra("TEMPLATE_IMAGE_RESOURCE", -1)
            val itemImageResource = intent.getIntExtra("ITEM_IMAGE_RESOURCE", -1)
            if (templateImageResource == -1 && itemImageResource == -1) {
                Toast.makeText(this, "Please select at least one image", Toast.LENGTH_LONG).show()
                return false
            }
        }
        
        // Validate prompt length (optional but if provided, should be reasonable)
        val prompt = promptEditText.text.toString().trim()
        if (prompt.length > 500) {
            Toast.makeText(this, "Prompt is too long (max 500 characters)", Toast.LENGTH_LONG).show()
            return false
        }
        
        // Validate negative prompt length
        val negativePrompt = negativePromptEditText.text.toString().trim()
        if (negativePrompt.length > 500) {
            Toast.makeText(this, "Negative prompt is too long (max 500 characters)", Toast.LENGTH_LONG).show()
            return false
        }
        
        return true
    }
    
    private fun startVideoGeneration() {
        if (isGenerating) return
        
        isGenerating = true
        generateButton.isEnabled = false
        generateButton.text = "Generating..."
        
        // Show video preview section
        videoPreviewSection.visibility = LinearLayout.VISIBLE
        loadingContainer.visibility = LinearLayout.VISIBLE
        videoResultContainer.visibility = LinearLayout.GONE
        
        // Start progress simulation
        simulateVideoGeneration()
    }
    
    private fun simulateVideoGeneration() {
        val handler = Handler(Looper.getMainLooper())
        var progress = 0
        
        val progressRunnable = object : Runnable {
            override fun run() {
                progress += 5
                progressPercentage.text = "$progress%"
                
                when (progress) {
                    in 0..25 -> progressText.text = "Analyzing images..."
                    in 26..50 -> progressText.text = "Processing frames..."
                    in 51..75 -> progressText.text = "Applying effects..."
                    in 76..95 -> progressText.text = "Finalizing video..."
                    100 -> {
                        progressText.text = "Video generated successfully!"
                        completeVideoGeneration()
                        return
                    }
                }
                
                if (progress < 100) {
                    handler.postDelayed(this, 200) // Update every 200ms
                }
            }
        }
        
        handler.post(progressRunnable)
    }
    
    private fun completeVideoGeneration() {
        isGenerating = false
        generateButton.isEnabled = true
        generateButton.text = "✨ Generate"
        
        // Hide loading, show result
        loadingContainer.visibility = LinearLayout.GONE
        videoResultContainer.visibility = LinearLayout.VISIBLE
        
        // Setup video preview (placeholder)
        setupVideoPreview()
        
        Toast.makeText(this, "Video generated successfully!", Toast.LENGTH_LONG).show()
    }
    
    private fun setupVideoPreview() {
        // In a real app, you would load the generated video here
        // For now, we'll just setup the controls
        playPauseButton.text = "▶ Play"
        isVideoPlaying = false
    }
    
    private fun toggleVideoPlayback() {
        if (isVideoPlaying) {
            // Pause video
            videoPreview.pause()
            playPauseButton.text = "▶ Play"
            isVideoPlaying = false
        } else {
            // Play video
            videoPreview.start()
            playPauseButton.text = "⏸ Pause"
            isVideoPlaying = true
        }
    }
    
    private fun downloadVideo() {
        // In a real app, implement video download logic
        Toast.makeText(this, "Download feature will be implemented", Toast.LENGTH_SHORT).show()
    }
    
    private fun shareVideo() {
        // In a real app, implement video sharing logic
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this amazing video I created!")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Video"))
    }
    
    override fun onBackPressed() {
        if (isGenerating) {
            // Show confirmation dialog when generating
            Toast.makeText(this, "Video generation in progress. Please wait...", Toast.LENGTH_SHORT).show()
            return
        }
        super.onBackPressed()
    }
    
    private fun displayItemImage() {
        // Hiển thị ảnh từ TemplatesFragment hoặc Intent khác lên ImageView dataSimulation
        val templateImageResource = intent.getIntExtra("TEMPLATE_IMAGE_RESOURCE", -1)
        val itemImageResource = intent.getIntExtra("ITEM_IMAGE_RESOURCE", -1)
        
        when {
            templateImageResource != -1 -> {
                // Ảnh từ TemplatesFragment
                dataSimulation.setImageResource(templateImageResource)
                
                // Hiển thị thông tin template (optional)
                val templateTitle = intent.getStringExtra("TEMPLATE_TITLE")
                val templateBadge = intent.getStringExtra("TEMPLATE_BADGE")
                if (!templateTitle.isNullOrEmpty()) {
                    // Có thể hiển thị title trong Toast hoặc TextView nếu cần
                    Toast.makeText(this, "Template: $templateTitle", Toast.LENGTH_SHORT).show()
                }
            }
            itemImageResource != -1 -> {
                // Ảnh từ Intent khác (backward compatibility)
                dataSimulation.setImageResource(itemImageResource)
            }
            else -> {
                // Hiển thị placeholder nếu không có ảnh
                dataSimulation.setImageResource(R.drawable.ic_image_placeholder)
            }
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Images"))
    }
    
    private fun updateImagesDisplay() {
        selectedImagesAdapter.notifyDataSetChanged()
        updateImagesCount()
    }
    
    private fun updateImagesCount() {
        imagesCountText.text = "Selected Images (${selectedImages.size})"
    }
}