package com.sioptik.main.camera_processor

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.camera.core.ImageProxy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale

class CameraProcessor {
    val WIDTH = 2400;

    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun saveBitmapToFile(bitmap: Bitmap?, file: File) {
        FileOutputStream(file).use { out ->
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }

    fun createTempFile(context: Context, type: String): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            "_${type}.jpg",
            storageDir
        ).apply {
            deleteOnExit()
        }
    }


    fun isImageTooWide(contentResolver: ContentResolver, uri: Uri, maxWidth: Int): Boolean {
        return try {
            // Use ContentResolver to open an InputStream from the URI
            val inputStream = contentResolver.openInputStream(uri)
            // Decode only the image bounds without loading the full image into memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            // Close the InputStream
            inputStream?.close()

            // Check if the width exceeds the maximum width threshold
            options.outWidth > maxWidth
        } catch (e: IOException) {
            // Handle any exceptions that may occur
            e.printStackTrace()
            false // Return false in case of an error
        }
    }

    fun scaleDownBitmap(originalBitmap: Bitmap, maxWidth: Int): Bitmap? {
        return try {
            // Calculate the aspect ratio of the original bitmap
            val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            // Create a ByteArrayOutputStream to write the scaled bitmap
            val outputStream = ByteArrayOutputStream()
            // Compress the bitmap to the OutputStream with a quality of 100 (maximum quality)
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            // Create an input stream from the ByteArrayOutputStream
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            // Decode the input stream to get the bitmap
            val options = BitmapFactory.Options().apply {
                // Set inJustDecodeBounds to true to get the dimensions of the bitmap without loading it into memory
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            // Close the InputStream
            inputStream.close()
            // Calculate the sample size based on the maximum width
            options.inSampleSize = calculateSampleSize(options, maxWidth)
            // Decode the input stream with the calculated sample size
            options.inJustDecodeBounds = false
            val scaledBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(outputStream.toByteArray()), null, options)
            // Return the scaled bitmap
            scaledBitmap
        } catch (e: IOException) {
            // Handle any exceptions that may occur
            e.printStackTrace()
            null // Return null in case of an error
        }
    }

    // Function to make the image smaller by scaling down while preserving aspect ratio
    fun scaleDownImage(contentResolver: ContentResolver, uri: Uri, maxWidth: Int): Bitmap? {
        return try {
            // Use ContentResolver to open an InputStream from the URI
            val inputStream = contentResolver.openInputStream(uri)
            // Decode the image file to get its dimensions
            val options = BitmapFactory.Options().apply {
                // Set inJustDecodeBounds to true to get the dimensions of the image without loading it into memory
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            // Close the InputStream
            inputStream?.close()
            // Calculate the sample size based on the maximum width
            options.inSampleSize = calculateSampleSize(options, maxWidth)
            // Decode the image file with the calculated sample size
            options.inJustDecodeBounds = false
            val scaledBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri), null, options)
            // Optionally rotate the Bitmap if necessary
            scaledBitmap // Return the scaled and rotated Bitmap
        } catch (e: IOException) {
            // Handle any exceptions that may occur
            e.printStackTrace()
            null // Return null in case of an error
        }
    }

    // Function to calculate the sample size for scaling down the image
    private fun calculateSampleSize(options: BitmapFactory.Options, maxWidth: Int): Int {
        val width = options.outWidth
        var inSampleSize = 1

        if (width > maxWidth) {
            // Calculate the sample size to reduce the width to fit within the maximum width
            inSampleSize = Math.ceil((width.toFloat() / maxWidth.toFloat()).toDouble()).toInt()
        }
        return inSampleSize
    }
}