package com.example.superphoto.splash

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.superphoto.databinding.FragmentOnBoardingThreeBinding
import com.example.superphoto.ui.activities.MainActivity

class OnBoardingThreeFragment : Fragment() {
    private lateinit var binding: FragmentOnBoardingThreeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnBoardingThreeBinding.inflate(layoutInflater)
        initListener()
        return binding.root
    }

    private fun initListener() {

    }

}
