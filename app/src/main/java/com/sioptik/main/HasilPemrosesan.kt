package com.sioptik.main

import android.content.ContentValues
import android.content.Context
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
import com.sioptik.main.image_processing_integration.OcrMock
import com.sioptik.main.image_processor.ImageProcessor
import com.sioptik.main.processing_result.DynamicContentFragment
import com.sioptik.main.processing_result.FullScreenImageActivity
import com.sioptik.main.processing_result.SharedViewModel
import com.sioptik.main.processing_result.json_parser.model.BoxMetadata
import com.sioptik.main.processing_result.json_parser.parser.BoxMetadataParser
import com.sioptik.main.riwayat_repository.RiwayatEntity
import com.sioptik.main.riwayat_repository.RiwayatViewModel
import com.sioptik.main.riwayat_repository.RiwayatViewModelFactory
import com.sioptik.main.tesseract.TesseractHelper
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
import kotlin.math.abs

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
        val button : Button = findViewById(R.id.retry_processing_button)

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
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

                // Process Image
                val detectedBoxes = detectBoxes(bitmap, boxesData!!)
                val croppedBoxes = cropBoxes(bitmap, detectedBoxes)

                val box = croppedBoxes[0]
                val resizedBox = Bitmap.createScaledBitmap(box, 35,35, true)

                Log.i("TEST NEEDED BOXES", "Needed Boxes: ${boxesData.data.num_of_boxes}")
                Log.i("TEST DETECTED BOXES", "Detected Boxes: ${detectedBoxes.size}")

                val ocrResults = processBoxes(croppedBoxes)
                val processedBitmap = processImage(bitmap, detectedBoxes, ocrResults)

                imageView.setImageBitmap(resizedBox)
                button.setOnClickListener {
                    saveImageToGallery(this, box, "CroppedImage", "Cropped image saved from OCR processing")
                }
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
}

private fun processImage(bitmap: Bitmap, boxes: List<Rect>, ocrResults: List<String>): Bitmap {
    val imgProcessor = ImageProcessor()
    val originalMat = imgProcessor.convertBitmapToMat(bitmap)
    val processedMat = imgProcessor.preprocessImage(originalMat)
    val resultImage = imgProcessor.visualizeContoursAndRectangles(processedMat, boxes, Scalar(255.0, 0.0, 0.0), Scalar(255.0, 255.0, 0.0), ocrResults, 4)
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


private fun detectBoxes (bitmap: Bitmap, boxesData: BoxMetadata) : List<Rect> {

    val imgProcessor = ImageProcessor()
    val originalMat = imgProcessor.convertBitmapToMat(bitmap)
    val processedMat = imgProcessor.preprocessImage(originalMat)

    // Detect Boxes
    var boxesContainer = mutableListOf<Rect>()
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

    }

    // Eliminate Redundant Boxes
    val similarityCleansedBoxesContainer = eliminateRedundantBoxes(boxesContainer)
    // Eliminate y_level isolated Boxes
    val isolatedCleansedBoxesContainer = eliminateIsolatedBoxes(similarityCleansedBoxesContainer)

    return isolatedCleansedBoxesContainer
}

private fun eliminateRedundantBoxes(boxes :List<Rect>) : List<Rect>{
    val boxesContainer = mutableListOf<Rect>()
    val differenceThreshold = 20
    val scaleThreshold = 10
    boxes.forEach { checkingRect ->
        var foundSimilar = false
        boxesContainer.forEachIndexed { index, iteratingRect ->
            val checkSimilarity = abs(iteratingRect.x - checkingRect.x) < differenceThreshold && abs(iteratingRect.y - checkingRect.y) < differenceThreshold
            if (checkSimilarity){
                val newWidth = if (checkingRect.width > iteratingRect.width) checkingRect.width else iteratingRect.width
                val newHeight = if (checkingRect.height > iteratingRect.height) checkingRect.height else iteratingRect.height
                val newRect = Rect((iteratingRect.x + checkingRect.x)/2, (iteratingRect.y + checkingRect.y)/2, newWidth, newHeight)
                boxesContainer[index] = newRect
                foundSimilar = true
            }
        }
        // If there are no boxes with similarity
        if (!foundSimilar) {
            boxesContainer.add(checkingRect)
        }
    }
    return boxesContainer
}

private fun eliminateIsolatedBoxes(boxes : List<Rect>) : List<Rect> {
    val boxesContainer = mutableListOf<Rect>()
    val differenceThreshold = 20
    val notIsolatedThreshold = 1
    boxes.forEachIndexed { checkingIdx, checkingRect ->
        var notIsolatedCount = 0
        boxes.forEachIndexed{ iteratingIdx, iteratingRect ->
            // Make sure that those Rects are not the same Rect
            if (iteratingIdx != checkingIdx){
                val checkNotIsolated = abs(iteratingRect.y - checkingRect.y) < differenceThreshold
                if (checkNotIsolated){
                    notIsolatedCount++
                }
            }
        }

        if (notIsolatedCount >= notIsolatedThreshold){
            boxesContainer.add(checkingRect)
        }
    }
    return boxesContainer
}

fun saveImageToGallery(context: Context, bitmap: Bitmap, title: String, description: String) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, title)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/YourAppName")
    }

    val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        // Memastikan outputStream tidak null sebelum penggunaan
        context.contentResolver.openOutputStream(it)?.use { outputStream ->
            // Compressing the bitmap
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        } ?: run {
            Toast.makeText(context, "Failed to open output stream", Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_LONG).show()
    } ?: run {
        Toast.makeText(context, "Failed to Save", Toast.LENGTH_LONG).show()
    }
}