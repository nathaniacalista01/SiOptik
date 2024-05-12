package com.sioptik.main.camera_processor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.sioptik.main.R
import com.sioptik.main.apriltag.AprilTagDetection
import com.sioptik.main.apriltag.AprilTagNative
import com.sioptik.main.image_processor.ImageProcessor
import org.opencv.core.Rect
import java.util.concurrent.Executors

class DetectionThread(private val context: Context, private val borderTl: View, private val borderTr: View, private val borderBl: View, private val borderBr: View, private val imageCapture: ImageCapture, private val apriltagBtn: Button) :
    Thread() {
    private val mainHandler = Handler(Looper.getMainLooper())
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
        AprilTagNative.apriltag_init("tag36h10", 2, 1.0, 0.0, 4)
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

    override fun run() {
        Log.i("TAG", "THREADING")
        while (!isInterrupted && !end) {
            Log.i("HEHE", "MASUK")
            var ok = false;

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
                            val apriltag = processAprilTagDetection(scaledBitmap)
                            mainHandler.post {
                                if (apriltag == null) {
                                    apriltagBtn.text = "UNDEFINED"
                                } else {
                                    apriltagBtn.text = apriltag.id.toString()
                                }
                            }
                            ok = true;
                            Log.i("APRILTAG", apriltag?.id.toString())
                        }
                    }
                }
            )



            try {
                sleep(2000)
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