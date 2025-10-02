/**
 * Test script to verify Nano Banana face preservation enhancements
 */

import com.example.superphoto.utils.FacePreservationUtils

fun main() {
    println("🧪 Testing Nano Banana Face Preservation Integration")
    println("=" * 60)
    
    // Test 1: Basic prompt enhancement
    println("\n📝 Test 1: Basic Prompt Enhancement")
    val basicPrompt = "Change hairstyle to curly hair"
    val enhancedPrompt = FacePreservationUtils.buildFacePreservationPrompt(
        userPrompt = basicPrompt,
        style = "realistic",
        apiProvider = "gemini"
    )
    
    println("Original: $basicPrompt")
    println("Enhanced: $enhancedPrompt")
    
    // Test 2: Validation system
    println("\n✅ Test 2: Prompt Validation")
    val validation = FacePreservationUtils.validateFacePreservationPrompt(enhancedPrompt)
    println("Validation Score: ${validation.score}")
    println("Is Valid: ${validation.isValid}")
    println("Has Core: ${validation.hasCore}")
    println("Has Critical: ${validation.hasCritical}")
    println("Has Identity: ${validation.hasIdentity}")
    println("Has Quality: ${validation.hasQuality}")
    
    // Test 3: Different styles
    println("\n🎨 Test 3: Style-Specific Enhancements")
    val styles = listOf("realistic", "artistic", "anime", "vintage", "portrait")
    
    styles.forEach { style ->
        val stylePrompt = FacePreservationUtils.buildFacePreservationPrompt(
            userPrompt = "Change outfit to formal suit",
            style = style,
            apiProvider = "gemini"
        )
        println("\n$style Style:")
        println("Length: ${stylePrompt.length} chars")
        println("Contains 'preserve': ${stylePrompt.contains("preserve", ignoreCase = true)}")
        println("Contains 'identity': ${stylePrompt.contains("identity", ignoreCase = true)}")
    }
    
    // Test 4: API-specific optimizations
    println("\n🔧 Test 4: API-Specific Optimizations")
    val apis = listOf("gemini", "pollinations", "huggingface")
    
    apis.forEach { api ->
        val apiPrompt = FacePreservationUtils.buildFacePreservationPrompt(
            userPrompt = "Add sunglasses",
            style = "realistic",
            apiProvider = api
        )
        val validation = FacePreservationUtils.validateFacePreservationPrompt(apiPrompt)
        println("\n$api API:")
        println("Score: ${validation.score}")
        println("Length: ${apiPrompt.length} chars")
    }
    
    // Test 5: Nano Banana patterns verification
    println("\n🍌 Test 5: Nano Banana Patterns Verification")
    val nanoBananaPrompt = FacePreservationUtils.buildFacePreservationPrompt(
        userPrompt = "Change background to beach scene",
        style = "realistic",
        apiProvider = "gemini"
    )
    
    val nanoBananaKeywords = listOf(
        "do not change",
        "preserve",
        "maintain",
        "identity",
        "facial features",
        "same person",
        "critical"
    )
    
    println("Nano Banana Keywords Found:")
    nanoBananaKeywords.forEach { keyword ->
        val found = nanoBananaPrompt.contains(keyword, ignoreCase = true)
        println("  ✓ '$keyword': ${if (found) "✅ FOUND" else "❌ MISSING"}")
    }
    
    println("\n🎯 Final Enhanced Prompt:")
    println(nanoBananaPrompt)
    
    println("\n" + "=" * 60)
    println("✅ Nano Banana Integration Test Complete!")
}