package com.sioptik.main.image_processing_integration

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.sioptik.main.ml.Model
import com.sioptik.main.ml.Ocr
import com.sioptik.main.processing_result.json_parser.model.BoxMetadata
import com.sioptik.main.tesseract.TesseractHelper
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

class OcrMock(private val context: Context) {

    val tesseractHelper : TesseractHelper = TesseractHelper();

    fun detectModel(bitmap : List<Bitmap>, apriltagId: Int, boxesData : BoxMetadata) : JsonTemplate{
        val model = Ocr.newInstance(context)

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1,35,35,1), DataType.FLOAT32)
        val jsonTemplate = JsonTemplateFactory(context).jsonTemplate(apriltagId)
        var currentIndex = 0
        for(field  in jsonTemplate.fieldNames){
            val numOfBoxes = jsonTemplate.getBoxes(field)
            var digits = ""
            val firstBoxInField = boxesData.data.boxes.get(currentIndex)
            Log.i("First box in field X : ", firstBoxInField.x.toString())
            Log.i("First box in field Y : ", firstBoxInField.y.toString())
            for (boxIndex in currentIndex until currentIndex + numOfBoxes){
                if(boxIndex < bitmap.size){
                    val box = bitmap[boxIndex]
                    val resizedBox = Bitmap.createScaledBitmap(box, 35, 35,true)
                    val byteBuffer = convertBitmapToByteBuffer(resizedBox)
                    inputFeature0.loadBuffer(byteBuffer)
                    val outputs = model.process(inputFeature0)
                    val outputFeature0 = outputs.outputFeature0AsTensorBuffer
                    val result = outputFeature0.floatArray
                    val maxIndex = result.indices.maxBy { result[it] } ?: -1
                    val resultString = "Index : %d result : %d".format(boxIndex, maxIndex)
                    Log.i("OCR", resultString)
                    digits += maxIndex.toString();
                }else{
                    break
                }
            }
            val number = digits.toIntOrNull() ?: 0

            jsonTemplate.entry(field, number)

            currentIndex += numOfBoxes
        }
        model.close()
        return jsonTemplate;
    };
    fun detect(bitmap: List<Bitmap>, apriltagId: Int): JsonTemplate {
        prepareTessData("ind");
        val dataPath = context.filesDir.absolutePath
        tesseractHelper.initTessBaseApi(dataPath, "ind")
        val jsonTemplate = JsonTemplateFactory(context).jsonTemplate(apriltagId)
        var currentIndex = 0;
        for (field in jsonTemplate.fieldNames) {
            val numOfBoxes = jsonTemplate.getBoxes(field)
            var digits = ""
            for (boxIndex in currentIndex until currentIndex + numOfBoxes) {
                if (boxIndex < bitmap.size) {
                    val box = bitmap[boxIndex];
                    val digit = tesseractHelper.recognizeDigits(box)
                    digits += digit
                } else {
                    break
                }
            }

            val number = digits.toIntOrNull() ?: 0

            jsonTemplate.entry(field, number)

            currentIndex += numOfBoxes
        }
        return jsonTemplate
    }
    fun prepareTessData(lang : String) {
        // Path to the internal directory
        val tessdataPath = File(context.filesDir, "tessdata")

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
                context.assets.open("tessdata/$lang.traineddata").use { inputStream ->
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

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {

        val byteBuffer = ByteBuffer.allocateDirect(4 * bitmap.width * bitmap.height)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.rewind()

        val intValues = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            val red = ((pixelValue shr 16) and 0xFF)
            val green = ((pixelValue shr 8) and 0xFF)
            val blue = (pixelValue and 0xFF)
            val normalizedPixelValue = (red + green + blue) / 3.0f
            byteBuffer.putFloat(normalizedPixelValue)
        }
        return byteBuffer
    }

}