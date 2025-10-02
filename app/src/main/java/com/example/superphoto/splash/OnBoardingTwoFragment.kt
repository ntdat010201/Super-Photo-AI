package com.example.superphoto.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.superphoto.databinding.FragmentOnBoardingTwoBinding

class OnBoardingTwoFragment : Fragment() {
    private lateinit var binding: FragmentOnBoardingTwoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnBoardingTwoBinding.inflate(layoutInflater)
        initListener()
        return binding.root
    }

    private fun initListener() {

    }


}