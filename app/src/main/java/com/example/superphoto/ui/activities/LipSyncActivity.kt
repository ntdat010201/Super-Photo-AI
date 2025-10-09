package com.example.superphoto.ui.activities

import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodec
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import com.example.superphoto.R
import com.example.superphoto.ui.views.VideoTimelineView
import com.example.superphoto.models.AudioTrack
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LipSyncActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var videoView: VideoView
    private lateinit var backButton: ImageView
    private lateinit var applyButton: Button
    private lateinit var playPauseButton: ImageView
    private lateinit var undoButton: ImageView
    private lateinit var redoButton: ImageView
    private lateinit var fullscreenButton: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var timelineSeekBar: SeekBar
    private lateinit var timeDisplay: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var addSpeechButton: LinearLayout
    private lateinit var characterAvatar: ImageView
    private lateinit var characterName: TextView
    private lateinit var videoTimelineView: VideoTimelineView
    private lateinit var trimButton: Button
    private lateinit var okCutButton: Button

    // State variables
    private var videoUri: Uri? = null
    private var audioUri: Uri? = null
    private var enhanceQuality: Boolean = false
    private var preserveExpression: Boolean = false
    private var isPlaying = false
    private var isFullscreen = false
    private var videoDuration = 0
    private var currentPosition = 0
    private var isTrimModeActive = false // Trạng thái trim mode
    private var lastPlayheadPosition: Long = 0L
    private var isVideoInitialized = false
    // MediaPlayer for audio playback
    private var mediaPlayer: MediaPlayer? = null
    
    // Audio track management
    private val audioTracks = mutableListOf<AudioTrack>()
    private var selectedAudioUri: Uri? = null
    
    // File picker for audio selection
    private lateinit var audioPickerLauncher: ActivityResultLauncher<Intent>

    // Handler for updating timeline
    private val handler = Handler(Looper.getMainLooper())
    private var updateTimelineRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isPlaying && videoView.isPlaying) {
                currentPosition = videoView.currentPosition
                timelineSeekBar.progress = currentPosition
                updateTimeDisplay()
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lip_sync)

        // Initialize audio file picker
        initializeAudioPicker()

        // Get data from intent
        getIntentData()

        // Initialize views
        initViews()

        // Setup listeners
        setupClickListeners()

        // Setup video
        setupVideo()

        // Setup timeline
        setupTimeline()
    }

    private fun getIntentData() {
        videoUri = intent.getStringExtra("video_uri")?.let { Uri.parse(it) }
        audioUri = intent.getStringExtra("audio_uri")?.let { Uri.parse(it) }
        enhanceQuality = intent.getBooleanExtra("enhance_quality", false)
        preserveExpression = intent.getBooleanExtra("preserve_expression", false)
    }

    private fun initializeAudioPicker() {
        audioPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedAudioUri = uri
                    processSelectedAudio(uri)
                }
            }
        }
    }

    private fun initViews() {
        videoView = findViewById(R.id.videoView)
        backButton = findViewById(R.id.backButton)
        applyButton = findViewById(R.id.applyButton)
        playPauseButton = findViewById(R.id.playPauseButton)
        undoButton = findViewById(R.id.undoButton)
        redoButton = findViewById(R.id.redoButton)
        fullscreenButton = findViewById(R.id.fullscreenButton)
        seekBar = findViewById(R.id.timelineSeekBar)
        timelineSeekBar = findViewById(R.id.timelineSeekBar)
        timeDisplay = findViewById(R.id.currentTimeText)
        totalTimeText = findViewById(R.id.totalTimeText)
        addSpeechButton = findViewById(R.id.addSpeechButton)
        characterAvatar = findViewById(R.id.characterAvatar)
        characterName = findViewById(R.id.characterName)
        videoTimelineView = findViewById(R.id.videoTimelineView)
        trimButton = findViewById(R.id.trimButton)
        okCutButton = findViewById(R.id.okCut)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        applyButton.setOnClickListener {
            applyLipSync()
        }

        playPauseButton.setOnClickListener {
            togglePlayPause()
        }

        undoButton.setOnClickListener {
            // Implement undo functionality
            Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show()
        }

        redoButton.setOnClickListener {
            // Implement redo functionality
            Toast.makeText(this, "Redo", Toast.LENGTH_SHORT).show()
        }

        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        addSpeechButton.setOnClickListener {
            showAddSpeechDialog()
        }

        videoView.setOnClickListener {
            togglePlayPause()
        }

        trimButton.setOnClickListener {
            toggleTrimMode()
        }

        okCutButton.setOnClickListener {
            cutVideo()
        }
    }

    private fun setupVideo() {
        videoUri?.let { uri ->
            videoView.setVideoURI(uri)

            videoView.setOnPreparedListener { mediaPlayer ->
                videoDuration = mediaPlayer.duration
                timelineSeekBar.max = videoDuration
                totalTimeText.text = formatTime(videoDuration)
                timeDisplay.text = "00:00"

                // Setup video timeline view
                videoTimelineView.setVideoDuration(videoDuration.toLong())
                videoTimelineView.setVideoPath(uri.toString())
                videoTimelineView.setOnSeekListener { position ->
                    videoView.seekTo(position.toInt())
                    currentPosition = position.toInt()
                    updateTimeDisplay()
                }

                // Set listener cho trim change
                videoTimelineView.onTrimChangeListener = { startMs, endMs ->
                    // Seek video đến start
                    videoView.seekTo(startMs.toInt())

                    // Optional: Loop playback giữa start và end
                    handler.removeCallbacks(updateTimelineRunnable)  // Stop old runnable
                    updateTimelineRunnable = object : Runnable {
                        override fun run() {
                            if (isPlaying && videoView.isPlaying) {
                                currentPosition = videoView.currentPosition
                                if (currentPosition >= endMs.toInt()) {
                                    videoView.seekTo(startMs.toInt())  // Reset to start
                                }
                                timelineSeekBar.progress = currentPosition
                                updateTimeDisplay()
                                handler.postDelayed(this, 100)
                            }
                        }
                    }

                    // Cập nhật seekbar max thành end - start
                    timelineSeekBar.max = (endMs - startMs).toInt()
                    Toast.makeText(this, "Trim: $startMs to $endMs ms", Toast.LENGTH_SHORT).show()
                }

                // Chỉ gọi showFirstFrame nếu video chưa được khởi tạo
                if (!isVideoInitialized) {
                    showFirstFrame()
                    isVideoInitialized = true
                }
            }

            videoView.setOnCompletionListener {
                isPlaying = false
                playPauseButton.setImageResource(R.drawable.ic_play)
                handler.removeCallbacks(updateTimelineRunnable)
            }

            videoView.setOnErrorListener { _, what, extra ->
                Toast.makeText(this, "Error playing video: $what, $extra", Toast.LENGTH_LONG).show()
                true
            }
        }
    }

    private fun setupTimeline() {
        // Setup seekbar listener
        timelineSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                    currentPosition = progress
                    updateTimeDisplay()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            videoView.pause()
            playPauseButton.setImageResource(R.drawable.ic_play)
            handler.removeCallbacks(updateTimelineRunnable)
        } else {
            videoView.start()
            playPauseButton.setImageResource(R.drawable.ic_pause)
            handler.post(updateTimelineRunnable)
        }
        isPlaying = !isPlaying
    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            // Exit fullscreen
            WindowCompat.setDecorFitsSystemWindows(window, true)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
            fullscreenButton.setImageResource(R.drawable.ic_fullscreen)
        } else {
            // Enter fullscreen
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            fullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit)
        }
        isFullscreen = !isFullscreen
    }

    private fun updateTimeDisplay() {
        timeDisplay.text = formatTime(currentPosition)

        // Update playhead position in timeline view
        if (videoTimelineView.getPlayheadPosition() != currentPosition.toLong()) {
            videoTimelineView.setPlayheadPosition(currentPosition.toLong())
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun toggleTrimMode() {
        isTrimModeActive = !isTrimModeActive
        
        if (isTrimModeActive) {
            // Bật trim mode
            videoTimelineView.enableTrimMode(true)
            trimButton.text = "Thoát Trim"
            trimButton.setBackgroundColor(getColor(R.color.red)) // Đổi màu để hiển thị trạng thái active
            Toast.makeText(this, "Chế độ Trim: Chỉ cuộn timeline, không click để seek", Toast.LENGTH_SHORT).show()
        } else {
            // Tắt trim mode
            videoTimelineView.enableTrimMode(false)
            trimButton.text = "Trim Video"
            trimButton.setBackgroundColor(getColor(R.color.ai_accent)) // Màu mặc định
            Toast.makeText(this, "Đã thoát chế độ Trim", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cutVideo() {
        if (!isTrimModeActive) {
            Toast.makeText(this, "Vui lòng bật chế độ Trim trước khi cắt video", Toast.LENGTH_SHORT).show()
            return
        }

        val startMs = videoTimelineView.getTrimStartMs()
        val endMs = videoTimelineView.getTrimEndMs()
        
        if (startMs >= endMs) {
            Toast.makeText(this, "Vùng cắt không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Đang cắt video...", Toast.LENGTH_SHORT).show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = trimVideoSegment(videoUri, startMs, endMs)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@LipSyncActivity, "Cắt video thành công!", Toast.LENGTH_LONG).show()
                        // Tắt trim mode sau khi cắt thành công
                        isTrimModeActive = false
                        videoTimelineView.enableTrimMode(false)
                        trimButton.text = "Trim Video"
                        trimButton.setBackgroundColor(getColor(R.color.ai_text_primary))
                    } else {
                        Toast.makeText(this@LipSyncActivity, "Lỗi khi cắt video", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LipSyncActivity, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun trimVideoSegment(videoUri: Uri?, startMs: Long, endMs: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (videoUri == null) return@withContext false

                val inputPath = getRealPathFromURI(videoUri) ?: return@withContext false
                val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "SuperPhoto")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                
                val outputFile = File(outputDir, "trimmed_video_${System.currentTimeMillis()}.mp4")
                
                val extractor = MediaExtractor()
                extractor.setDataSource(inputPath)
                
                val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                
                var videoTrackIndex = -1
                var audioTrackIndex = -1
                var muxerVideoTrackIndex = -1
                var muxerAudioTrackIndex = -1
                
                // Tìm video và audio tracks
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    
                    when {
                        mime.startsWith("video/") -> {
                            videoTrackIndex = i
                            muxerVideoTrackIndex = muxer.addTrack(format)
                        }
                        mime.startsWith("audio/") -> {
                            audioTrackIndex = i
                            muxerAudioTrackIndex = muxer.addTrack(format)
                        }
                    }
                }
                
                muxer.start()
                
                // Xử lý video track
                if (videoTrackIndex >= 0) {
                    processTrack(extractor, muxer, videoTrackIndex, muxerVideoTrackIndex, startMs * 1000, endMs * 1000)
                }
                
                // Xử lý audio track
                if (audioTrackIndex >= 0) {
                    processTrack(extractor, muxer, audioTrackIndex, muxerAudioTrackIndex, startMs * 1000, endMs * 1000)
                }
                
                muxer.stop()
                muxer.release()
                extractor.release()
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun processTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        extractorTrackIndex: Int,
        muxerTrackIndex: Int,
        startTimeUs: Long,
        endTimeUs: Long
    ) {
        extractor.selectTrack(extractorTrackIndex)
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break

            val sampleTime = extractor.sampleTime
            if (sampleTime > endTimeUs) break

            if (sampleTime >= startTimeUs) {
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = sampleTime - startTimeUs

                bufferInfo.flags = try {
                    extractor.getSampleFlags()
                } catch (e: NoSuchMethodError) {
                    // Nếu thiết bị Android quá cũ không hỗ trợ getSampleFlags()
                    0
                }

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            }

            extractor.advance()
        }

        extractor.unselectTrack(extractorTrackIndex)
    }


    private fun getRealPathFromURI(uri: Uri): String? {
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(android.provider.MediaStore.Video.Media.DATA)
                    if (columnIndex >= 0) {
                        it.getString(columnIndex)
                    } else {
                        uri.path
                    }
                } else {
                    uri.path
                }
            }
        } catch (e: Exception) {
            uri.path
        }
    }

    private fun showAddSpeechDialog() {
        // Lưu vị trí playhead trước khi mở dialog
        lastPlayheadPosition = videoTimelineView.getPlayheadPosition()
        // Create dialog for character selection and speech input
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_speech, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        val characterRecyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.characterRecyclerView)
        val selectedAudioText = dialogView.findViewById<TextView>(R.id.selectedAudioText)
        val audioDurationText = dialogView.findViewById<TextView>(R.id.audioDurationText)
        val selectAudioButton = dialogView.findViewById<Button>(R.id.selectAudioButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val addButton = dialogView.findViewById<Button>(R.id.addButton)

        // Reset selected audio for new dialog
        selectedAudioUri = null
        selectedAudioText.text = "No audio file selected"
        audioDurationText.visibility = View.GONE

        // Setup character selection (you can implement RecyclerView adapter here)

        selectAudioButton.setOnClickListener {
            openAudioFilePicker()
        }

        addButton.setOnClickListener {
            when {
                selectedAudioUri != null -> {
                    addAudioToTimeline(selectedAudioUri!!)
                    dialog.dismiss()
                }
                else -> {
                    Toast.makeText(this, "Please select an audio file or enter speech text", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cancelButton.setOnClickListener {
            videoTimelineView.setPlayheadPosition(lastPlayheadPosition)
            dialog.dismiss()
        }

        // Store dialog reference for updating UI
        currentSpeechDialog = dialog
        currentDialogViews = DialogViews(selectedAudioText, audioDurationText)
        dialog.show()
    }

    // Helper class to store dialog views
    private data class DialogViews(
        val selectedAudioText: TextView,
        val audioDurationText: TextView
    )

    private var currentSpeechDialog: androidx.appcompat.app.AlertDialog? = null
    private var currentDialogViews: DialogViews? = null

    private fun openAudioFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        audioPickerLauncher.launch(Intent.createChooser(intent, "Select Audio File"))
    }

    private fun processSelectedAudio(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val audioInfo = getAudioInfo(uri)
                withContext(Dispatchers.Main) {
                    updateDialogWithAudioInfo(audioInfo)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LipSyncActivity, "Error processing audio file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getAudioInfo(uri: Uri): AudioInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown"
            val displayName = getFileNameFromUri(uri) ?: title
            
            AudioInfo(displayName, duration)
        } finally {
            retriever.release()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else null
        }
    }

    private fun updateDialogWithAudioInfo(audioInfo: AudioInfo) {
        currentDialogViews?.let { views ->
            views.selectedAudioText.text = audioInfo.fileName
            views.audioDurationText.text = "Duration: ${formatTime(audioInfo.duration.toInt())}"
            views.audioDurationText.visibility = View.VISIBLE
        }
    }

    private fun addAudioToTimeline(audioUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val audioInfo = getAudioInfo(audioUri)
                val audioTrack = AudioTrack(
                    id = UUID.randomUUID().toString(),
                    uri = audioUri,
                    filePath = getRealPathFromURI(audioUri) ?: "",
                    fileName = audioInfo.fileName,
                    duration = audioInfo.duration,
                    startPosition = lastPlayheadPosition, // Sử dụng vị trí playhead đã lưu
                    endPosition = lastPlayheadPosition + audioInfo.duration
                )

                withContext(Dispatchers.Main) {
                    audioTracks.add(audioTrack)
                    videoTimelineView.addAudioTrack(audioTrack)

                    // Khôi phục vị trí playhead
                    videoTimelineView.setPlayheadPosition(lastPlayheadPosition)
                    videoView.seekTo(lastPlayheadPosition.toInt())
                    currentPosition = lastPlayheadPosition.toInt()
                    updateTimeDisplay()

                    Toast.makeText(this@LipSyncActivity, "Audio added to timeline: ${audioInfo.fileName}", Toast.LENGTH_SHORT).show()

                    characterName.text = "Audio Track"
                    characterAvatar.setImageResource(R.drawable.ic_audio_file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LipSyncActivity, "Error adding audio to timeline: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addSpeechToTimeline(speechText: String) {
        // Add speech marker to timeline at current position (future TTS implementation)
        Toast.makeText(this, "Speech added: $speechText", Toast.LENGTH_SHORT).show()

        // Update character info
        characterName.text = "Character 1"
        characterAvatar.setImageResource(R.drawable.ic_character_avatar)
    }

    // Data class for audio information
    private data class AudioInfo(
        val fileName: String,
        val duration: Long
    )

    private fun showFirstFrame() {
        // Seek to the beginning and pause to show first frame
        videoView.seekTo(0)
        currentPosition = 0

        // Start the video briefly to load the first frame, then pause immediately
        videoView.start()

        // Pause after a very short delay to ensure first frame is displayed
        handler.postDelayed({
            if (videoView.isPlaying && !isPlaying) {
                videoView.pause()
            }
        }, 50) // 50ms delay to ensure frame is loaded

        updateTimeDisplay()
    }

    private fun applyLipSync() {
        // Apply lip sync processing
        Toast.makeText(this, "Applying lip sync...", Toast.LENGTH_SHORT).show()

        // Here you would implement the actual lip sync processing
        // For now, just show a success message
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Lip sync applied successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimelineRunnable)
        mediaPlayer?.release()
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            videoView.pause()
            playPauseButton.setImageResource(R.drawable.ic_play)
            isPlaying = false
            handler.removeCallbacks(updateTimelineRunnable)
        }
    }
}