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
    private var minScrollX = 0f
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
        strokeWidth = 10f
        style = Paint.Style.STROKE
    }
    private var playheadX = 0f // Fixed position at center

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
        // Scroll timeline to center this position
        scrollToPosition(position)
        invalidate()
        isSettingPositionExternally = false
    }

    fun setPlayheadPosition(position: Long) {
        isSettingPositionExternally = true
        this.currentPosition = position
        // Scroll timeline to center this position
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
        // Professional video editor scroll bounds:
        // - Left max: first frame start aligns with playhead (center)
        // - Right max: last frame end aligns with playhead (center)
        minScrollX = -(width / 2f)
        maxScrollX = totalWidth - (width / 2f)
    }

    // updatePlayheadPosition() removed - no more playhead logic

    private fun calculateCurrentPositionFromScroll() {
        // Chỉ để trống - không cập nhật position nữa
        // Chỉ cuộn mượt mà, PlayheadPaint đứng yên ở giữa
    }

    private fun calculateCurrentPositionFromPlayhead(): Long {
        if (thumbnailPositions.isEmpty()) return 0L
        
        // Calculate which frame is at the playhead position (center of screen)
        val playheadPositionInTimeline = playheadX + scrollX
        val frameWidth = thumbnailWidth + thumbnailSpacing
        val exactFramePosition = playheadPositionInTimeline / frameWidth
        
        // Get the current frame index and interpolation factor
        val currentFrameIndex = exactFramePosition.toInt()
        val interpolationFactor = exactFramePosition - currentFrameIndex
        
        return if (currentFrameIndex >= 0 && currentFrameIndex < thumbnailPositions.size) {
            // Calculate the exact time position based on interpolation
            val currentFrameTime = thumbnailPositions[currentFrameIndex]
            val nextFrameIndex = currentFrameIndex + 1
            
            if (nextFrameIndex < thumbnailPositions.size) {
                val nextFrameTime = thumbnailPositions[nextFrameIndex]
                val timeDifference = nextFrameTime - currentFrameTime
                currentFrameTime + (timeDifference * interpolationFactor).toLong()
            } else {
                currentFrameTime
            }
        } else {
            0L
        }
    }

    private fun scrollToPosition(position: Long) {
        if (thumbnailPositions.isEmpty()) return
        
        // Find the closest frame to this position
        val frameIndex = thumbnailPositions.indexOfFirst { it >= position }
            .takeIf { it != -1 } ?: (thumbnailPositions.size - 1)
        
        // Calculate scroll position to center this frame under the playhead
        val frameWidth = thumbnailWidth + thumbnailSpacing
        val targetScrollX = (frameIndex * frameWidth - width / 2f).coerceIn(minScrollX, maxScrollX)
        
        // Only scroll if we're not already scrolling manually
        if (!isScrolling) {
            scrollX = targetScrollX
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateMaxScroll()
        // Set playhead at center of screen (fixed position)
        playheadX = width / 2f
        // Initialize scrollX so first frame aligns with playhead at center
        scrollX = -(width / 2f)
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

        // Draw fixed playhead at center of screen
        canvas.drawLine(
            playheadX, 
            topMargin, 
            playheadX, 
            topMargin + thumbnailHeight, 
            playheadPaint
        )

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
                // handleTap removed - no more auto snap on touch up
                // This prevents the jerky snap back behavior when releasing finger
            }
        }

        return handled || super.onTouchEvent(event)
    }

    private fun handleTap(x: Float) {
        if (thumbnailPositions.isEmpty()) return
        
        // Calculate exact position based on tap location with smooth interpolation
        val adjustedX = x + scrollX
        val frameWidth = thumbnailWidth + thumbnailSpacing
        val exactFramePosition = adjustedX / frameWidth
        
        // Get the current frame index and interpolation factor
        val currentFrameIndex = exactFramePosition.toInt()
        val interpolationFactor = exactFramePosition - currentFrameIndex
        
        if (currentFrameIndex >= 0 && currentFrameIndex < thumbnailPositions.size) {
            // Calculate the exact time position based on interpolation
            val currentFrameTime = thumbnailPositions[currentFrameIndex]
            val nextFrameIndex = currentFrameIndex + 1
            
            val targetTime = if (nextFrameIndex < thumbnailPositions.size) {
                val nextFrameTime = thumbnailPositions[nextFrameIndex]
                val timeDifference = nextFrameTime - currentFrameTime
                currentFrameTime + (timeDifference * interpolationFactor).toLong()
            } else {
                currentFrameTime
            }
            
            // Update current position and notify listener
            currentPosition = targetTime
            onPositionChangeListener?.invoke(currentPosition)
            
            // Calculate scroll position to center the tapped position at playhead
            val targetScrollX = (adjustedX - width / 2f).coerceIn(minScrollX, maxScrollX)
            
            // Animate scroll to center the tapped position
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

            // Cuộn mượt mà với bounds như editor video chuyên nghiệp
            val adjustedDistanceX = distanceX * 0.6f
            scrollX = (scrollX + adjustedDistanceX).coerceIn(minScrollX, maxScrollX)
            
            // Tính toán và thông báo frame hiện tại tại vị trí playhead
            val currentFramePosition = calculateCurrentPositionFromPlayhead()
            currentPosition = currentFramePosition
            onPositionChangeListener?.invoke(currentPosition)
            
            // Vẽ lại
            postInvalidateOnAnimation()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // Chỉ cuộn mượt với fling, không cập nhật position
            if (abs(velocityX) > 100) {
                startSmoothScroll(-velocityX * 0.2f)
            }

            // Chỉ reset trạng thái scroll
            isScrolling = false
            return true
        }
    }

    private fun startSmoothScroll(distance: Float) {
        scrollAnimator?.cancel()

        val startScrollX = scrollX
        val targetScrollX = (scrollX + distance).coerceIn(minScrollX, maxScrollX)

        if (abs(targetScrollX - startScrollX) < 1f) return

        scrollAnimator = ValueAnimator.ofFloat(startScrollX, targetScrollX).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                scrollX = animator.animatedValue as Float
                // Chỉ vẽ lại, không cập nhật position
                postInvalidateOnAnimation()
            }
            start()
        }
    }
}