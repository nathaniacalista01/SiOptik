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
import com.sioptik.main.processing_result.json_parser.model.BoxData
import com.sioptik.main.processing_result.json_parser.model.BoxMetadata
import com.sioptik.main.processing_result.json_parser.parser.BoxMetadataParser
import com.sioptik.main.processing_result.json_parser.parser.JsonParser
import org.json.JSONObject
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import kotlin.math.sqrt

class HasilPemrosesan : AppCompatActivity() {
    private val viewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hasil_pemrosesan)
        val imageView: ImageView = findViewById(R.id.processed_image)

        // Get Extra
        val imageUriString = intent.getStringExtra("image_uri")
        val april_tag = intent.getStringExtra("april_tag")


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
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

                // Process Image
                val detectedBoxes = detectBoxes(bitmap, boxesData!!)
                val processedBitmap = processImage(bitmap, detectedBoxes)

                // Crop Detected Boxes for OCR
                val croppedBoxes = cropBoxes(bitmap, detectedBoxes)


                imageView.setImageBitmap(processedBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
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


private fun detectBoxes (bitmap: Bitmap, boxesData: BoxMetadata) : List<Rect> {

    val imgProcessor = ImageProcessor()
    // Initial Mat
    val originalMat = imgProcessor.convertBitmapToMat(bitmap)
    val processedMat = imgProcessor.preprocessImage(originalMat)
//    // Detect Boxes

    val boxesContainer = mutableListOf<Rect>()
    val w = bitmap.width
    val h = bitmap.height
    val ref_w = boxesData.w_ref
    val ref_h = boxesData.h_ref
    val w_ratio = w.toFloat() / ref_w.toFloat()
    val h_ratio = h.toFloat() / ref_h.toFloat()

    val boxesRectData = boxesData.data.boxes
    val searchPadding = 150;
    boxesRectData.forEach { boxData ->
        val temp_x = (boxData.x * w_ratio).toInt()
        val temp_y = (boxData.y * h_ratio).toInt()
        val temp_w = (boxData.w * w_ratio).toInt()
        val temp_h = temp_w
        val testRect = Rect(temp_x, temp_y, temp_w, temp_h) // Only for testing

        val adjusted_x = if (temp_x - searchPadding > 0) (temp_x - searchPadding) else 0
        val adjusted_y = if (temp_y - searchPadding > 0) (temp_y - searchPadding) else 0

        val end_x = temp_x + temp_w + searchPadding
        val adjusted_w = if (end_x > w) (w - adjusted_x) else (end_x - adjusted_x)
        val end_y = temp_y + temp_h + searchPadding
        val adjusted_h = if (end_y > h) (h - adjusted_y) else (end_y - adjusted_y)

        val searchingRect: Rect = Rect(adjusted_x, adjusted_y, adjusted_w, adjusted_h)

        // Crop the searching area
        val searchingMat: Mat = Mat(processedMat, searchingRect)
        val boxes = imgProcessor.detectBoxes(searchingMat)

        // All-in
        boxes.forEach { rect ->
            val adjustingRect : Rect = Rect((rect.x + adjusted_x), (rect.y + adjusted_y), rect.width, rect.height)
            boxesContainer.add(adjustingRect)
        }

        // Selection by Center Distance
//        val chosenRect = boxSearchSelection(boxes, searchingRect, adjusted_x, adjusted_y)
//        if (chosenRect != null){
//            val adjustingRect : Rect = Rect((chosenRect.x + adjusted_x), (chosenRect.y + adjusted_y), chosenRect.width, chosenRect.height)
//            boxesContainer.add(adjustingRect)
//        }
    }

    return boxesContainer
}

private fun boxSearchSelection(boxes: List<Rect>, searchingRect: Rect, adjusted_x : Int,adjusted_y : Int) : Rect? {
    // From the list, only select 1
    val centerSearchX = ((searchingRect.x + searchingRect.width)/2)
    val centerSearchY = ((searchingRect.y + searchingRect.height)/2)
    var suitableRect: Rect? = null
    var minDistance: Double = 999.0
    boxes.forEach { rect ->
        val adjustingRect : Rect = Rect((rect.x + adjusted_x), (rect.y + adjusted_y), rect.width, rect.height)
        val centerBoxX = ((adjustingRect.x + adjustingRect.width)/2)
        val centerBoxY = ((adjustingRect.y + adjustingRect.height)/2)
        val distance = sqrt(((centerSearchX - centerBoxX)*(centerSearchX - centerBoxX) + (centerSearchY - centerBoxY)*(centerSearchY - centerBoxY)).toDouble())
        if (distance < minDistance){
            suitableRect = rect
            minDistance = distance
        }
    }
    return suitableRect
}


