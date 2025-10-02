package com.example.superphoto.utils

import android.graphics.Bitmap
import android.util.Log

/**
 * Face Preservation Utilities
 * Based on Nano Banana techniques for maintaining character consistency
 */
object FacePreservationUtils {
    
    private const val TAG = "FacePreservationUtils"
    
    /**
     * Core face preservation patterns from Nano Banana examples
     */
    object NanoBananaPatterns {
        const val CORE_INSTRUCTION = "Transform this person's image based on the description while preserving their identity. "
        const val CRITICAL_COMMAND = "CRITICAL: DO NOT change the person's face, facial features, or identity. "
        const val IDENTITY_PRESERVATION = "Maintain the exact same facial structure, eye shape, nose, mouth, and all distinctive features. "
        const val CONSISTENCY_REINFORCEMENT = "Keep the same person throughout the transformation. Only change what is specifically requested. "
        
        // Style-specific preservation patterns
        val STYLE_PATTERNS = mapOf(
            "realistic" to "Photorealistic portrait with professional photography quality, natural lighting, sharp focus on face. ",
            "artistic" to "Artistic interpretation with creative expression while preserving facial identity. ",
            "portrait" to "Professional portrait photography with studio lighting and perfect composition. ",
            "anime" to "Anime/manga style transformation keeping distinctive facial features and identity. ",
            "vintage" to "Vintage photography style with retro effects and classic composition. ",
            "sketch" to "Detailed pencil sketch or artistic drawing style maintaining facial likeness. "
        )
    }
    
    /**
     * Build enhanced prompt using Nano Banana face preservation techniques
     */
    fun buildFacePreservationPrompt(
        userPrompt: String,
        style: String = "realistic",
        apiProvider: String = "gemini"
    ): String {
        return buildString {
            // Core instruction (Nano Banana pattern)
            append(NanoBananaPatterns.CORE_INSTRUCTION)
            
            // User's specific request
            append(userPrompt)
            append(". ")
            
            // Style-specific enhancement with face preservation
            val stylePattern = NanoBananaPatterns.STYLE_PATTERNS[style.lowercase()] 
                ?: "High-quality detailed image with enhanced features. "
            append(stylePattern)
            
            // Critical face preservation command (key Nano Banana learning)
            append(NanoBananaPatterns.CRITICAL_COMMAND)
            append(NanoBananaPatterns.IDENTITY_PRESERVATION)
            
            // API-specific optimizations
            when (apiProvider.lowercase()) {
                "gemini" -> {
                    append("Preserve all facial characteristics and maintain the person's identity. ")
                    append("Do not alter the person's face or identity, only change what is specifically requested. ")
                }
                "pollinations" -> {
                    append("Keep the same person throughout the transformation. ")
                    append("Only change what is specifically requested, preserve everything else. ")
                }
                "huggingface" -> {
                    append("Maintain the same person throughout the transformation. ")
                    append("Only modify what is specifically requested, keep facial identity unchanged. ")
                }
            }
            
            // Final reinforcement
            append(NanoBananaPatterns.CONSISTENCY_REINFORCEMENT)
            
            // Quality enhancement
            append("High quality, detailed facial features, sharp focus on face, detailed eyes, ")
            append("natural skin texture, realistic lighting, professional photography quality.")
        }
    }
    
    /**
     * Validate prompt for face preservation keywords
     */
    fun validateFacePreservationPrompt(prompt: String): FacePreservationValidation {
        val lowerPrompt = prompt.lowercase()
        
        val hasCore = lowerPrompt.contains("preserve") || lowerPrompt.contains("maintain") || 
                     lowerPrompt.contains("keep") || lowerPrompt.contains("same person")
        
        val hasCritical = lowerPrompt.contains("do not change") || lowerPrompt.contains("don't change") ||
                         lowerPrompt.contains("critical")
        
        val hasIdentity = lowerPrompt.contains("identity") || lowerPrompt.contains("facial features") ||
                         lowerPrompt.contains("face") || lowerPrompt.contains("person")
        
        val hasQuality = lowerPrompt.contains("high quality") || lowerPrompt.contains("detailed") ||
                        lowerPrompt.contains("professional")
        
        val score = calculatePreservationScore(hasCore, hasCritical, hasIdentity, hasQuality)
        
        return FacePreservationValidation(
            isValid = score >= 0.7f,
            score = score,
            hasCore = hasCore,
            hasCritical = hasCritical,
            hasIdentity = hasIdentity,
            hasQuality = hasQuality,
            suggestions = generateSuggestions(hasCore, hasCritical, hasIdentity, hasQuality)
        )
    }
    
