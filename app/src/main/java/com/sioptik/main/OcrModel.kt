package com.sioptik.main

import android.content.Context
import android.graphics.Bitmap
import com.sioptik.main.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OcrModel(private val context: Context) {

    fun detect(bitmap : Bitmap) : String{
        val resizedBox = Bitmap.createScaledBitmap(bitmap, 35,35, true);
        val byteBuffer = convertBitmapToByteBuffer(resizedBox)
        val model = Model.newInstance(context)
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1,35,35,1), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer
        val result = outputFeature0.floatArray
        val maxIndex = result.indices.maxBy { result[it] } ?: -1
        if(maxIndex.toString() == "10"){
            return "0"
        }else{
            return maxIndex.toString();
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