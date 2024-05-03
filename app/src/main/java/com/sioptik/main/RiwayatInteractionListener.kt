package com.sioptik.main

import com.sioptik.main.riwayat_repository.RiwayatEntity

interface RiwayatInteractionListener {
    fun onDeleteRiwayat(riwayat: RiwayatEntity)

    fun onClickLihatDetail(riwayat: RiwayatEntity)

    fun onClickLihatGambar(riwayat: RiwayatEntity)

}
