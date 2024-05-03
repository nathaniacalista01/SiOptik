package com.sioptik.main

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.net.toFile
import androidx.fragment.app.commit
import com.sioptik.main.image_processing_integration.JsonFileAdapter
import com.sioptik.main.processing_result.DynamicContentFragment
import com.sioptik.main.processing_result.SharedViewModel

class DetailRiwayat : AppCompatActivity() {
    private val viewModel: SharedViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_riwayat)

        val jsonFileAdapter = JsonFileAdapter()
        val jsonFileUri = intent.getStringExtra("jsonFileUri")
        val jsonTemplate =
            if (jsonFileUri == "test" || jsonFileUri == null) {
                jsonFileAdapter.readJsonFile("test.json", this)
            } else {
                val file = Uri.parse(jsonFileUri).toFile()
                jsonFileAdapter.readJsonFile(file.name, this)
            }

        viewModel.jsonTemplate = jsonTemplate

        supportFragmentManager.commit {
            replace(R.id.jsonFragmentContainer, DynamicContentFragment())
        }
    }
}