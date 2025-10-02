package com.example.superphoto.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.superphoto.splash.OnBoardingOneFragment
import com.example.superphoto.splash.OnBoardingThreeFragment
import com.example.superphoto.splash.OnBoardingTwoFragment


class IntroPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnBoardingOneFragment()
            1 -> OnBoardingTwoFragment()
            2 -> OnBoardingThreeFragment()
            else -> OnBoardingOneFragment()
        }
    }
}