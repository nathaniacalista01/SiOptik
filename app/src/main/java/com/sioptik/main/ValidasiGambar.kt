package com.sioptik.main

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.sioptik.main.apriltag.AprilTagDetection
import com.sioptik.main.apriltag.AprilTagFunction
import com.sioptik.main.apriltag.AprilTagNative
import com.sioptik.main.border_processor.BorderProcessor
import com.sioptik.main.camera_processor.CameraProcessor
import com.sioptik.main.image_processor.ImageProcessor
import com.sioptik.main.tesseract.TesseractHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar

class ValidasiGambar : AppCompatActivity() {
    private lateinit var processedBitmap: Bitmap;
    private lateinit var tesseractHelper: TesseractHelper;
    private lateinit var progressBar: ProgressBar;
    private lateinit var  loadingOverlayBg: View;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("VALIDASI", "BARU MASUK")
        setContentView(R.layout.validasi_gambar)

        val retryButton = findViewById<Button>(R.id.retryButton)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val imageView: ImageView = findViewById(R.id.imageValidation)
        progressBar = findViewById(R.id.progressBar)
        loadingOverlayBg = findViewById(R.id.loadingOverlayBg)

        // Show the loading indicator immediately
        showLoading(true, progressBar, loadingOverlayBg)

        retryButton.setOnClickListener {
            val cameraIntent = Intent(this, Kamera::class.java)
            startActivity(cameraIntent)
        }

        val frameLayout = findViewById<FrameLayout>(R.id.imageValidationContainer)
        val apriltagTagView = findViewById<Button>(R.id.april_tag)
        val imageUriString = intent.getStringExtra("image_uri")
        AprilTagNative.apriltag_init("tag36h10", 2, 1.0, 0.0, 4)

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imageView.setImageURI(imageUri)
            CoroutineScope(Dispatchers.IO).launch {
                processImage(imageUri, imageView, progressBar, loadingOverlayBg, apriltagTagView, sendButton)
            }
        } else {
            Toast.makeText(this, "Image is NULL", Toast.LENGTH_SHORT).show()
            showLoading(false, progressBar, loadingOverlayBg) // Hide loading because nothing to process
        }
    }


    private fun processImage(imageUri: Uri, imageView: ImageView, progressBar: ProgressBar, loadingOverlayBg: View, apriltagTagView: Button, sendButton: Button) {
        try {
            var borderProcessor = BorderProcessor()
            val aprilTagFunction = AprilTagFunction()
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            val borders = borderProcessor.processBorderDetection(this, bitmap)
            val processedBitmap = borderProcessor.processAndCropImage(bitmap, borders)
            val apriltag = aprilTagFunction.processAprilTagDetection(processedBitmap)



            // Check if Borders and AprilTag are detected
            if (borders.size != 4 || apriltag == null) {
                if (borders.size != 4) {
                    Toast.makeText(this, "Borders are invalid", Toast.LENGTH_SHORT).show()
                }
                if (apriltag == null) {
                    Toast.makeText(this, "April Tag is not detected", Toast.LENGTH_SHORT).show()
                }

                runOnUiThread {
                    apriltagTagView.text = apriltag?.id.toString()
                    imageView.setImageBitmap(processedBitmap)
                    sendButton.isEnabled = false
                    showLoading(false, progressBar, loadingOverlayBg)
                }
            } else {
                // Save Cropped Image
                val cameraProcessor = CameraProcessor()
                val imageProcessor = ImageProcessor()
                val tempFile = cameraProcessor.createTempFile(this, "CROPPED")
                val resizedBitmap = imageProcessor.resizeImage(processedBitmap, imageProcessor.WIDTH, imageProcessor.HEIGHT)
                cameraProcessor.saveBitmapToFile(resizedBitmap, tempFile)

                val savedUri = FileProvider.getUriForFile(
                    this,
                    "com.sioptik.main.provider",
                    tempFile
                )

                runOnUiThread {
                    apriltagTagView.text = apriltag?.id.toString()
                    imageView.setImageBitmap(processedBitmap)
                    sendButton.setOnClickListener {
                        showLoading(true, progressBar, loadingOverlayBg);
                        Intent(this, HasilPemrosesan::class.java).also { previewIntent ->
                            previewIntent.putExtra("image_uri", savedUri.toString())
                            previewIntent.putExtra("april_tag", apriltag.id.toString())
                            startActivity(previewIntent)
                        }
                    }
                    showLoading(false, progressBar, loadingOverlayBg)
                }


            }


        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load or process image", Toast.LENGTH_SHORT).show()
            sendButton.isEnabled = false
            showLoading(false, progressBar, loadingOverlayBg)
        }
    }

    override fun onResume() {
        super.onResume()
        showLoading(false, progressBar, loadingOverlayBg)
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
