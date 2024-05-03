package com.sioptik.main.riwayat_repository

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RiwayatViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RiwayatViewModel::class.java)) {
            val database = RiwayatDatabase.getInstance(context)
            val repository = RiwayatRepository(database.riwayatDao())
            @Suppress("UNCHECKED_CAST")
            return RiwayatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
