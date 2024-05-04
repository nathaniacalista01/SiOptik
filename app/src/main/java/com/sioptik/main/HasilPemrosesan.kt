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
import com.sioptik.main.image_processing_integration.JsonFileAdapter
import com.sioptik.main.image_processing_integration.JsonTemplateFactory
import com.sioptik.main.image_processing_integration.OcrMock
import com.sioptik.main.image_processor.ImageProcessor
import com.sioptik.main.processing_result.DynamicContentFragment
import com.sioptik.main.processing_result.FullScreenImageActivity
import com.sioptik.main.processing_result.SharedViewModel
import com.sioptik.main.processing_result.json_parser.parser.JsonParser
import org.opencv.core.Mat
import org.opencv.core.Rect
import com.sioptik.main.riwayat_repository.RiwayatEntity
import com.sioptik.main.riwayat_repository.RiwayatViewModel
import com.sioptik.main.riwayat_repository.RiwayatViewModelFactory
import org.opencv.core.Scalar
import java.util.Date
import kotlin.random.Random

class HasilPemrosesan : AppCompatActivity() {
    private val viewModel: SharedViewModel by viewModels()
    private val riwayatViewModel: RiwayatViewModel by viewModels() {
        RiwayatViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hasil_pemrosesan)

        val imageView: ImageView = findViewById(R.id.processed_image)
        val finishButton: Button = findViewById(R.id.finish_button)

        Log.i("TEST HASIL", intent.getStringExtra("image_uri").toString())

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

                // Process Image
                val detectedBoxes = detectBoxes(bitmap)
                val processedBitmap = processImage(bitmap, detectedBoxes)

                // Crop Detected Boxes for OCR
                val croppedBoxes = cropBoxes(bitmap, detectedBoxes)
//                Log.i("TEST CROPPED BOXES", croppedBoxes.toString())

                imageView.setImageBitmap(processedBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load or process image", Toast.LENGTH_SHORT).show()
            }

        }

        val apriltagId = intent.getIntExtra("apriltag_id", 0)
        val ocr = OcrMock(this)
        val jsonTemplate = ocr.detect(null, apriltagId)

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
}

private fun processImage (bitmap: Bitmap, boxes: List<Rect>) : Bitmap {
    val imgProcessor = ImageProcessor()
    // Initial Mat
    val originalMat = imgProcessor.convertBitmapToMat(bitmap)
    val processedMat = imgProcessor.preprocessImage(originalMat) // Maybe will be used but better save it first
    // Contour Boxes
    val resultImage = imgProcessor.visualizeContoursAndRectangles(processedMat, boxes, Scalar(255.0, .0, 0.0), true, 2)
    return imgProcessor.convertMatToBitmap(resultImage)
}

private fun cropBoxes(bitmap: Bitmap, boxes: List<Rect>) : List<Bitmap> {
    val imgProcessor = ImageProcessor()
    // Initial Mat
    val originalMat = imgProcessor.convertBitmapToMat(bitmap)
    val croppedImages = mutableListOf<Bitmap>()
    boxes.forEach{box ->
        val newMat : Mat = Mat(originalMat, box)
        val newBitmap = imgProcessor.convertMatToBitmap(newMat)
        croppedImages.add(newBitmap)
    }
    return croppedImages
}


private fun detectBoxes (bitmap: Bitmap) : List<Rect> {
    val imgProcessor = ImageProcessor()
    // Initial Mat
    val originalMat = imgProcessor.convertBitmapToMat(bitmap)
    val processedMat = imgProcessor.preprocessImage(originalMat)
    // Detect Boxes
    val boxes = imgProcessor.detectBoxes(processedMat)
    return boxes
}

