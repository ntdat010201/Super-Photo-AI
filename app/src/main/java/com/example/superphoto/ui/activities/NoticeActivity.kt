package com.example.superphoto.ui.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.superphoto.R

class NoticeActivity : AppCompatActivity() {
    
    private lateinit var btnClose: ImageView
    private lateinit var cbDontShowAgain: CheckBox
    private lateinit var btnNext: Button
    private lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        const val PREF_NAME = "notice_preferences"
        const val KEY_DONT_SHOW_AGAIN = "dont_show_again"
        
        fun shouldShowNotice(context: android.content.Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            return !prefs.getBoolean(KEY_DONT_SHOW_AGAIN, false)
        }
        
        fun resetNoticePreference(context: android.content.Context) {
            val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_DONT_SHOW_AGAIN, false)
                .apply()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notice)
        
        initViews()
        initSharedPreferences()
        setupClickListeners()
    }
    
    private fun initViews() {
        btnClose = findViewById(R.id.btnClose)
        cbDontShowAgain = findViewById(R.id.cbDontShowAgain)
        btnNext = findViewById(R.id.btnNext)
    }
    
    private fun initSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
    }
    
    private fun setupClickListeners() {
        // Close button click
        btnClose.setOnClickListener {
            handleClose()
        }
        
        // Next button click
        btnNext.setOnClickListener {
            handleNext()
        }
        
        // Don't show again checkbox
        cbDontShowAgain.setOnCheckedChangeListener { _, isChecked ->
            // Save preference immediately when checkbox is changed
            sharedPreferences.edit()
                .putBoolean(KEY_DONT_SHOW_AGAIN, isChecked)
                .apply()
        }
    }
    
    private fun handleClose() {
        // Save preference if checkbox is checked
        if (cbDontShowAgain.isChecked) {
            sharedPreferences.edit()
                .putBoolean(KEY_DONT_SHOW_AGAIN, true)
                .apply()
        }
        
        // Close activity
        finish()
    }
    
    private fun handleNext() {
        // Save preference if checkbox is checked
        if (cbDontShowAgain.isChecked) {
            sharedPreferences.edit()
                .putBoolean(KEY_DONT_SHOW_AGAIN, true)
                .apply()
        }
        
        // Set result to indicate user proceeded
        setResult(RESULT_OK)
        finish()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        handleClose()
    }
    

}