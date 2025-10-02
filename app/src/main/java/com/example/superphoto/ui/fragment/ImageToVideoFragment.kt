package com.example.superphoto.ui.fragment

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.superphoto.R
import com.example.superphoto.adapter.SelectedImageAdapter

/**
 * Image to Video Fragment - Placeholder for future video generation features
 * Ready for new API implementation
 */
class ImageToVideoFragment : Fragment() {

    // UI Elements
    private lateinit var uploadArea: LinearLayout
    private lateinit var refreshButton: ImageView
    private lateinit var promptEditText: EditText
    private lateinit var negativePromptEditText: EditText
    private lateinit var duration10s: TextView
    private lateinit var duration15s: TextView
    private lateinit var duration20s: TextView
    private lateinit var generateButton: Button
    private lateinit var imagesCountText: TextView
    private lateinit var addMoreImagesButton: Button
    private lateinit var selectedImagesContainer: LinearLayout
    private lateinit var rcvImgToVideo: RecyclerView
    private lateinit var selectedImageAdapter: SelectedImageAdapter
    private lateinit var videoPreviewSection: LinearLayout
    private lateinit var loadingContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var progressPercentage: TextView
    private lateinit var videoResultContainer: LinearLayout
    private lateinit var videoPreview: VideoView
    private lateinit var playPauseButton: Button
    private lateinit var downloadVideoButton: Button
    private lateinit var shareVideoButton: Button

