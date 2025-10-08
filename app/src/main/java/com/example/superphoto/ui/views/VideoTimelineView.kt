package com.example.superphoto.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.superphoto.R
import kotlin.math.*

class VideoTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Zoom levels and frame intervals
    private val zoomLevels = arrayOf(1f, 2f, 4f, 5f)
    private val frameIntervals = arrayOf(1000L, 500L, 250L, 200L) // milliseconds per frame

    private var currentZoomLevel = 0 // Index in zoomLevels array
    private var currentZoom = zoomLevels[currentZoomLevel]

    // Video properties
    private var videoUri: Uri? = null
    private var videoDuration: Long = 0L
    private var currentPosition: Long = 0L

    // UI dimensions - Optimized for performance and quality
    private val thumbnailWidth = 240f  // Balanced size for performance
    private val thumbnailHeight = 160f  // Maintain 16:9 aspect ratio
    private val thumbnailSpacing = 10f  // Optimal spacing
    private var thumbnails = mutableListOf<Bitmap?>()
    private var thumbnailPositions = mutableListOf<Long>() // Time positions in milliseconds

    // Thumbnail caching for different zoom levels
    private val thumbnailCache = mutableMapOf<Int, Pair<MutableList<Bitmap?>, MutableList<Long>>>()

    // Scroll and gesture handling with smooth animation
    private var scrollX = 0f
    private var maxScrollX = 0f
    private var scaleDetector: ScaleGestureDetector
    private var gestureDetector: GestureDetector
    private var scrollAnimator: ValueAnimator? = null
    private var isScrolling = false

    // Throttling for smooth scrolling
    private var lastScrollTime = 0L
    private val scrollThrottleMs = 16L // ~60fps
    private var pendingScrollUpdate = false
    private var isSettingPositionExternally = false

    // Paint objects
    private val thumbnailPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ai_border)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ai_text_secondary)
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }
    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        strokeWidth = 4f
    }

    // Playhead position
    private var playheadX = 0f

    // Listener for position changes
    var onPositionChangeListener: ((Long) -> Unit)? = null

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())

        // Enable touch events
        isClickable = true
        isFocusable = true
    }

    fun setVideoUri(uri: Uri) {
        videoUri = uri
        // Clear cache when video changes
        thumbnailCache.clear()
        extractVideoInfo()
        generateThumbnails()
        invalidate()
    }

    fun setVideoDuration(duration: Long) {
        this.videoDuration = duration
        generateThumbnails()
        calculateMaxScroll()
        invalidate()
    }

    fun setVideoPath(path: String) {
        videoUri = Uri.parse(path)
        // Clear cache when video changes
        thumbnailCache.clear()
        extractVideoInfo()
        generateThumbnails()
        invalidate()
    }

    fun setOnSeekListener(listener: (Long) -> Unit) {
        this.onPositionChangeListener = listener
    }

    fun setCurrentPosition(position: Long) {
        isSettingPositionExternally = true
        this.currentPosition = position
        updatePlayheadPosition()
        // Scroll timeline to center this position at the playhead
        scrollToPosition(position)
        invalidate()
        isSettingPositionExternally = false
    }

    fun setPlayheadPosition(position: Long) {
        isSettingPositionExternally = true
        this.currentPosition = position
        updatePlayheadPosition()
        // Scroll timeline to center this position at the playhead
        scrollToPosition(position)
        invalidate()
        isSettingPositionExternally = false
    }

    private fun extractVideoInfo() {
        videoUri?.let { uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                videoDuration = duration?.toLongOrNull() ?: 0L

                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
                videoDuration = 30000L // Default 30 seconds if extraction fails
            }
        }
    }

    private fun generateThumbnails() {
        // Check cache first
        val cachedData = thumbnailCache[currentZoomLevel]
        if (cachedData != null) {
            thumbnails = cachedData.first
            thumbnailPositions = cachedData.second
            calculateMaxScroll()
            Log.d("VideoTimelineView", "Using cached thumbnails for zoom level ${currentZoomLevel + 1}x")
            return
        }

        thumbnails.clear()
        thumbnailPositions.clear()

        if (videoDuration <= 0) return

        val frameInterval = frameIntervals[currentZoomLevel]
        val totalFrames = (videoDuration / frameInterval).toInt()

        Log.d("VideoTimelineView", "Generating thumbnails - Zoom Level: ${currentZoomLevel + 1}x, Frame Interval: ${frameInterval}ms, Total Frames: $totalFrames, Video Duration: ${videoDuration}ms")

        videoUri?.let { uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                for (i in 0 until totalFrames) {
                    val timeUs = (i * frameInterval * 1000) // Convert to microseconds
                    if (timeUs <= videoDuration * 1000) {
                        try {
                            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            val scaledBitmap = bitmap?.let { originalBitmap ->
                                // Calculate proper scaling to maintain aspect ratio
                                val originalWidth = originalBitmap.width.toFloat()
                                val originalHeight = originalBitmap.height.toFloat()
                                val targetWidth = thumbnailWidth.toInt()
                                val targetHeight = thumbnailHeight.toInt()

                                // Calculate scale to fit within bounds while maintaining aspect ratio
                                val scaleX = targetWidth / originalWidth
                                val scaleY = targetHeight / originalHeight
                                val scale = minOf(scaleX, scaleY)

                                val scaledWidth = (originalWidth * scale).toInt()
                                val scaledHeight = (originalHeight * scale).toInt()

                                // Create scaled bitmap with proper filtering
                                Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
                            }
                            thumbnails.add(scaledBitmap)
                            thumbnailPositions.add(i * frameInterval)
                        } catch (e: Exception) {
                            thumbnails.add(null)
                            thumbnailPositions.add(i * frameInterval)
                        }
                    }
                }

                retriever.release()
                calculateMaxScroll()

                // Cache the generated thumbnails for this zoom level
                thumbnailCache[currentZoomLevel] = Pair(
                    thumbnails.toMutableList(),
                    thumbnailPositions.toMutableList()
                )
                Log.d("VideoTimelineView", "Cached thumbnails for zoom level ${currentZoomLevel + 1}x")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateMaxScroll() {
        val totalWidth = thumbnails.size * (thumbnailWidth + thumbnailSpacing)
        maxScrollX = max(0f, totalWidth - width)
    }

    private fun updatePlayheadPosition() {
        // Playhead is now fixed at the center of the screen
        playheadX = width / 2f
    }

    private fun calculateCurrentPositionFromScroll() {
        // Don't update position if we're setting it externally or if no thumbnails
        if (isSettingPositionExternally || thumbnailPositions.isEmpty()) return

        // Only update position if we're actively scrolling to avoid conflicts
        if (!isScrolling) return

        // Calculate which frame is at the center (playhead position)
        val centerX = width / 2f + scrollX
        val frameIndex = (centerX / (thumbnailWidth + thumbnailSpacing)).toInt()

        if (frameIndex >= 0 && frameIndex < thumbnailPositions.size) {
            val newPosition = thumbnailPositions[frameIndex]
            // Only update if position actually changed to avoid unnecessary updates
            if (newPosition != currentPosition) {
                currentPosition = newPosition
            }
        }
    }

    private fun scrollToPosition(position: Long) {
        // Find the frame index for this position
        val frameIndex = thumbnailPositions.indexOfFirst { it >= position }
        if (frameIndex >= 0) {
            // Calculate the scroll position to center this frame at the playhead
            val frameX = frameIndex * (thumbnailWidth + thumbnailSpacing)
            val targetScrollX = (frameX - width / 2f).coerceIn(0f, maxScrollX)

            // Only scroll if we're not already scrolling manually
            if (!isScrolling) {
                scrollX = targetScrollX
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateMaxScroll()
        updatePlayheadPosition()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw thumbnails with optimized layout for larger size
        var x = -scrollX
        val topMargin = 20f // More space at top for larger thumbnails
        for (i in thumbnails.indices) {
            val thumbnail = thumbnails[i]
            val rect = RectF(x, topMargin, x + thumbnailWidth, topMargin + thumbnailHeight)

            if (thumbnail != null) {
                // Calculate centered position to maintain aspect ratio
                val bitmapWidth = thumbnail.width.toFloat()
                val bitmapHeight = thumbnail.height.toFloat()
                val centerX = rect.centerX()
                val centerY = rect.centerY()

                val drawRect = RectF(
                    centerX - bitmapWidth / 2,
                    centerY - bitmapHeight / 2,
                    centerX + bitmapWidth / 2,
                    centerY + bitmapHeight / 2
                )

                // Draw bitmap with proper aspect ratio
                canvas.drawBitmap(thumbnail, null, drawRect, thumbnailPaint)
            } else {
                // Draw placeholder with better visibility
                canvas.drawRect(rect, borderPaint)
                // Add loading indicator
                canvas.drawText("...", x + thumbnailWidth / 2, topMargin + thumbnailHeight / 2, timePaint)
            }

            // Draw border with better visibility
            canvas.drawRect(rect, borderPaint)

            // Draw time markers - only show at main frames (every second)
            val timePosition = thumbnailPositions.getOrNull(i) ?: 0L
            // Only show time text at exact second positions (0ms, 1000ms, 2000ms, etc.)
            if (timePosition % 1000L == 0L) {
                val timeText = formatTime(timePosition, i)
                canvas.drawText(timeText, x + thumbnailWidth / 2, rect.bottom + 40f, timePaint)
            }

            x += thumbnailWidth + thumbnailSpacing
        }

        // Draw playhead
        if (playheadX >= 0 && playheadX <= width) {
            canvas.drawLine(playheadX, 0f, playheadX, height.toFloat(), playheadPaint)
        }

        // Draw zoom level indicator
        val zoomText = "${currentZoom.toInt()}x"
        canvas.drawText(zoomText, width - 50f, 30f, timePaint)
    }

    private fun formatTime(milliseconds: Long, index: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled

        when (event.action) {
            MotionEvent.ACTION_UP -> {
                // Handle tap to seek
                if (!scaleDetector.isInProgress) {
                    handleTap(event.x)
                }
            }
        }

        return handled || super.onTouchEvent(event)
    }

    private fun handleTap(x: Float) {
        // Calculate which frame was tapped and scroll to center it at playhead
        val adjustedX = x + scrollX
        val frameIndex = (adjustedX / (thumbnailWidth + thumbnailSpacing)).toInt()

        if (frameIndex >= 0 && frameIndex < thumbnailPositions.size) {
            // Calculate scroll position to center the tapped frame at playhead
            val targetFrameX = frameIndex * (thumbnailWidth + thumbnailSpacing)
            val targetScrollX = (targetFrameX - width / 2f).coerceIn(0f, maxScrollX)

            // Animate scroll to center the tapped frame
            startSmoothScroll(targetScrollX - scrollX)
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor

            // More sensitive zoom thresholds for smoother experience
            if (scaleFactor > 1.03f && currentZoomLevel < zoomLevels.size - 1) {
                // Zoom in with smooth transition
                currentZoomLevel++
                currentZoom = zoomLevels[currentZoomLevel]

                // Use cached thumbnails if available for instant response
                val cached = thumbnailCache[currentZoomLevel]
                if (cached != null) {
                    thumbnails = cached.first
                    thumbnailPositions = cached.second
                    calculateMaxScroll()
                    invalidate()
                } else {
                    generateThumbnails()
                }
                return true
            } else if (scaleFactor < 0.97f && currentZoomLevel > 0) {
                // Zoom out with smooth transition
                currentZoomLevel--
                currentZoom = zoomLevels[currentZoomLevel]

                // Use cached thumbnails if available for instant response
                val cached = thumbnailCache[currentZoomLevel]
                if (cached != null) {
                    thumbnails = cached.first
                    thumbnailPositions = cached.second
                    calculateMaxScroll()
                    invalidate()
                } else {
                    generateThumbnails()
                }
                return true
            }

            return false
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Cancel any ongoing scroll animation
            scrollAnimator?.cancel()
            isScrolling = true

            // Reduce scroll sensitivity for smoother experience (60% of original)
            val adjustedDistanceX = distanceX * 0.6f
            scrollX = (scrollX + adjustedDistanceX).coerceIn(0f, maxScrollX)
            updatePlayheadPosition()

            // Throttle expensive operations for smoother scrolling
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScrollTime >= scrollThrottleMs) {
                calculateCurrentPositionFromScroll()
                onPositionChangeListener?.invoke(currentPosition)
                lastScrollTime = currentTime
                pendingScrollUpdate = false
            } else {
                pendingScrollUpdate = true
                // Schedule delayed update if needed
                postDelayed({
                    if (pendingScrollUpdate && !isScrolling) {
                        calculateCurrentPositionFromScroll()
                        onPositionChangeListener?.invoke(currentPosition)
                        pendingScrollUpdate = false
                    }
                }, scrollThrottleMs)
            }

            // Use postInvalidateOnAnimation for smoother rendering
            postInvalidateOnAnimation()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // Handle fling gesture for smooth scrolling with animation
            if (abs(velocityX) > 100) {
                startSmoothScroll(-velocityX * 0.2f) // Reduce fling sensitivity
            }

            // Reset scrolling state and handle pending updates
            isScrolling = false
            if (pendingScrollUpdate) {
                calculateCurrentPositionFromScroll()
                onPositionChangeListener?.invoke(currentPosition)
                pendingScrollUpdate = false
            }
            return true
        }
    }

    private fun startSmoothScroll(distance: Float) {
        scrollAnimator?.cancel()

        val startScrollX = scrollX
        val targetScrollX = (scrollX + distance).coerceIn(0f, maxScrollX)

        if (abs(targetScrollX - startScrollX) < 1f) return

        scrollAnimator = ValueAnimator.ofFloat(startScrollX, targetScrollX).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                scrollX = animator.animatedValue as Float
                updatePlayheadPosition()
                calculateCurrentPositionFromScroll()
                onPositionChangeListener?.invoke(currentPosition)
                // Use postInvalidateOnAnimation for smoother animation
                postInvalidateOnAnimation()
            }
            start()
        }
    }
}