package com.example.superphoto.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.superphoto.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

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
    private val thumbnailWidth = 200f  // Reduced for performance
    private val thumbnailHeight = 112f  // Maintain 16:9 aspect ratio
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
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY // Gray placeholder
        style = Paint.Style.FILL
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

    // Placeholder rectangles
    private var placeholderRects = mutableListOf<RectF>()

    // Thumbnail loading control
    private var lastLoadedFrameIndex = 0
    private val initialFrameCount = 20 // Load 20 frames initially
    private val additionalFrameCount = 10 // Load 10 more when scrolling

    // Listener for position changes
    var onPositionChangeListener: ((Long) -> Unit)? = null

    // Optimization variables
    private var isGeneratingThumbnails = false
    private var currentScaleMatrix = Matrix() // For scaling thumbnails during transition
    private var zoomAnimator: ValueAnimator? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Trim mode
    private var isTrimMode = false  // Bật/tắt trim mode
    private var trimStartMs: Long = 0L  // Vị trí bắt đầu trim (ms)
    private var trimEndMs: Long = 0L  // Vị trí kết thúc trim (ms, mặc định toàn bộ video)

    // Drag state
    private var draggingHandle: HandleType? = null  // Đang drag handle nào?
    private enum class HandleType { START, END }
    private val handleWidth = 20f  // Chiều rộng handle (để detect touch)
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.red)  // Màu handle, ví dụ đỏ
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    // Listener cho trim change
    var onTrimChangeListener: ((Long, Long) -> Unit)? = null

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
        this.trimEndMs = duration
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
        scrollToPosition(position)
        invalidate()
        isSettingPositionExternally = false
    }

    fun setPlayheadPosition(position: Long) {
        isSettingPositionExternally = true
        this.currentPosition = position
        scrollToPosition(position)
        invalidate()
        isSettingPositionExternally = false
    }

    private fun extractVideoInfo() {
        videoUri?.let { uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val duration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                videoDuration = duration?.toLongOrNull() ?: 0L
                trimEndMs = videoDuration
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
                videoDuration = 30000L // Default 30 seconds if extraction fails
            }
        }
        // Initialize placeholders based on video duration
        initializePlaceholders()
    }

    private fun initializePlaceholders() {
        placeholderRects.clear()
        if (videoDuration <= 0) return

        val frameInterval = frameIntervals[0] // Use zoom level 0 interval
        val totalFrames = minOf((videoDuration / frameInterval).toInt(), initialFrameCount)
        var x = -scrollX
        val topMargin = 20f

        for (i in 0 until totalFrames) {
            val rect = RectF(x, topMargin, x + thumbnailWidth, topMargin + thumbnailHeight)
            placeholderRects.add(rect)
            x += thumbnailWidth + thumbnailSpacing
        }
    }

    private fun preloadInitialZoomLevel() {
        coroutineScope.launch {
            if (!thumbnailCache.containsKey(0)) {
                withContext(Dispatchers.IO) {
                    generateThumbnailsForLevel(0, initialFrameCount)
                }
                switchToZoomLevel(0)
                placeholderRects.clear() // Clear placeholders after thumbnails are loaded
            }
            preloadRemainingZoomLevels()
        }
    }

    private fun preloadRemainingZoomLevels() {
        coroutineScope.launch {
            for (level in 1 until zoomLevels.size) {
                if (!thumbnailCache.containsKey(level)) {
                    withContext(Dispatchers.IO) {
                        generateThumbnailsForLevel(level, initialFrameCount)
                    }
                }
            }
        }
    }

    private fun generateThumbnailsForLevel(
        level: Int,
        frameCount: Int
    ): Pair<MutableList<Bitmap?>, MutableList<Long>> {
        val thumbnailsList = mutableListOf<Bitmap?>()
        val positionsList = mutableListOf<Long>()

        if (videoDuration <= 0) return Pair(thumbnailsList, positionsList)

        val frameInterval = frameIntervals[level]
        val totalFrames = minOf((videoDuration / frameInterval).toInt(), frameCount)

        Log.d("VideoTimelineView", "Generating thumbnails for level $level - Frames: $totalFrames")

        videoUri?.let { uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                for (i in 0 until totalFrames) {
                    val timeUs = (i * frameInterval * 1000)
                    if (timeUs <= videoDuration * 1000) {
                        try {
                            val bitmap = retriever.getFrameAtTime(
                                timeUs,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            )
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
        lastLoadedFrameIndex = totalFrames
        return thumbnailCache[level]!!
    }

    private fun loadMoreThumbnails() {
        if (isGeneratingThumbnails) return
        isGeneratingThumbnails = true

        coroutineScope.launch {
            val startIndex = lastLoadedFrameIndex
            val data = withContext(Dispatchers.IO) {
                generateAdditionalThumbnails(currentZoomLevel, startIndex, additionalFrameCount)
            }
            thumbnails.addAll(data.first)
            thumbnailPositions.addAll(data.second)
            calculateMaxScroll()
            invalidate()
            isGeneratingThumbnails = false
        }
    }

    private fun generateAdditionalThumbnails(
        level: Int,
        startIndex: Int,
        frameCount: Int
    ): Pair<MutableList<Bitmap?>, MutableList<Long>> {
        val thumbnailsList = mutableListOf<Bitmap?>()
        val positionsList = mutableListOf<Long>()

        if (videoDuration <= 0) return Pair(thumbnailsList, positionsList)

        val frameInterval = frameIntervals[level]
        val totalFrames = minOf((videoDuration / frameInterval).toInt() - startIndex, frameCount)

        if (totalFrames <= 0) return Pair(thumbnailsList, positionsList)

        Log.d(
            "VideoTimelineView",
            "Generating additional thumbnails for level $level - Frames: $totalFrames"
        )

        videoUri?.let { uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                for (i in startIndex until startIndex + totalFrames) {
                    val timeUs = (i * frameInterval * 1000)
                    if (timeUs <= videoDuration * 1000) {
                        try {
                            val bitmap = retriever.getFrameAtTime(
                                timeUs,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            )
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
        lastLoadedFrameIndex += totalFrames
        thumbnailCache[level] = Pair(thumbnails, thumbnailPositions)
        return Pair(thumbnailsList, positionsList)
    }

    private fun scaleBitmap(originalBitmap: Bitmap): Bitmap {
        val originalWidth = originalBitmap.width.toFloat()
        val originalHeight = originalBitmap.height.toFloat()
        val targetWidth = thumbnailWidth.toInt()
        val targetHeight = thumbnailHeight.toInt()
        val scale = minOf(targetWidth / originalWidth, targetHeight / originalHeight)
        return Bitmap.createScaledBitmap(
            originalBitmap,
            (originalWidth * scale).toInt(),
            (originalHeight * scale).toInt(),
            true
        )
    }

    private fun generateThumbnails() {
        if (isGeneratingThumbnails) return
        isGeneratingThumbnails = true

        coroutineScope.launch {
            val data = withContext(Dispatchers.IO) {
                generateThumbnailsForLevel(currentZoomLevel, initialFrameCount)
            }
            thumbnails = data.first
            thumbnailPositions = data.second
            calculateMaxScroll()
            placeholderRects.clear() // Clear placeholders after thumbnails are loaded
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
        initializePlaceholders() // Reinitialize placeholders on size change
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.concat(currentScaleMatrix)

        var x = -scrollX
        val topMargin = 20f

        // Draw placeholders if thumbnails are not yet loaded
        if (thumbnails.isEmpty()) {
            for (rect in placeholderRects) {
                canvas.drawRect(rect, placeholderPaint)
                canvas.drawRect(rect, borderPaint)
            }
        }

        // Draw thumbnails
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
                canvas.drawRect(rect, placeholderPaint)
                canvas.drawText(
                    "...",
                    x + thumbnailWidth / 2,
                    topMargin + thumbnailHeight / 2,
                    timePaint
                )
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

        // Check if more thumbnails need to be loaded
        if (thumbnails.isNotEmpty() && scrollX > maxScrollX - width * 2) {
            loadMoreThumbnails()
        }

        if (isTrimMode) {
            canvas.save()
            canvas.concat(currentScaleMatrix)

            // Tính vị trí X của handles
            val pixelsPerMs = (thumbnailWidth + thumbnailSpacing) / frameIntervals[currentZoomLevel]
            val startX = -scrollX + (trimStartMs * pixelsPerMs)
            val endX = -scrollX + (trimEndMs * pixelsPerMs)

            // Vẽ handle start (left)
            canvas.drawLine(startX, topMargin - 20f, startX, topMargin + thumbnailHeight + 20f, handlePaint)

            // Vẽ handle end (right)
            canvas.drawLine(endX, topMargin - 20f, endX, topMargin + thumbnailHeight + 20f, handlePaint)

            // Optional: Vẽ overlay mờ giữa start và end để highlight đoạn chọn
            val overlayPaint = Paint().apply { color = Color.argb(50, 0, 255, 0); style = Paint.Style.FILL }  // Xanh mờ
            canvas.drawRect(startX, topMargin, endX, topMargin + thumbnailHeight, overlayPaint)

            canvas.restore()
        }
    }

    private fun formatTime(milliseconds: Long, index: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isTrimMode) {
            // Giữ nguyên code cũ nếu không ở trim mode
            var handled = scaleDetector.onTouchEvent(event)
            handled = gestureDetector.onTouchEvent(event) || handled
            return handled || super.onTouchEvent(event)
        }

        // Trim mode: Xử lý drag handles và cho phép scrolling
        val pixelsPerMs = (thumbnailWidth + thumbnailSpacing) / frameIntervals[currentZoomLevel]
        val startX = -scrollX + (trimStartMs * pixelsPerMs)
        val endX = -scrollX + (trimEndMs * pixelsPerMs)

        var trimHandled = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchX = event.x
                if (abs(touchX - startX) < handleWidth) {
                    draggingHandle = HandleType.START
                    trimHandled = true
                } else if (abs(touchX - endX) < handleWidth) {
                    draggingHandle = HandleType.END
                    trimHandled = true
                }
                // Không gọi handleTap trong trim mode để tránh click-to-seek
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingHandle != null) {
                    val newX = event.x + scrollX  // Tính vị trí thực trên timeline
                    var newMs = (newX / pixelsPerMs).toLong().coerceIn(0L, videoDuration)

                    if (draggingHandle == HandleType.START) {
                        trimStartMs = newMs.coerceAtMost(trimEndMs - 1000L)  // Ít nhất 1s cách end
                    } else {
                        trimEndMs = newMs.coerceAtLeast(trimStartMs + 1000L)  // Ít nhất 1s cách start
                    }

                    invalidate()
                    onTrimChangeListener?.invoke(trimStartMs, trimEndMs)
                    trimHandled = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (draggingHandle != null) {
                    draggingHandle = null
                    trimHandled = true
                }
            }
        }

        // Nếu không phải drag handle, cho phép gesture detector xử lý scrolling
        if (!trimHandled) {
            var handled = scaleDetector.onTouchEvent(event)
            handled = gestureDetector.onTouchEvent(event) || handled
            return handled || super.onTouchEvent(event)
        }

        return true  // Consume event nếu đã xử lý trim handle
    }

    private fun handleTap(x: Float) {
        if (thumbnailPositions.isEmpty()) return
        
        // Nếu đang ở trim mode, không cho phép click để seek
        if (isTrimMode) {
            return
        }

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

    fun enableTrimMode(enable: Boolean) {
        isTrimMode = enable
        if (enable) {
            trimStartMs = 0L
            trimEndMs = videoDuration
        }
        invalidate()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var accumulatedScaleFactor = 1f
        private val zoomInThreshold = 1.3f
        private val zoomOutThreshold = 0.7f
        private val minPinchDistance = 50f

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            accumulatedScaleFactor = 1f
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            accumulatedScaleFactor *= scaleFactor

            val pinchDistance = abs(detector.currentSpan - detector.previousSpan)

            if (pinchDistance < minPinchDistance) {
                return false
            }

            val newZoomLevel = when {
                accumulatedScaleFactor > zoomInThreshold && currentZoomLevel < zoomLevels.size - 1 -> {
                    currentZoomLevel + 1
                }

                accumulatedScaleFactor < zoomOutThreshold && currentZoomLevel > 0 -> {
                    currentZoomLevel - 1
                }

                else -> return false
            }

            accumulatedScaleFactor = 1f
            animateZoomTransition(newZoomLevel, scaleFactor > 1f, detector.focusX)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            accumulatedScaleFactor = 1f
        }
    }

    private fun animateZoomTransition(newLevel: Int, isZoomIn: Boolean, pinchFocusX: Float) {
        zoomAnimator?.cancel()

        val startZoom = currentZoom
        val endZoom = zoomLevels[newLevel]
        val startScrollX = scrollX

        val focusX = pinchFocusX + scrollX

        zoomAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                currentZoom = startZoom + (endZoom - startZoom) * progress

                val scale = currentZoom / startZoom
                currentScaleMatrix.setScale(scale, 1f, focusX, 0f)

                scrollX = startScrollX * scale + focusX * (1f - scale)
                scrollX = scrollX.coerceIn(minScrollX, maxScrollX)

                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    switchToZoomLevel(newLevel)
                    currentScaleMatrix.reset()
                    scrollX = (focusX * (endZoom / startZoom) - pinchFocusX).coerceIn(
                        minScrollX,
                        maxScrollX
                    )
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
            generateThumbnails()
        }
        calculateMaxScroll()
        initializePlaceholders() // Reinitialize placeholders for new zoom level
        if (isTrimMode) enableTrimMode(true)
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

    // Getter methods for trim range
    fun getTrimStartMs(): Long = trimStartMs
    fun getTrimEndMs(): Long = trimEndMs
}
