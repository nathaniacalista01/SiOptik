package com.sioptik.main

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sioptik.main.image_processor.ImageProcessor
import com.sioptik.main.processing_result.DynamicContentFragment
import com.sioptik.main.processing_result.FullScreenImageActivity
import com.sioptik.main.processing_result.SharedViewModel
import com.sioptik.main.processing_result.json_parser.parser.JsonParser
import com.sioptik.main.tesseract.TesseractHelper
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class HasilPemrosesan : AppCompatActivity() {
    private val lang = "ind"
    private val viewModel: SharedViewModel by viewModels()
    private lateinit var tesseractHelper: TesseractHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hasil_pemrosesan)

        prepareTessData()
        val dataPath = filesDir.absolutePath
        tesseractHelper = TesseractHelper()
        tesseractHelper.initTessBaseApi(dataPath, lang)

        val imageView: ImageView = findViewById(R.id.processed_image)
        val imageUriString = intent.getStringExtra("image_uri")

        if (imageUriString != null){
            val imageUri = Uri.parse(imageUriString)

            imageView.setImageURI(imageUri)
            imageView.setOnClickListener {
                val intent = Intent(this, FullScreenImageActivity::class.java)
                intent.putExtra("imageUri", imageUriString)
                startActivity(intent)
            }

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                val detectedBoxes = detectBoxes(bitmap)
                val croppedBoxes = cropBoxes(bitmap, detectedBoxes)
                val ocrResults = processBoxes(croppedBoxes)
                val processedBitmap = processImage(bitmap, detectedBoxes, ocrResults)

                imageView.setImageBitmap(processedBitmap)
            } catch (e: Exception) {
                Log.e("Image Processing", "Failed to load or process image", e)
                Toast.makeText(this, "Failed to load or process image", Toast.LENGTH_SHORT).show()
            }

        }

        val jsonString = """
            {
              "title": "Hasil Pemilihan Presiden RI",
              "description": "Deskripsi Hihihi",
              "aprilTagId": 100,
              "tpsId": 10,
              "candidates": [
                {
                  "orderNumber": 1,
                  "choiceName": "Alis",
                  "totalVoters": 500000
                },
                {
                  "orderNumber": 2,
                  "choiceName": "Prabski",
                  "totalVoters": 450000
                },
                {
                  "orderNumber": 3,
                  "choiceName": "Skipper",
                  "totalVoters": 350000
                }
              ]
            }
            """.trimIndent()

        val jsonData = JsonParser.parse(jsonString)
        viewModel.jsonData = jsonData

        supportFragmentManager.commit {
            replace(R.id.fragmentContainerView, DynamicContentFragment())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tesseractHelper.destroy()
    }

    private fun processBoxes(croppedBoxes: List<Bitmap>): List<String> {
        val ocrResults = mutableListOf<String>()
        croppedBoxes.forEach { croppedBitmap ->
            val text = tesseractHelper.recognizeDigits(croppedBitmap)
            ocrResults.add(text ?: "X")
        }
        return ocrResults
    }

    private fun prepareTessData() {
        // Path to the internal directory
        val tessdataPath = File(filesDir, "tessdata")

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
                assets.open("tessdata/$lang.traineddata").use { inputStream ->
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

private fun processImage(bitmap: Bitmap, boxes: List<Rect>, ocrResults: List<String>): Bitmap {
    val imgProcessor = ImageProcessor()
    val originalMat = imgProcessor.convertBitmapToMat(bitmap)
    val processedMat = imgProcessor.preprocessImage(originalMat)
    val resultImage = imgProcessor.visualizeContoursAndRectangles(processedMat, boxes, Scalar(255.0, 0.0, 0.0), ocrResults, 2)
    return imgProcessor.convertMatToBitmap(resultImage)
}

private fun cropBoxes(bitmap: Bitmap, boxes: List<Rect>): List<Bitmap> {
    val imgProcessor = ImageProcessor()
    val originalMat = imgProcessor.convertBitmapToMat(bitmap)
    return boxes.map { box ->
        val newMat = Mat(originalMat, box)
        imgProcessor.convertMatToBitmap(newMat)
    }
}

private fun detectBoxes(bitmap: Bitmap): List<Rect> {
    val imgProcessor = ImageProcessor()
    val originalMat = imgProcessor.convertBitmapToMat(bitmap)
    val processedMat = imgProcessor.preprocessImage(originalMat)
    return imgProcessor.detectBoxes(processedMat)
}