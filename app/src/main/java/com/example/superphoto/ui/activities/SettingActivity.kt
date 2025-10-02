package com.example.superphoto.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.superphoto.R

class SettingActivity : AppCompatActivity() {
    
    private lateinit var backButton: ImageView
    private lateinit var languageItem: ConstraintLayout
    private lateinit var rateAppItem: ConstraintLayout
    private lateinit var shareAppItem: ConstraintLayout
    private lateinit var contactUsItem: ConstraintLayout
    private lateinit var privacyPolicyItem: ConstraintLayout
    private lateinit var termsOfServiceItem: ConstraintLayout
    

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)
        
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        languageItem = findViewById(R.id.languageItem)
        rateAppItem = findViewById(R.id.rateAppItem)
        shareAppItem = findViewById(R.id.shareAppItem)
        contactUsItem = findViewById(R.id.contactUsItem)
        privacyPolicyItem = findViewById(R.id.privacyPolicyItem)
        termsOfServiceItem = findViewById(R.id.termsOfServiceItem)

    }
    
    private fun setupClickListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }
        
        // Language setting
        languageItem.setOnClickListener {
            val intent = Intent(this, LanguageActivity::class.java)
            startActivity(intent)
        }
        
        // Rate app
        rateAppItem.setOnClickListener {
            openPlayStore()
        }
        
        // Share app
        shareAppItem.setOnClickListener {
            shareApp()
        }
        
        // Contact us
        contactUsItem.setOnClickListener {
            openEmailClient()
        }
        
        // Privacy policy
        privacyPolicyItem.setOnClickListener {
            openPrivacyPolicy()
        }
        
        // Terms of service
        termsOfServiceItem.setOnClickListener {
            openTermsOfService()
        }

    }
    
    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            startActivity(intent)
        }
    }
    
    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out SuperPhoto App!")
            putExtra(Intent.EXTRA_TEXT, "Download SuperPhoto - the best photo editing app! https://play.google.com/store/apps/details?id=$packageName")
        }
        startActivity(Intent.createChooser(shareIntent, "Share SuperPhoto"))
    }
    
    private fun openEmailClient() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@superphoto.com")
            putExtra(Intent.EXTRA_SUBJECT, "SuperPhoto App Support")
            putExtra(Intent.EXTRA_TEXT, "Hi SuperPhoto Team,\n\nI need help with...")
        }
        
        try {
            startActivity(emailIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://superphoto.com/privacy-policy"))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open privacy policy", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openTermsOfService() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://superphoto.com/terms-of-service"))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open terms of service", Toast.LENGTH_SHORT).show()
        }
    }

}