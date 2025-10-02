package com.example.superphoto.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException

object ImageUtils {
    private const val TAG = "ImageUtils"
    
    /**
     * Convert Bitmap to Base64 string for API calls
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    /**
     * Convert Base64 string to Bitmap
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting base64 to bitmap: ${e.message}")
            null
        }
    }
    
    /**
     * Load bitmap from URI
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: IOException) {
            Log.e(TAG, "Error loading bitmap from URI: ${e.message}")
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error loading bitmap from URI: ${e.message}")
            null
        }
    }
    
    /**
     * Resize bitmap to maximum dimensions while maintaining aspect ratio
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val aspectRatio = width.toFloat() / height.toFloat()
        
        val (newWidth, newHeight) = if (aspectRatio > 1) {
            // Landscape
            val newW = minOf(maxWidth, width)
            val newH = (newW / aspectRatio).toInt()
            newW to newH
        } else {
            // Portrait or square
            val newH = minOf(maxHeight, height)
            val newW = (newH * aspectRatio).toInt()
            newW to newH
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Create a sample bitmap for testing
     */
    fun createSampleBitmap(): Bitmap {
        return Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply {
            // Fill with a gradient or solid color for testing
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLUE
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }
    
    /**
     * Get image file size in bytes
     */
    fun getImageSize(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 90): Int {
        val stream = ByteArrayOutputStream()
        bitmap.compress(format, quality, stream)
        return stream.size()
    }
    
    /**
     * Check if image dimensions are valid for AI processing
     */
    fun isValidImageSize(bitmap: Bitmap, minSize: Int = 300, maxSize: Int = 4096): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        return width >= minSize && height >= minSize && width <= maxSize && height <= maxSize
    }
}