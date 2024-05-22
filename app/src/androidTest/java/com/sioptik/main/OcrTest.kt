package com.sioptik.main

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sioptik.main.image_processing_integration.OcrMock
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OcrTest {
    @Test
    fun testOcr() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ocrMock = OcrMock(context)
        ocrMock.prepareTessData("ind")
        val tesseractHelper = ocrMock.tesseractHelper
        val dataPath = context.filesDir.absolutePath
        tesseractHelper.initTessBaseApi(dataPath, "ind")

        val listFiles = context.assets.list("ocrtest")
        if (listFiles == null) {
            Log.d("OcrTest", "listFiles is null")
        } else if (listFiles.isEmpty()) {
            Log.d("OcrTest", "listFiles is empty")
        } else {
            listFiles.forEach { fileName ->
                if (fileName.endsWith(".jpg", ignoreCase = true)) {
                    Log.d("OcrTest", "filename: $fileName")
                    try {
                        // Open the .jpg file and get the bitmap
                        context.assets.open("ocrtest/$fileName").use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            // Recognize digits from the bitmap
                            val result = tesseractHelper.recognizeDigits(bitmap)
                            Log.d("OcrTest", "result: $result")
                        }
                    } catch (e: IOException) {
                        Log.e("OcrTest", "Error opening file: $fileName", e)
                    }
                }

            }

        }
        Log.d("OcrTest", "Test finished")
    }
}