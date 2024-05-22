package com.sioptik.main

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sioptik.main.box_processor.BoxProcessor
import com.sioptik.main.image_processing_integration.JsonFileAdapter
import com.sioptik.main.image_processing_integration.JsonTemplate
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.core.Rect
import org.opencv.core.Scalar
import java.util.Date
import kotlin.math.abs

class HasilPemrosesan : AppCompatActivity() {
    private val lang = "ind"
    private val viewModel: SharedViewModel by viewModels()
    private val riwayatViewModel: RiwayatViewModel by viewModels() {
        RiwayatViewModelFactory(this)
    }
    private lateinit var croppedBoxes : List<Bitmap>;
    private lateinit var jsonTemplate: JsonTemplate;
    private lateinit var container: DynamicContentFragment;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hasil_pemrosesan)

        val imageView: ImageView = findViewById(R.id.processed_image)
        val button : Button = findViewById(R.id.retry_processing_button)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val loadingOverlayBg: View = findViewById(R.id.loadingOverlayBg)
        val finishButton: Button = findViewById(R.id.finish_button)

        showLoading(true, progressBar, loadingOverlayBg)

        val imageUriString = intent.getStringExtra("image_uri")
        val april_tag = intent.getStringExtra("april_tag")
        Log.i("TEST APRIL TAG", "${april_tag}")

        var boxesData : BoxMetadata? = null
        try {
            val inputStream = resources.openRawResource(R.raw.box_metadata)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            boxesData = BoxMetadataParser.parse(jsonString, april_tag!!)
        }  catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to read JSON", Toast.LENGTH_SHORT).show()
        }

        if (imageUriString != null){
            val imageUri = Uri.parse(imageUriString)

            imageView.setImageURI(imageUri)
            imageView.setOnClickListener {
                val intent = Intent(this, FullScreenImageActivity::class.java)
                intent.putExtra("imageUri", imageUriString)
                startActivity(intent)
            }

            CoroutineScope(Dispatchers.IO).launch {
                startRecognition(applicationContext, imageUri, boxesData!!, imageView, button, april_tag, progressBar, loadingOverlayBg)
            }
        }

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

                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                startActivity(intent);
            }


        }
    }

    private fun startRecognition(context: Context, imageUri: Uri, boxesData: BoxMetadata, imageView: ImageView, button: Button, apriltag: String?, progressBar: ProgressBar, loadingOverlayBg: View) {
        try {
            val boxProcessor = BoxProcessor()
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

            // Process Image
            val detectedBoxes = boxProcessor.detectBoxes(bitmap, boxesData)
            val tolerance = 5
            val sortedBoxes = detectedBoxes.sortedWith(Comparator { a, b ->
                if (abs(a.y - b.y) <= tolerance) {
                    a.x.compareTo(b.x)
                } else {
                    a.y.compareTo(b.y)
                }
            })
            croppedBoxes = boxProcessor.cropBoxes(bitmap, sortedBoxes)

            val box = croppedBoxes[0]
            val resizedBox = Bitmap.createScaledBitmap(box, 35, 35, true)

            Log.i("TEST NEEDED BOXES", "Needed Boxes: ${boxesData.data.num_of_boxes}")
            Log.i("TEST DETECTED BOXES", "Detected Boxes: ${detectedBoxes.size}")

            val ocrResults = processBoxes(croppedBoxes)
            val processedBitmap = processImage(bitmap, sortedBoxes, ocrResults)
            val ocr = OcrMock(this)
            jsonTemplate = ocr.detect(croppedBoxes, apriltag!!.toInt())
            viewModel.jsonTemplate = jsonTemplate

            runOnUiThread {
                imageView.setImageBitmap(processedBitmap)
                button.setOnClickListener {
                    saveImageToGallery(
                        this,
                        box,
                        "CroppedImage",
                        "Cropped image saved from OCR processing"
                    )
                }

                Log.i("TEST", jsonTemplate.toString())
                supportFragmentManager.commit {
                    container = DynamicContentFragment()
                    replace(R.id.fragmentContainerView, container)
                }
                showLoading(false, progressBar, loadingOverlayBg)
            }
        } catch (e: Exception) {
            Log.e("Image Processing", "Failed to load or process image", e)
            runOnUiThread {
                Toast.makeText(this, "Failed to load or process image", Toast.LENGTH_SHORT).show()
                showLoading(false, progressBar, loadingOverlayBg)
            }
        }
    }

    private fun processBoxes(croppedBoxes: List<Bitmap>): List<String> {
        val ocrResults = mutableListOf<String>()
        var number = 1;
        croppedBoxes.forEach { croppedBitmap ->
            ocrResults.add(number.toString() ?: "X")
            number += 1;
        }
        return ocrResults
    }

    private fun processImage(bitmap: Bitmap, boxes: List<Rect>, ocrResults: List<String>): Bitmap {
        val imgProcessor = ImageProcessor()
        val originalMat = imgProcessor.convertBitmapToMat(bitmap)
        val processedMat = imgProcessor.preprocessImage(originalMat)
        val resultImage = imgProcessor.visualizeContoursAndRectangles(processedMat, boxes, Scalar(255.0, 0.0, 0.0), Scalar(255.0, 255.0, 0.0), ocrResults, 4)
        return imgProcessor.convertMatToBitmap(resultImage)
    }

    private fun showLoading(show: Boolean, progressBar: ProgressBar, loadingOverlay: View) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            loadingOverlay.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
            loadingOverlay.visibility = View.GONE
        }
    }
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