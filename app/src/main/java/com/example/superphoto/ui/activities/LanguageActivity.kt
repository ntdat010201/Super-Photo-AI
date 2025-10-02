package com.example.superphoto.ui.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.superphoto.R
import com.example.superphoto.data.model.Language
import com.example.superphoto.ui.adapters.LanguageAdapter

class LanguageActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var languageAdapter: LanguageAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_language)
        
        setupRecyclerView()
        setupClickListeners()
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rcv_language)
        
        val languages = getLanguageList()
        
        languageAdapter = LanguageAdapter(languages) { selectedLanguage ->
            // Handle language selection
            // You can save to SharedPreferences or send result back
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LanguageActivity)
            adapter = languageAdapter
        }
    }
    
    private fun setupClickListeners() {
        findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.back)?.setOnClickListener {
            finish()
        }
        
        findViewById<android.widget.TextView>(R.id.tv_save)?.setOnClickListener {
            // Save selected language and finish
            val selectedLanguage = languageAdapter.getSelectedLanguage()
            // TODO: Save to SharedPreferences or return result
            finish()
        }
    }
    
    private fun getLanguageList(): List<Language> {
        return listOf(
            Language("English (US)", "en_US", R.drawable.img_flag_english, true),
            Language("Spanish (Español)", "es_ES", R.drawable.img_flag_spanish),
            Language("French (Français)", "fr_FR", R.drawable.img_flag_france),
            Language("German (Deutsch)", "de_DE", R.drawable.img_flag_germany),
            Language("Portuguese (Português)", "pt_PT", R.drawable.img_flag_portuguese),
            Language("Japanese (日本語)", "ja_JP", R.drawable.img_flag_nhat_ban),
            Language("Korean (한국어)", "ko_KR", R.drawable.img_flag_han_quoc),
            Language("Vietnamese (Tiếng Việt)", "vi_VN", R.drawable.img_flag_vietnam),
            Language("Finnish (Suomi)", "fi_FI", R.drawable.img_flag_finland),
            Language("Canadian English", "en_CA", R.drawable.img_flag_canada)
        )
    }
}