package com.sioptik.main.camera_processor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.sioptik.main.R
import com.sioptik.main.image_processor.ImageProcessor
import org.opencv.core.Rect
import java.util.concurrent.Executors

class DetectionThread(private val context: Context, private val borderTl: View, private val borderTr: View, private val borderBl: View, private val borderBr: View, private val imageCapture: ImageCapture) :
    Thread() {
    private val cameraProcessor = CameraProcessor()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val borderImageDrawable = R.drawable.border4
    private val borderImageSmallDrawable = R.drawable.border7
    private val imgProcessor = ImageProcessor()
    private val borderImage = imgProcessor.loadDrawableImage(context, borderImageDrawable)
    private val borderImageSmall = imgProcessor.loadDrawableImage(context, borderImageSmallDrawable)
    public var end: Boolean = false
    fun initialize() {
        Log.i(TAG, "Detection thread initialize")
    }

    fun processBitmap(bitmap: Bitmap) {
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
                Log.i("TEST BORDER DETECTION", "Ketemu")
                processButton(index, true)
            } else {
                Log.i("TEST BORDER DETECTION", "Border Not Found")
                processButton(index, false)
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    fun processButton(idx: Int, status: Boolean) {
        val color = if (status) R.color.cream else R.color.white
        val targetView = when (idx) {
            1 -> borderTr
            2 -> borderBl
            3 -> borderBr
            else -> borderTl
        }

        // Run UI updates on the main thread
        targetView.post {
            val resolvedColor = ContextCompat.getColor(context, color)
            targetView.setBackgroundColor(resolvedColor)
        }
    }


    override fun destroy() {
        cameraExecutor.shutdown()
    }

    override fun run() {
        Log.i("TAG", "THREADING")
        while (!isInterrupted && !end) {
            Log.i("HEHE", "MASUK")

            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("FAILED", "Photo capture failed: ${exc.message}", exc)
                        end = true
                        currentThread().interrupt()
                    }

                    override fun onCaptureSuccess(image: ImageProxy) {
                        super.onCaptureSuccess(image)
                        val bitmap = cameraProcessor.imageProxyToBitmap(image)
                        val scaledBitmap = cameraProcessor.scaleDownBitmap(bitmap!!, 1600)
                        image.close()
                        if (scaledBitmap != null) {
                            processBitmap(scaledBitmap)
                        }
                    }
                }
            )

            try {
                sleep(1000)
            } catch (ie: InterruptedException) {
                break
            }
        }
        Log.i("TAG", "THREADING SELESAI")
    }



    companion object {
        private const val TAG = "DetectionThread"
        private const val MAX_FRAME_QUEUE_SIZE = 1
    }
}