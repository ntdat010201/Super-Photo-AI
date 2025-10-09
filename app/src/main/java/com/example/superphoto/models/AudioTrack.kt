package com.example.superphoto.models

import android.net.Uri

/**
 * Model class representing an audio track in the timeline
 */
data class AudioTrack(
    val id: String,
    val uri: Uri,
    val filePath: String,
    val fileName: String,
    val duration: Long, // Duration in milliseconds
    val startPosition: Long, // Start position in timeline (milliseconds)
    val endPosition: Long, // End position in timeline (milliseconds)
    val volume: Float = 1.0f, // Volume level (0.0 to 1.0)
    val isEnabled: Boolean = true
) {
    /**
     * Get the display name for the audio track
     */
    fun getDisplayName(): String {
        return fileName.substringBeforeLast(".")
    }
    
    /**
     * Get formatted duration string
     */
    fun getFormattedDuration(): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    /**
     * Check if the audio track overlaps with given time range
     */
    fun overlaps(startMs: Long, endMs: Long): Boolean {
        return !(endPosition <= startMs || startPosition >= endMs)
    }
    
    /**
     * Get the visible portion of the track within given time range
     */
    fun getVisiblePortion(viewStartMs: Long, viewEndMs: Long): Pair<Long, Long> {
        val visibleStart = maxOf(startPosition, viewStartMs)
        val visibleEnd = minOf(endPosition, viewEndMs)
        return Pair(visibleStart, visibleEnd)
    }
}