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
import com.sioptik.main.apriltag.AprilTagNative
import com.sioptik.main.camera_processor.CameraProcessor
import com.sioptik.main.image_processor.ImageProcessor
import com.sioptik.main.processing_result.FullScreenImageActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import java.util.ArrayList

class ValidasiGambar : AppCompatActivity() {
    private lateinit var processedBitmap: Bitmap;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("VALIDASI", "BARU MASUK")
        setContentView(R.layout.validasi_gambar)

        val retryButton = findViewById<Button>(R.id.retryButton)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val imageView: ImageView = findViewById(R.id.imageValidation)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val loadingOverlayBg: View = findViewById(R.id.loadingOverlayBg)

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
                val imageUri = Uri.parse(imageUriString)
                processImage(imageUri, imageView, progressBar, loadingOverlayBg, apriltagTagView, sendButton)
            }
        } else {
            Toast.makeText(this, "Image is NULL", Toast.LENGTH_SHORT).show()
            showLoading(false, progressBar, loadingOverlayBg) // Hide loading because nothing to process
        }
    }


    private fun processImage(imageUri: Uri, imageView: ImageView, progressBar: ProgressBar, loadingOverlayBg: View, apriltagTagView: Button, sendButton: Button) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            val borders = processBorderDetection(bitmap)
            val processedBitmap = processAndCropImage(bitmap, borders)
            val apriltag = processAprilTagDetection(processedBitmap)

            runOnUiThread {
                if (apriltag != null) {
                    apriltagTagView.text = apriltag.id.toString()
                }

                imageView.setImageBitmap(processedBitmap)

                // Check if Borders and AprilTag are detected
                if (borders.size != 4 || apriltag == null) {
                    if (borders.size != 4) {
                        Toast.makeText(this, "Borders are invalid", Toast.LENGTH_SHORT).show()
                    }
                    if (apriltag == null) {
                        Toast.makeText(this, "April Tag is not detected", Toast.LENGTH_SHORT).show()
                    }
                    sendButton.isEnabled = false
                } else {
                    // Save Cropped Image
                    val cameraProcessor = CameraProcessor()
                    val tempFile = cameraProcessor.createTempFile(this, "CROPPED")
                    cameraProcessor.saveBitmapToFile(processedBitmap, tempFile)

                    val savedUri = FileProvider.getUriForFile(
                        this,
                        "com.sioptik.main.provider",
                        tempFile
                    )

                    sendButton.setOnClickListener {
                        val previewIntent = Intent(this, HasilPemrosesan::class.java)
                        previewIntent.putExtra("image_uri", savedUri.toString())
                        startActivity(previewIntent)
                    }
                }

                // Processing complete, hide loading indicator
                showLoading(false, progressBar, loadingOverlayBg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load or process image", Toast.LENGTH_SHORT).show()
            sendButton.isEnabled = false
            showLoading(false, progressBar, loadingOverlayBg)
        }
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



    private fun processBorderDetection(bitmap: Bitmap) : List<Rect> {
        val imgProcessor = ImageProcessor()
        val borderImageDrawable = R.drawable.border_smaller
        val borderImageSmallDrawable = R.drawable.border_smallest
        val borderImage = imgProcessor.loadDrawableImage(this, borderImageDrawable)
        val borderImageSmall = imgProcessor.loadDrawableImage(this, borderImageSmallDrawable)

        // Initial Mat
        val originalMat = imgProcessor.convertBitmapToMat(bitmap)

        // SplittedImages
        val splittedImagesRects = imgProcessor.splitImageRects(originalMat)
        val splittedImages = imgProcessor.splitImage(originalMat)
        val borderContainer = mutableListOf<Rect>()

        splittedImages.forEachIndexed { index, mat ->
            // Detect Borders
            var border = imgProcessor.detectBorder(imgProcessor.convertToGray(mat), imgProcessor.convertToGray(borderImage))
            if (border == null){
                border = imgProcessor.detectBorder(imgProcessor.convertToGray(mat), imgProcessor.convertToGray(borderImageSmall))
            }

            if (border != null){
                // Assume that it will only get 1
                val currentRect = splittedImagesRects[index]
                val adjustedBorder = Rect((currentRect.x + border.x), (currentRect.y + border.y), border.width, border.height)
                borderContainer.add(adjustedBorder)
            } else {
                Log.i("TEST BORDER DETECTION", "Border Not Found")
            }
        }
        return borderContainer
    }

    private fun processAndCropImage(bitmap: Bitmap, borderContainer : List<Rect>): Bitmap {
        val imgProcessor = ImageProcessor()

        // Initial Mat
        val originalMat = imgProcessor.convertBitmapToMat(bitmap)
        val resultImage = imgProcessor.visualizeContoursAndRectangles(originalMat, borderContainer, Scalar(0.0, 255.0, 255.0), false,6) // Comment out this line to see processed image

        // Crop
        var croppedResultImage = resultImage
        if (borderContainer.size == 4){
            val padding = 30;
            val w = originalMat.width()
            val h = originalMat.height()
            val tl_rect = borderContainer[0]
            val tr_rect = borderContainer[1]
            val bl_rect = borderContainer[2]
            val br_rect = borderContainer[3]

            // Adjustment By Checking Four corners
            // Four corners might be difference in X and Y
            var tlx = if (tl_rect.x <= bl_rect.x) tl_rect.x else bl_rect.x
            var tly = if (tl_rect.y <= tr_rect.y) tl_rect.y else tr_rect.y
            var brx = if (br_rect.x >= tr_rect.x) br_rect.x else tr_rect.x
            var bry = if (br_rect.y >= bl_rect.y) br_rect.y else bl_rect.y

            // Adjustment if it is beyond the limit of the image
            tlx = if (tlx - padding <= 0) 0 else tlx- padding
            tly = if (tly - padding <= 0) 0 else tly - padding
            brx = if (brx + (br_rect.width) + padding >= w) w else brx + (br_rect.width) + padding
            bry = if (bry + (br_rect.height) + padding >= h) h else bry + (br_rect.height) + padding
//            Log.i("TEST BOUNDARIES", tlx.toString() + "||" + tly.toString() + "||" + brx.toString() + "||" + bry.toString())

            croppedResultImage = Mat(resultImage, Rect(tlx, tly, (brx - tlx), (bry - tly)))
        }
        return imgProcessor.convertMatToBitmap(croppedResultImage)
    }

    private fun processAprilTagDetection (bitmap: Bitmap) : AprilTagDetection? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val byteArray = getNV21(width, height/4, bitmap)
            val detections : ArrayList<AprilTagDetection> = AprilTagNative.apriltag_detect_yuv(byteArray, width, height)

            val apriltag = detections[0]
            return apriltag
        } catch (e: Exception){
            return null
        }
    }


    fun getNV21(inputWidth: Int, inputHeight: Int, scaled: Bitmap): ByteArray? {
        var scaled = scaled
        val argb = IntArray(inputWidth * inputHeight)

        // Ensure that the bitmap is in ARGB_8888 format
        scaled = scaled.copy(Bitmap.Config.ARGB_8888, true)
        scaled.getPixels(argb, 0, inputWidth, 3 * (inputWidth / 4) - 1, 0, inputWidth / 4, inputHeight)
        val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight)
        scaled.recycle()
        return yuv
    }

    fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        try {
            val frameSize = width * height
            var yIndex = 0
            var uvIndex = frameSize
            var a: Int
            var R: Int
            var G: Int
            var B: Int
            var Y: Int
            var U: Int
            var V: Int
            var index = 0
            for (j in 0 until height) {
                for (i in 0 until width) {
                    a = argb[index] and -0x1000000 shr 24 // a is not used obviously
                    R = argb[index] and 0xff0000 shr 16
                    G = argb[index] and 0xff00 shr 8
                    B = argb[index] and 0xff shr 0

                    // well known RGB to YUV algorithm
                    Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                    U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                    V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128

                    // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                    //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                    //    pixel AND every other scanline.
                    yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
                    if (j % 2 == 0 && index % 2 == 0) {
                        yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                        yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                    }
                    index++
                }
            }
        } catch (e: IndexOutOfBoundsException) { }
    }

}
