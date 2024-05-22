package com.sioptik.main

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sioptik.main.apriltag.AprilTagNative
import com.sioptik.main.box_processor.BoxProcessor
import com.sioptik.main.processing_result.json_parser.model.BoxMetadata
import com.sioptik.main.processing_result.json_parser.parser.BoxMetadataParser
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.core.Core
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Rect
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class BoxDetectionTest {

    fun getNV21(inputWidth: Int, inputHeight: Int, scaled: Bitmap): ByteArray {
        var scaled = scaled
        val argb = IntArray(inputWidth * inputHeight)

        // Ensure that the bitmap is in ARGB_8888 format
        scaled = scaled.copy(Bitmap.Config.ARGB_8888, true)
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
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
        } catch (e: IndexOutOfBoundsException) {
        }
    }

    @Test
    fun testAprilTagInit() {
        AprilTagNative.apriltag_init("tag36h10", 0, 8.0, 0.0, 4)
        Assert.assertTrue(true)
    }

    fun scaleDownBitmap(originalBitmap: Bitmap, maxWidth: Int): Bitmap? {
        return try {
            // Calculate the aspect ratio of the original bitmap
            val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height

            // Create a ByteArrayOutputStream to write the scaled bitmap
            val outputStream = ByteArrayOutputStream()

            // Compress the bitmap to the OutputStream with a quality of 100 (maximum quality)
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

            // Create an input stream from the ByteArrayOutputStream
            var inputStream = ByteArrayInputStream(outputStream.toByteArray())

            // Decode the input stream to get the bitmap
            val options = BitmapFactory.Options()

            // Set inJustDecodeBounds to true to get the dimensions of the bitmap without loading it into memory
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)

            // Close the InputStream
            inputStream.close()

            // Calculate the sample size based on the maximum width
            options.inSampleSize = calculateSampleSize(options, maxWidth)

            // Decode the input stream with the calculated sample size
            options.inJustDecodeBounds = false
            inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val scaledBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Return the scaled bitmap
            scaledBitmap
        } catch (e: IOException) {
            // Handle any exceptions that may occur
            e.printStackTrace()
            null // Return null in case of an error
        }
    }

    private fun calculateSampleSize(options: BitmapFactory.Options, maxWidth: Int): Int {
        val width = options.outWidth
        var inSampleSize = 1
        if (width > maxWidth) {
            // Calculate the sample size to reduce the width to fit within the maximum width
            inSampleSize = Math.ceil(width.toDouble() / maxWidth).toInt()
        }
        return inSampleSize
    }

    fun writeToInternalStorage(fileName: String?, content: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            val fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            fos.write(content.toByteArray())
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    @Test
    public fun TestBoxDetection() {
        OpenCVLoader.initDebug();
        val context = InstrumentationRegistry.getInstrumentation().context
        val assetManager = context.assets
        var files: Array<String?>? = null

        var jsonString = "";

        try {
            files = assetManager.list("")
            if (files != null) {
                for (file in files) {
                    if (file!!.contains("json", ignoreCase = true)) {
                        // Open the JSON metadata file
                        try {
                            val inputStream = assetManager.open(file!!)
                            jsonString = inputStream.bufferedReader().use { it.readText() }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d("TEST CANNOT READ JSON", "Failed to read JSON")
                        }
                    }
                }
            }

        }   catch (e: IOException) {
            Log.e("TEST", "Got IO Exception")
            e.printStackTrace()
        }

        var sum_error = 0.0;
        var count = 0;

        try {
            files = assetManager.list("")
            if (files != null) {
                for (file in files) {
                    if (file!!.contains("jpg", ignoreCase = true)){
                        Log.i("TEST FILE", file.toString())

                        // DETECT APRIL TAG
                        val inputStream = assetManager.open(file!!)
                        var bitmap = BitmapFactory.decodeStream(inputStream)
                        bitmap = AprilTagTest.scaleDownBitmap(bitmap, 2400)
                        val width = bitmap.width
                        val height = bitmap.height
                        val byteArray: ByteArray = getNV21(width, height, bitmap)
                        AprilTagNative.apriltag_init("tag36h10", 2, 1.0, 0.0, 4)
                        val detections = AprilTagNative.apriltag_detect_yuv(byteArray, width, height)
                        val aprilTag = detections[0].id.toString()
                        Log.i("TEST ${file}", aprilTag)

                        // BOXES DATA
                        var boxesData: BoxMetadata? = null
                        boxesData = BoxMetadataParser.parse(jsonString, aprilTag)
                        Log.i("TEST", "BOXES DATA PARSING === DONE")

                        // BORDER DETECTION
                        val boxProcessor = BoxProcessor()
                        val detectedBoxes = boxProcessor.detectBoxes(bitmap, boxesData!!)
                        val needed_boxes = boxesData.data.num_of_boxes
                        val detected_boxes = detectedBoxes.size
                        Log.i("TEST NEEDED BOXES", "Needed Boxes: ${needed_boxes}")
                        Log.i("TEST DETECTED BOXES", "Detected Boxes: ${detected_boxes}")
                        Log.i("TEST", "BOXES DETECTION === DONE")

                        // Save detected boxes to file
                        saveBoxesToGallery(detectedBoxes, boxProcessor, bitmap, context)

                        val error = abs(detected_boxes - needed_boxes).toFloat() / needed_boxes * 100
                        sum_error += error
                        count += 1
                        inputStream.close()
                    }
                }
                val mean_error = sum_error / count
                Log.i("TEST FINAL RESULT", "Mean Error : ${mean_error}")
            }
        } catch (e: IOException) {
            Log.e("TEST", "Got IO Exception")
            e.printStackTrace()
        }
    }

    private fun saveBoxesToGallery(
        detectedBoxes: List<Rect>,
        boxProcessor: BoxProcessor,
        bitmap: Bitmap,
        context: Context
    ) {
        val tolerance = 5;
        val sortedBoxes = detectedBoxes.sortedWith(Comparator { a, b ->
            if (abs(a.y - b.y) <= tolerance) {
                a.x.compareTo(b.x)
            } else {
                a.y.compareTo(b.y)
            }
        })
        val croppedBoxes = boxProcessor.cropBoxes(bitmap, sortedBoxes)

        for (i in croppedBoxes.indices) {
            saveImageToGallery(context, croppedBoxes[i], "Test_${i}.jpg", "Cropped box")
        }
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap, title: String, description: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, title)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/YourAppName")
        }

        val uri: Uri? = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        uri?.let {
            // Memastikan outputStream tidak null sebelum penggunaan
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                // Compressing the bitmap
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            } ?: run {
                Log.d("SaveImageToGallery", "Failed to open output stream");
            }
            Log.d("SameImageToGallery", "Saved to Gallery")

        } ?: run {
            Log.d("SaveImageToGallery", "Failed to save")
        }
    }
}