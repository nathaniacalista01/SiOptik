package com.sioptik.main.riwayat_repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.launch

class RiwayatViewModel(private val riwayatRepository: RiwayatRepository): ViewModel() {
    fun getAllRiwayat() = riwayatRepository.getAll().asLiveData()

    fun insertRiwayat(riwayat: RiwayatEntity) = viewModelScope.launch {
        riwayatRepository.insert(riwayat)
    }

    fun deleteRiwayat(riwayat: RiwayatEntity) = viewModelScope.launch {
        riwayatRepository.delete(riwayat)
    }
}