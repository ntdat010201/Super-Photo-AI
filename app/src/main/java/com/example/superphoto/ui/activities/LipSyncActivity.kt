package com.example.superphoto.ui.activities

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.superphoto.R
import com.example.superphoto.ui.views.VideoTimelineView

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

    // Media player for audio playback
    private var mediaPlayer: MediaPlayer? = null

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

                // Show first frame immediately
                showFirstFrame()
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
        videoTimelineView.setPlayheadPosition(currentPosition.toLong())
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
            trimButton.setBackgroundColor(getColor(R.color.ai_text_primary)) // Màu mặc định
            Toast.makeText(this, "Đã thoát chế độ Trim", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddSpeechDialog() {
        // Create dialog for character selection and speech input
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_speech, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        val characterRecyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.characterRecyclerView)
        val speechEditText = dialogView.findViewById<EditText>(R.id.speechEditText)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val addButton = dialogView.findViewById<Button>(R.id.addButton)

        // Setup character selection (you can implement RecyclerView adapter here)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        addButton.setOnClickListener {
            val speechText = speechEditText.text.toString()
            if (speechText.isNotEmpty()) {
                addSpeechToTimeline(speechText)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter speech text", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun addSpeechToTimeline(speechText: String) {
        // Add speech marker to timeline at current position
        Toast.makeText(this, "Speech added: $speechText", Toast.LENGTH_SHORT).show()

        // Update character info
        characterName.text = "Character 1"
        characterAvatar.setImageResource(R.drawable.ic_character_avatar)
    }

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