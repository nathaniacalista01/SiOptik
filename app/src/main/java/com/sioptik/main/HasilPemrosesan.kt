package com.sioptik.main

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sioptik.main.box_processor.BoxProcessor
import com.sioptik.main.image_processing_integration.JsonFileAdapter
import com.sioptik.main.image_processing_integration.JsonTemplateFactory
import com.sioptik.main.image_processing_integration.OcrMock
import com.sioptik.main.image_processor.ImageProcessor
import com.sioptik.main.processing_result.DynamicContentFragment
import com.sioptik.main.processing_result.FullScreenImageActivity
import com.sioptik.main.processing_result.SharedViewModel
import com.sioptik.main.processing_result.json_parser.model.BoxData
import com.sioptik.main.processing_result.json_parser.model.BoxMetadata
import com.sioptik.main.processing_result.json_parser.parser.BoxMetadataParser
import com.sioptik.main.processing_result.json_parser.parser.JsonParser
import org.json.JSONObject
import com.sioptik.main.tesseract.TesseractHelper
import org.opencv.core.Mat
import org.opencv.core.Rect
import com.sioptik.main.riwayat_repository.RiwayatEntity
import com.sioptik.main.riwayat_repository.RiwayatViewModel
import com.sioptik.main.riwayat_repository.RiwayatViewModelFactory
import org.opencv.core.Scalar
import kotlin.math.abs
import kotlin.math.sqrt
import java.util.Date
import kotlin.random.Random
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class HasilPemrosesan : AppCompatActivity() {
    private val lang = "ind"
    private val viewModel: SharedViewModel by viewModels()
    private val riwayatViewModel: RiwayatViewModel by viewModels() {
        RiwayatViewModelFactory(this)
    }
    private lateinit var tesseractHelper: TesseractHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hasil_pemrosesan)

        prepareTessData()
        val dataPath = filesDir.absolutePath
        tesseractHelper = TesseractHelper()
        tesseractHelper.initTessBaseApi(dataPath, lang)

        val imageView: ImageView = findViewById(R.id.processed_image)

        // Get Extra
        val finishButton: Button = findViewById(R.id.finish_button)

        val imageUriString = intent.getStringExtra("image_uri")
        val april_tag = intent.getStringExtra("april_tag")
        Log.i("TEST APRIL TAG", "${april_tag}")


        // Open the JSON metadata file
        var boxesData : BoxMetadata? = null
        try {
            val inputStream = resources.openRawResource(R.raw.box_metadata)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            boxesData = BoxMetadataParser.parse(jsonString, april_tag!!)
        }  catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to read JSON", Toast.LENGTH_SHORT).show()
        }


        // Image Processing
        if (imageUriString != null){
            val imageUri = Uri.parse(imageUriString)

            imageView.setImageURI(imageUri)
            imageView.setOnClickListener {
                val intent = Intent(this, FullScreenImageActivity::class.java)
                intent.putExtra("imageUri", imageUriString)
                startActivity(intent)
            }

            try {
                val boxProcessor = BoxProcessor()
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

                // Process Image
                val detectedBoxes = boxProcessor.detectBoxes(bitmap, boxesData!!)
                val croppedBoxes = boxProcessor.cropBoxes(bitmap, detectedBoxes)

                Log.i("TEST NEEDED BOXES", "Needed Boxes: ${boxesData?.data?.num_of_boxes}")
                Log.i("TEST DETECTED BOXES", "Detected Boxes: ${detectedBoxes.size}")

                val ocrResults = processBoxes(croppedBoxes)
                val processedBitmap = processImage(bitmap, detectedBoxes, ocrResults)

                imageView.setImageBitmap(processedBitmap)
            } catch (e: Exception) {
                Log.e("Image Processing", "Failed to load or process image", e)
                Toast.makeText(this, "Failed to load or process image", Toast.LENGTH_SHORT).show()
            }

        }

        // Get Metadata through April_Tag
        val ocr = OcrMock(this)
        val jsonTemplate = ocr.detect(null, april_tag!!.toInt())
        viewModel.jsonTemplate = jsonTemplate

        finishButton.setOnClickListener {
            val viewModelJsonTemplate = viewModel.jsonTemplate
            if (viewModelJsonTemplate != null && imageUriString != null) {
                val jsonFileAdapter = JsonFileAdapter()
                val jsonFileUri = jsonFileAdapter.saveJsonFile(viewModelJsonTemplate, this)
              val riwayat = RiwayatEntity(
                  0,
                  viewModelJsonTemplate.apriltagId,
                  Date(),
                  jsonFileUri.toString(),
                  imageUriString,
                  imageUriString
              )
                riwayatViewModel.insertRiwayat(riwayat);
            }

        }

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

    private fun processImage(bitmap: Bitmap, boxes: List<Rect>, ocrResults: List<String>): Bitmap {
        val imgProcessor = ImageProcessor()
        val originalMat = imgProcessor.convertBitmapToMat(bitmap)
        val processedMat = imgProcessor.preprocessImage(originalMat)
        val resultImage = imgProcessor.visualizeContoursAndRectangles(processedMat, boxes, Scalar(255.0, 0.0, 0.0), Scalar(255.0, 255.0, 0.0), ocrResults, 4)
        return imgProcessor.convertMatToBitmap(resultImage)
    }
}



