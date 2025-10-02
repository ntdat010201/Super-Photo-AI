package com.example.superphoto.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.superphoto.R
import com.example.superphoto.adapter.IntroPagerAdapter
import com.example.superphoto.base.BaseActivity
import com.example.superphoto.databinding.ActivityOnBoardingBinding
import com.example.superphoto.ui.activities.MainActivity

class OnBoardingActivity : BaseActivity() {
    private lateinit var binding: ActivityOnBoardingBinding
    private val numPages = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
        initListener()
    }

    private fun initData() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setupDots(position)

                val fadeIn = AnimationUtils.loadAnimation(this@OnBoardingActivity, R.anim.fade_in)

                when (position) {
                    0 -> {
                        binding.nextButton.startAnimation(fadeIn)
                        binding.skipButton.startAnimation(fadeIn)
                    }

                    1 -> {
                        binding.nextButton.startAnimation(fadeIn)
                        binding.skipButton.startAnimation(fadeIn)

                    }

                    2 -> {
                        binding.nextButton.startAnimation(fadeIn)
                        binding.skipButton.startAnimation(fadeIn)

                    }

                    else -> {
                        binding.nextButton.clearAnimation()
                        binding.skipButton.clearAnimation()
                        binding.nextButton.visibility = View.VISIBLE
                        binding.skipButton.visibility = View.VISIBLE
                    }
                }

            }
        })
    }

    private fun initView() {
        val adapter = IntroPagerAdapter(this)
        binding.viewPager.adapter = adapter

        binding.viewPager.post {
            setupDots(binding.viewPager.currentItem)
        }
    }

    private fun initListener() {
        binding.nextButton.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < numPages - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                getSharedPreferences("settings", MODE_PRIVATE)
                    .edit()
                    .putBoolean("onboarding_done", true)
                    .apply()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        binding.skipButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupDots(position: Int) {
        binding.dotLayout.removeAllViews()
        for (i in 0 until numPages) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(
                if (i == position) 48 else 16,
                16
            )
            params.setMargins(8, 0, 8, 0)
            dot.layoutParams = params

            dot.background = ContextCompat.getDrawable(
                this,
                if (i == position) R.drawable.dot_selected else R.drawable.dot_unselected
            )

            binding.dotLayout.addView(dot)
        }
    }


}
