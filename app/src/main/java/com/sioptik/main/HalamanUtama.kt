package com.sioptik.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sioptik.main.databinding.FragmentHalamanUtamaBinding

class HalamanUtama : Fragment() {
    private var _binding : FragmentHalamanUtamaBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHalamanUtamaBinding.inflate(inflater, container, false)
        binding.scanButton.setOnClickListener {
            val cameraIntent = Intent(activity, Kamera::class.java)
            startActivity(cameraIntent)
        }
        binding.historyButton.setOnClickListener {
            val historyIntent = Intent(activity, Riwayat::class.java)
            startActivity(historyIntent)
        }

        return binding.root
    }

}