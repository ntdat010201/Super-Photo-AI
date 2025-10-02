package com.example.superphoto.constants

import java.io.Serializable

object TransformationConstants {
    
    enum class Transformation(
        val id: String,
        val displayName: String,
        val description: String
    ) : Serializable {
        BACKGROUND_REMOVAL("bg_removal", "Background Removal", "Remove background from photos"),
        FACE_SWAP("face_swap", "Face Swap", "Swap faces between two photos"),
        AI_ENHANCE("ai_enhance", "AI Enhance", "Enhance photo quality with AI"),
        COLORIZE("colorize", "AI Colorize", "Add colors to black and white photos"),
        OBJECT_REMOVAL("object_removal", "Object Removal", "Remove unwanted objects"),
        STYLE_TRANSFER("style_transfer", "Style Transfer", "Apply artistic styles to photos")
    }
}