package com.example.superphoto.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import kotlinx.coroutines.*
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

    // New variables for optimization
    private var isGeneratingThumbnails = false
    private var currentScaleMatrix = Matrix() // Để scale thumbnails khi transition
    private var zoomAnimator: ValueAnimator? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())

        // Enable touch events
        isClickable = true
        isFocusable = true
    }

    fun setVideoUri(uri: Uri) {
        videoUri = uri
        thumbnailCache.clear()
        extractVideoInfo()
        preloadInitialZoomLevel() // Preload zoom level 1x first
        invalidate()
    }

    fun setVideoDuration(duration: Long) {
        this.videoDuration = duration
        preloadInitialZoomLevel() // Preload zoom level 1x first
        invalidate()
    }

    fun setVideoPath(path: String) {
        videoUri = Uri.parse(path)
        thumbnailCache.clear()
        extractVideoInfo()
        preloadInitialZoomLevel() // Preload zoom level 1x first
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

    private fun preloadInitialZoomLevel() {
        coroutineScope.launch {
            // Load and display zoom level 0 (1x) first
            if (!thumbnailCache.containsKey(0)) {
                withContext(Dispatchers.IO) {
                    generateThumbnailsForLevel(0)
                }
                // Switch to zoom level 0 immediately after generating
                switchToZoomLevel(0)
            }
            // Then preload other zoom levels in the background
            preloadRemainingZoomLevels()
        }
    }

    private fun preloadRemainingZoomLevels() {
        coroutineScope.launch {
            for (level in 1 until zoomLevels.size) {
                if (!thumbnailCache.containsKey(level)) {
                    withContext(Dispatchers.IO) {
                        generateThumbnailsForLevel(level)
                    }
                }
            }
        }
    }

    private fun generateThumbnailsForLevel(level: Int): Pair<MutableList<Bitmap?>, MutableList<Long>> {
        val thumbnailsList = mutableListOf<Bitmap?>()
        val positionsList = mutableListOf<Long>()

        if (videoDuration <= 0) return Pair(thumbnailsList, positionsList)

        val frameInterval = frameIntervals[level]
        var totalFrames = (videoDuration / frameInterval).toInt()
        totalFrames = minOf(totalFrames, 500) // Limit to avoid OOM

        Log.d("VideoTimelineView", "Generating thumbnails for level $level - Frames: $totalFrames")

        videoUri?.let { uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                for (i in 0 until totalFrames) {
                    val timeUs = (i * frameInterval * 1000)
                    if (timeUs <= videoDuration * 1000) {
                        try {
                            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            val scaledBitmap = bitmap?.let { scaleBitmap(it) }
                            thumbnailsList.add(scaledBitmap)
                            positionsList.add(i * frameInterval)
                        } catch (e: Exception) {
                            thumbnailsList.add(null)
                            positionsList.add(i * frameInterval)
                        }
                    }
                }
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thumbnailCache[level] = Pair(thumbnailsList, positionsList)
        return thumbnailCache[level]!!
    }

    private fun scaleBitmap(originalBitmap: Bitmap): Bitmap {
        val originalWidth = originalBitmap.width.toFloat()
        val originalHeight = originalBitmap.height.toFloat()
        val targetWidth = thumbnailWidth.toInt()
        val targetHeight = thumbnailHeight.toInt()
        val scale = minOf(targetWidth / originalWidth, targetHeight / originalHeight)
        return Bitmap.createScaledBitmap(originalBitmap, (originalWidth * scale).toInt(), (originalHeight * scale).toInt(), true)
    }

    private fun generateThumbnails() {
        if (isGeneratingThumbnails) return
        isGeneratingThumbnails = true

        coroutineScope.launch {
            val data = withContext(Dispatchers.IO) {
                generateThumbnailsForLevel(currentZoomLevel)
            }
            thumbnails = data.first
            thumbnailPositions = data.second
            calculateMaxScroll()
            invalidate()
            isGeneratingThumbnails = false
        }
    }

    private fun calculateMaxScroll() {
        val totalWidth = thumbnails.size * (thumbnailWidth + thumbnailSpacing)
        minScrollX = -(width / 2f)
        maxScrollX = totalWidth - (width / 2f)
    }

    private fun calculateCurrentPositionFromPlayhead(): Long {
        if (thumbnailPositions.isEmpty()) return 0L

        val playheadPositionInTimeline = playheadX + scrollX
        val frameWidth = thumbnailWidth + thumbnailSpacing
        val exactFramePosition = playheadPositionInTimeline / frameWidth

        val currentFrameIndex = exactFramePosition.toInt()
        val interpolationFactor = exactFramePosition - currentFrameIndex

        return if (currentFrameIndex >= 0 && currentFrameIndex < thumbnailPositions.size) {
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

        val frameIndex = thumbnailPositions.indexOfFirst { it >= position }
            .takeIf { it != -1 } ?: (thumbnailPositions.size - 1)

        val frameWidth = thumbnailWidth + thumbnailSpacing
        val targetScrollX = (frameIndex * frameWidth - width / 2f).coerceIn(minScrollX, maxScrollX)

        if (!isScrolling) {
            scrollX = targetScrollX
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateMaxScroll()
        playheadX = width / 2f
        scrollX = -(width / 2f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.concat(currentScaleMatrix) // Apply scale matrix to entire timeline

        var x = -scrollX
        val topMargin = 20f
        for (i in thumbnails.indices) {
            val thumbnail = thumbnails[i]
            val rect = RectF(x, topMargin, x + thumbnailWidth, topMargin + thumbnailHeight)

            if (thumbnail != null) {
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

                canvas.drawBitmap(thumbnail, null, drawRect, thumbnailPaint)
            } else {
                canvas.drawRect(rect, borderPaint)
                canvas.drawText("...", x + thumbnailWidth / 2, topMargin + thumbnailHeight / 2, timePaint)
            }

            canvas.drawRect(rect, borderPaint)

            val timePosition = thumbnailPositions.getOrNull(i) ?: 0L
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

        canvas.restore()

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
                // No snap on up
            }
        }

        return handled || super.onTouchEvent(event)
    }

    private fun handleTap(x: Float) {
        if (thumbnailPositions.isEmpty()) return

        val adjustedX = x + scrollX
        val frameWidth = thumbnailWidth + thumbnailSpacing
        val exactFramePosition = adjustedX / frameWidth

        val currentFrameIndex = exactFramePosition.toInt()
        val interpolationFactor = exactFramePosition - currentFrameIndex

        if (currentFrameIndex >= 0 && currentFrameIndex < thumbnailPositions.size) {
            val currentFrameTime = thumbnailPositions[currentFrameIndex]
            val nextFrameIndex = currentFrameIndex + 1

            val targetTime = if (nextFrameIndex < thumbnailPositions.size) {
                val nextFrameTime = thumbnailPositions[nextFrameIndex]
                val timeDifference = nextFrameTime - currentFrameTime
                currentFrameTime + (timeDifference * interpolationFactor).toLong()
            } else {
                currentFrameTime
            }

            currentPosition = targetTime
            onPositionChangeListener?.invoke(currentPosition)

            val targetScrollX = (adjustedX - width / 2f).coerceIn(minScrollX, maxScrollX)

            startSmoothScroll(targetScrollX - scrollX)
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var accumulatedScaleFactor = 1f // Accumulate scale changes
        private val zoomInThreshold = 1.3f // Zoom-in threshold
        private val zoomOutThreshold = 0.7f // Zoom-out threshold
        private val minPinchDistance = 50f // Minimum pinch distance (pixels)

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            accumulatedScaleFactor = 1f // Reset accumulation at gesture start
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            accumulatedScaleFactor *= scaleFactor // Accumulate scale change

            // Calculate pinch distance
            val pinchDistance = abs(detector.currentSpan - detector.previousSpan)

            // Only switch zoom level if accumulated scale is significant and pinch distance is sufficient
            if (pinchDistance < minPinchDistance) {
                return false // Ignore if pinch gesture is too small
            }

            val newZoomLevel = when {
                accumulatedScaleFactor > zoomInThreshold && currentZoomLevel < zoomLevels.size - 1 -> {
                    currentZoomLevel + 1
                }
                accumulatedScaleFactor < zoomOutThreshold && currentZoomLevel > 0 -> {
                    currentZoomLevel - 1
                }
                else -> return false // No zoom level change
            }

            // Reset accumulation and start animation with focusX from pinch
            accumulatedScaleFactor = 1f
            animateZoomTransition(newZoomLevel, scaleFactor > 1f, detector.focusX)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            accumulatedScaleFactor = 1f // Reset at gesture end
        }
    }

    private fun animateZoomTransition(newLevel: Int, isZoomIn: Boolean, pinchFocusX: Float) {
        zoomAnimator?.cancel()

        val startZoom = currentZoom
        val endZoom = zoomLevels[newLevel]
        val startScrollX = scrollX

        // Pivot point is the pinch position in the timeline (adjusted with scrollX)
        val focusX = pinchFocusX + scrollX

        zoomAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                currentZoom = startZoom + (endZoom - startZoom) * progress

                // Calculate scale and adjust matrix with pivot at focusX
                val scale = currentZoom / startZoom
                currentScaleMatrix.setScale(scale, 1f, focusX, 0f)

                // Adjust scrollX to expand outward from focusX, keeping playhead stable
                scrollX = startScrollX * scale + focusX * (1f - scale)
                scrollX = scrollX.coerceIn(minScrollX, maxScrollX)

                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Switch thumbnails only after animation completes
                    switchToZoomLevel(newLevel)
                    // Reset matrix and adjust scroll to maintain focus
                    currentScaleMatrix.reset()
                    scrollX = (focusX * (endZoom / startZoom) - pinchFocusX).coerceIn(minScrollX, maxScrollX)
                    invalidate()
                }
            })
            start()
        }
    }

    private fun switchToZoomLevel(level: Int) {
        currentZoomLevel = level
        currentZoom = zoomLevels[level]
        val cached = thumbnailCache[level]
        if (cached != null) {
            thumbnails = cached.first
            thumbnailPositions = cached.second
        } else {
            generateThumbnails() // Async if not cached
        }
        calculateMaxScroll()
        invalidate()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            scrollAnimator?.cancel()
            isScrolling = true

            val adjustedDistanceX = distanceX * 0.6f
            scrollX = (scrollX + adjustedDistanceX).coerceIn(minScrollX, maxScrollX)

            val currentFramePosition = calculateCurrentPositionFromPlayhead()
            currentPosition = currentFramePosition
            onPositionChangeListener?.invoke(currentPosition)

            postInvalidateOnAnimation()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (abs(velocityX) > 100) {
                startSmoothScroll(-velocityX * 0.2f)
            }

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
                postInvalidateOnAnimation()
            }
            start()
        }
    }
}