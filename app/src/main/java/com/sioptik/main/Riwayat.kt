package com.sioptik.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sioptik.main.riwayat_repository.RiwayatEntity
import com.sioptik.main.riwayat_repository.RiwayatViewModel
import com.sioptik.main.riwayat_repository.RiwayatViewModelFactory
import java.util.Date

class Riwayat : AppCompatActivity(), RiwayatInteractionListener {
    private val riwayatViewModel: RiwayatViewModel by viewModels {
        RiwayatViewModelFactory(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat)

        val riwayatRecyclerViewAdapter = RiwayatRecyclerViewAdapter(emptyList(), this)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this);
        recyclerView.adapter = riwayatRecyclerViewAdapter

        riwayatViewModel.getAllRiwayat().observe(this) {
            riwayatRecyclerViewAdapter.updateData(it)
        }

        riwayatViewModel.insertRiwayat(
            RiwayatEntity(
            1,
            101,
            Date(),
            "test",
            "test",
            "test"
        )
        )
    }

    override fun onDeleteRiwayat(riwayat: RiwayatEntity) {
        try {
            val jsonFile = Uri.parse(riwayat.jsonFileUri).toFile()
            if (jsonFile.exists()) {
                jsonFile.delete()
            }
            val originalImageFile = Uri.parse(riwayat.originalImageUri).toFile()
            if (originalImageFile.exists()) {
                originalImageFile.delete()
            }
            val annotatedImageFile = Uri.parse(riwayat.annotatedImageUri).toFile()
            if (annotatedImageFile.exists()) {
                annotatedImageFile.delete()
            }
        } catch (e: IllegalArgumentException) {
            Log.d("Riwayat", "Illegal Argument Exception");
        } finally {
            riwayatViewModel.deleteRiwayat(riwayat)
        }
    }

    override fun onClickLihatDetail(riwayat: RiwayatEntity) {
        val intent = Intent(this, DetailRiwayat::class.java)
        intent.putExtra("jsonFileUri", riwayat.jsonFileUri)
        startActivity(intent)
    }

    override fun onClickLihatGambar(riwayat: RiwayatEntity) {
        val intent = Intent(this, Gambar::class.java)
        intent.putExtra("annotatedImageUri", riwayat.annotatedImageUri)
        startActivity(intent)
    }
}