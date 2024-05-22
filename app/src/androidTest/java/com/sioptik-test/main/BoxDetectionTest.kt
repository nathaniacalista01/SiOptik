package com.`sioptik-test`.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sioptik.main.apriltag.AprilTagNative
import com.sioptik.main.border_processor.BorderProcessor
import com.`sioptik-test`.main.box_processor.BoxProcessor
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


    @Test
    public fun TestBoxDetection() {
        OpenCVLoader.initDebug();
        val context : Context= InstrumentationRegistry.getInstrumentation().context
//        val context : Context = ApplicationProvider.getApplicationContext()
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
                        val width = bitmap.width
                        val height = bitmap.height
                        val byteArray: ByteArray = getNV21(width, height, bitmap)
                        AprilTagNative.apriltag_init("tag36h10", 2, 1.0, 0.0, 4)
                        val detections = AprilTagNative.apriltag_detect_yuv(byteArray, width, height)
                        if (detections.size > 0){

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

                            val error = abs(detected_boxes - needed_boxes).toFloat() / needed_boxes * 100
                            sum_error += error
                            count += 1

                            val mean_error = sum_error / count
                            Log.i("TEST RESULT", "Mean Error : ${mean_error}")

                        } else {
                            Log.i("TEST APRIL TAG", "April Tag Not Detected")
                        }

                        inputStream.close()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("TEST", "Got IO Exception")
            e.printStackTrace()
        }
    }
}