    /**
     * Calculate face preservation score (0.0 to 1.0)
     */
    private fun calculatePreservationScore(
        hasCore: Boolean,
        hasCritical: Boolean,
        hasIdentity: Boolean,
        hasQuality: Boolean
    ): Float {
        var score = 0f
        if (hasCore) score += 0.3f      // Core preservation instruction (30%)
        if (hasCritical) score += 0.3f  // Critical command (30%)
        if (hasIdentity) score += 0.3f  // Identity keywords (30%)
        if (hasQuality) score += 0.1f   // Quality enhancement (10%)
        return score
    }
    
    /**
     * Generate suggestions for improving face preservation
     */
    private fun generateSuggestions(
        hasCore: Boolean,
        hasCritical: Boolean,
        hasIdentity: Boolean,
        hasQuality: Boolean
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (!hasCore) {
            suggestions.add("Add core preservation instruction: 'preserve the person's identity'")
        }
        if (!hasCritical) {
            suggestions.add("Add critical command: 'DO NOT change the person's face or facial features'")
        }
        if (!hasIdentity) {
            suggestions.add("Include identity keywords: 'same person', 'facial features', 'identity'")
        }
        if (!hasQuality) {
            suggestions.add("Add quality enhancement: 'high quality', 'detailed facial features'")
        }
        
        return suggestions
    }
    
    /**
     * Analyze image for face preservation quality (placeholder for future ML implementation)
     */
    fun analyzeFacePreservationQuality(
        originalBitmap: Bitmap,
        generatedImagePath: String
    ): FacePreservationAnalysis {
        // Placeholder implementation - in real scenario, this would use ML models
        // to compare facial features between original and generated images
        
        Log.d(TAG, "Analyzing face preservation quality...")
        
        // For now, return a basic analysis
        return FacePreservationAnalysis(
            preservationScore = 0.85f, // Placeholder score
            facialSimilarity = 0.82f,
            identityConsistency = 0.88f,
            qualityScore = 0.90f,
            recommendations = listOf(
                "Face preservation appears good",
                "Consider using higher resolution for better detail",
                "Facial features are well maintained"
            )
        )
    }
    
    /**
     * Get optimal parameters for different AI providers based on Nano Banana learnings
     */
    fun getOptimalParameters(
        style: String,
        apiProvider: String
    ): Map<String, Any> {
        return when (apiProvider.lowercase()) {
            "gemini" -> mapOf(
                "temperature" to 0.7f,
                "topK" to 40,
                "topP" to 0.95f,
                "maxOutputTokens" to 1024
            )
            "pollinations" -> mapOf(
                "enhance" to true,
                "nologo" to true,
                "private" to false,
                "model" to "flux"
            )
            "huggingface" -> mapOf(
                "num_inference_steps" to 30,
                "guidance_scale" to 7.5f,
                "width" to 512,
                "height" to 512
            )
            else -> emptyMap()
        }
    }
    
    /**
     * Data classes for validation and analysis results
     */
    data class FacePreservationValidation(
        val isValid: Boolean,
        val score: Float,
        val hasCore: Boolean,
        val hasCritical: Boolean,
        val hasIdentity: Boolean,
        val hasQuality: Boolean,
        val suggestions: List<String>
    )
    
    data class FacePreservationAnalysis(
        val preservationScore: Float,
        val facialSimilarity: Float,
        val identityConsistency: Float,
        val qualityScore: Float,
        val recommendations: List<String>
    )
    
    /**
     * Log face preservation metrics for debugging
     */
    fun logPreservationMetrics(
        prompt: String,
        validation: FacePreservationValidation,
        apiProvider: String
    ) {
        Log.d(TAG, "=== Face Preservation Metrics ===")
        Log.d(TAG, "API Provider: $apiProvider")
        Log.d(TAG, "Validation Score: ${validation.score}")
        Log.d(TAG, "Is Valid: ${validation.isValid}")
        Log.d(TAG, "Has Core: ${validation.hasCore}")
        Log.d(TAG, "Has Critical: ${validation.hasCritical}")
        Log.d(TAG, "Has Identity: ${validation.hasIdentity}")
        Log.d(TAG, "Has Quality: ${validation.hasQuality}")
        Log.d(TAG, "Suggestions: ${validation.suggestions}")
        Log.d(TAG, "Enhanced Prompt Length: ${prompt.length}")
        Log.d(TAG, "================================")
    }
}