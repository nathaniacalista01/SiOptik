package com.sioptik.main

import android.content.Context
import androidx.core.net.toFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.sioptik.main.image_processing_integration.JsonFileAdapter
import com.sioptik.main.image_processing_integration.OcrMock
import com.sioptik.main.riwayat_repository.RiwayatDao
import com.sioptik.main.riwayat_repository.RiwayatDatabase
import com.sioptik.main.riwayat_repository.RiwayatEntity
import com.sioptik.main.riwayat_repository.RiwayatRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date


@RunWith(AndroidJUnit4::class)
@MediumTest
class ImageProcessingIntegrationTest {
    private lateinit var db: RiwayatDatabase
    private lateinit var riwayatDao: RiwayatDao
    private lateinit var riwayatRepository: RiwayatRepository

    @Before
    fun setupDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, RiwayatDatabase::class.java).build()
        riwayatDao = db.riwayatDao()
        riwayatRepository = RiwayatRepository(riwayatDao)
    }

    @Test
    fun imageProcessingFlowTest() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

        val apriltagId = 101
        val ocrMock = OcrMock(appContext)
        val jsonTemplate = ocrMock.detect(null, apriltagId)
        val jsonFileAdapter = JsonFileAdapter()
        val jsonUri = jsonFileAdapter.saveJsonFile(jsonTemplate, appContext)

        val writtenJsonTemplate = jsonFileAdapter.readJsonFile(jsonUri.toFile().name, appContext)
        println("Intended JSON template:")
        println(jsonTemplate.toString())
        println()
        println("Written JSON template: ")
        println(writtenJsonTemplate.toString())
        assert(writtenJsonTemplate.toString() == jsonTemplate.toString())

        val riwayatToInsert = RiwayatEntity(
            0,
            apriltagId,
            Date(),
            jsonUri.toString(),
            "originalImageUri",
            "annotatedImageUri"
        )

        riwayatRepository.insert(riwayatToInsert)
        riwayatRepository.deleteAll()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }
}