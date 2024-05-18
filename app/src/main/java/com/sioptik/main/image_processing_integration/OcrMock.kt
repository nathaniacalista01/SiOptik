package com.sioptik.main.image_processing_integration

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.sioptik.main.tesseract.TesseractHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

class OcrMock(private val context: Context) {

    val tesseractHelper : TesseractHelper = TesseractHelper();
    fun detect(bitmap: List<Bitmap>, apriltagId: Int): JsonTemplate {
        prepareTessData("ind");
        val dataPath = context.filesDir.absolutePath
        tesseractHelper.initTessBaseApi(dataPath, "ind")
        val jsonTemplate = JsonTemplateFactory(context).jsonTemplate(apriltagId)
        var currentIndex = 0;
        for (field in jsonTemplate.fieldNames) {
            val numOfBoxes = jsonTemplate.getBoxes(field)
            var digits = ""
            for (boxIndex in currentIndex until currentIndex + numOfBoxes) {
                if (boxIndex < bitmap.size) {
                    val box = bitmap[boxIndex];
                    val digit = tesseractHelper.recognizeDigits(box)
                    digits += digit
                } else {
                    break
                }
            }

            val number = digits.toIntOrNull() ?: 0

            jsonTemplate.entry(field, number)

            currentIndex += numOfBoxes
        }
        return jsonTemplate
    }
    fun prepareTessData(lang : String) {
        // Path to the internal directory
        val tessdataPath = File(context.filesDir, "tessdata")

        if (!tessdataPath.exists()) {
            if (!tessdataPath.mkdirs()) {
                Log.e("Tesseract", "Failed to create directory: ${tessdataPath.absolutePath}")
                return
            } else {
                Log.i("Tesseract", "Created directory: ${tessdataPath.absolutePath}")
            }
        }

        val tessdataFile = File(tessdataPath, "$lang.traineddata")
        if (!tessdataFile.exists()) {
            try {
                context.assets.open("tessdata/$lang.traineddata").use { inputStream ->
                    FileOutputStream(tessdataFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.i("Tesseract", "Copied '$lang.traineddata' to tessdata")
            } catch (e: IOException) {
                Log.e("Tesseract", "Unable to copy '$lang.traineddata': ", e)
            }
        } else {
            Log.i("Tesseract", "'$lang.traineddata' already exists no need to copy")
        }
    }
}