package com.sioptik.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class Gambar : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gambar)
        val annotatedImageView = findViewById<ImageView>(R.id.annotated_image)
        val annotatedImageUriString = intent.getStringExtra("annotatedImageUri")
        Log.d("Gambar", annotatedImageUriString ?: "annotated uri null")
        val annotatedImageUri =
            if(annotatedImageUriString == "test" || annotatedImageUriString == null) {
                Log.d("Gambar", "Masuk ke image test")
                val annotatedImageDirectory = File(filesDir, "annotated-image")
                val imageFile = File(annotatedImageDirectory, "test-image.jpg")
                Uri.fromFile(imageFile)
            } else {
                Log.d("Gambar", "Ga masuk ke image test")
                Uri.parse(annotatedImageUriString)
            }

        val annotatedImageBitmap = decodeBitmapFromUri(this, annotatedImageUri, 300, 400)
        if (annotatedImageBitmap != null) {
            annotatedImageView.setImageBitmap(annotatedImageBitmap)
        }

        val kembaliButton = findViewById<Button>(R.id.kembali_button)
        kembaliButton.setOnClickListener {
            finish()
        }
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}


fun decodeBitmapFromUri(context: Context, imageUri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    // Obtain the stream from the URI
    val inputStream = context.contentResolver.openInputStream(imageUri)
    BitmapFactory.decodeStream(inputStream, null, options)
    inputStream?.close()

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false
    val inputStream2 = context.contentResolver.openInputStream(imageUri)
    val bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
    inputStream2?.close()

    return bitmap
}