    // State variables
    private var selectedDuration = 10
    private var selectedImageUri: Uri? = null
    private val selectedImages = mutableListOf<Uri>()
    private var isGenerating = false
    private var generatedVideoUri: Uri? = null
    private var isVideoPlaying = false

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                addImageToSelection(uri)
            }
        }
    }

    private val multipleImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                if (data.clipData != null) {
                    val clipData = data.clipData!!
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        addImageToSelection(uri)
                    }
                } else if (data.data != null) {
                    val uri = data.data!!
                    addImageToSelection(uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_to_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupClickListeners()
        setupRecyclerView()
        updateDurationSelection() // Set default 10s selection styling
        showReadyStatus()
    }

    private fun initViews(view: View) {
        uploadArea = view.findViewById(R.id.uploadArea)
        refreshButton = view.findViewById(R.id.refreshButton)
        promptEditText = view.findViewById(R.id.promptEditText)
        negativePromptEditText = view.findViewById(R.id.negativePromptEditText)
        duration10s = view.findViewById(R.id.duration10s)
        duration15s = view.findViewById(R.id.duration15s)
        duration20s = view.findViewById(R.id.duration20s)
        generateButton = view.findViewById(R.id.generateButton)
        imagesCountText = view.findViewById(R.id.imagesCountText)
        addMoreImagesButton = view.findViewById(R.id.addMoreImagesButton)
        selectedImagesContainer = view.findViewById(R.id.selectedImagesContainer)
        rcvImgToVideo = view.findViewById(R.id.rcv_img_to_video)
        videoPreviewSection = view.findViewById(R.id.videoPreviewSection)
        loadingContainer = view.findViewById(R.id.loadingContainer)
        progressBar = view.findViewById(R.id.progressBar)
        progressText = view.findViewById(R.id.progressText)
        progressPercentage = view.findViewById(R.id.progressPercentage)
        videoResultContainer = view.findViewById(R.id.videoResultContainer)
        videoPreview = view.findViewById(R.id.videoPreview)
        playPauseButton = view.findViewById(R.id.playPauseButton)
        downloadVideoButton = view.findViewById(R.id.downloadVideoButton)
        shareVideoButton = view.findViewById(R.id.shareVideoButton)
    }

    private fun setupClickListeners() {
        uploadArea.setOnClickListener { openImagePicker() }
        refreshButton.setOnClickListener { 
            animateRotation(refreshButton)
            refreshInterface() 
        }
        duration10s.setOnClickListener { selectDuration(10) }
        duration15s.setOnClickListener { selectDuration(15) }
        duration20s.setOnClickListener { selectDuration(20) }
        generateButton.setOnClickListener { generateVideo() }
        addMoreImagesButton.setOnClickListener { openMultipleImagePicker() }
        playPauseButton.setOnClickListener { toggleVideoPlayback() }
        downloadVideoButton.setOnClickListener { downloadVideo() }
        shareVideoButton.setOnClickListener { shareVideo() }
    }

    private fun showReadyStatus() {
        progressText.text = "🎬 Ready for new video generation API"
        progressText.visibility = View.VISIBLE
    }

    private fun selectDuration(duration: Int) {
        selectedDuration = duration
        updateDurationSelection()
    }

    private fun updateDurationSelection() {
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.ai_text_primary)
        val accentPurple = ContextCompat.getDrawable(requireContext(), R.drawable.ai_duration_button)
        val defaultBackground = ContextCompat.getDrawable(requireContext(), R.drawable.ai_duration_ripple)

        // Reset all durations to default state
        duration10s.setTextColor(primaryColor)
        duration15s.setTextColor(primaryColor)
        duration20s.setTextColor(primaryColor)
        duration10s.background = defaultBackground
        duration15s.background = defaultBackground
        duration20s.background = defaultBackground

        // Set selected duration with white text and purple background
        when (selectedDuration) {
            10 -> {
                duration10s.setTextColor(whiteColor)
                duration10s.background = accentPurple
            }
            15 -> {
                duration15s.setTextColor(whiteColor)
                duration15s.background = accentPurple
            }
            20 -> {
                duration20s.setTextColor(whiteColor)
                duration20s.background = accentPurple
            }
        }
    }

    private fun generateVideo() {
        val prompt = promptEditText.text.toString().trim()
        
        if (prompt.isEmpty() && selectedImages.isEmpty()) {
            showToast("Please enter a prompt or select images")
            return
        }

        showToast("Video generation will be implemented with new API")
        
        // Show generation parameters
        val params = buildString {
            append("🎬 Video Generation Parameters:\n")
            append("• Duration: ${selectedDuration}s\n")
            append("• Images: ${selectedImages.size}\n")
            if (prompt.isNotEmpty()) {
                append("• Prompt: $prompt\n")
            }
            val negativePrompt = negativePromptEditText.text.toString().trim()
            if (negativePrompt.isNotEmpty()) {
                append("• Negative prompt: $negativePrompt\n")
            }
            append("\nReady for video processing!")
        }
        
        progressText.text = params
        videoPreviewSection.visibility = View.VISIBLE
        videoResultContainer.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        selectedImageAdapter = SelectedImageAdapter(selectedImages) { position ->
            removeImageFromSelection(position)
        }
        rcvImgToVideo.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rcvImgToVideo.adapter = selectedImageAdapter
    }

    private fun addImageToSelection(uri: Uri) {
        if (selectedImages.size < 10) {
            selectedImages.add(uri)
            selectedImageAdapter.notifyItemInserted(selectedImages.size - 1)
            updateSelectedImagesVisibility()
            updateImagesCount()
        } else {
            showToast("Maximum 10 images allowed")
        }
    }

    private fun removeImageFromSelection(position: Int) {
        if (position < selectedImages.size) {
            selectedImages.removeAt(position)
            selectedImageAdapter.notifyItemRemoved(position)
            updateSelectedImagesVisibility()
            updateImagesCount()
        }
    }

    private fun updateSelectedImagesVisibility() {
        selectedImagesContainer.visibility = if (selectedImages.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateImagesCount() {
        imagesCountText.text = "${selectedImages.size}/10 images selected"
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun openMultipleImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        multipleImagePickerLauncher.launch(intent)
    }

    private fun refreshInterface() {
        selectedImages.clear()
        selectedImageAdapter.notifyDataSetChanged()
        promptEditText.text.clear()
        negativePromptEditText.text.clear()
        selectedDuration = 10
        updateDurationSelection()
        updateSelectedImagesVisibility()
        updateImagesCount()
        videoPreviewSection.visibility = View.GONE
        videoResultContainer.visibility = View.GONE
        showReadyStatus()
        showToast("Interface refreshed")
    }

    private fun animateRotation(view: View) {
        val rotation = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        rotation.duration = 500
        rotation.interpolator = AccelerateDecelerateInterpolator()
        rotation.start()
    }

    private fun toggleVideoPlayback() {
        showToast("Video playback will be implemented with new API")
    }

    private fun downloadVideo() {
        showToast("Video download will be implemented with new API")
    }

    private fun shareVideo() {
        showToast("Video sharing will be implemented with new API")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance(): ImageToVideoFragment {
            return ImageToVideoFragment()
        }
    }
}