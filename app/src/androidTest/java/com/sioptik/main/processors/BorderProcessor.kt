package com.sioptik.main.processors

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.sioptik.main.R
import com.sioptik.main.image_processor.ImageProcessor
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar

class BorderProcessor {

    fun processBorderDetection(context : Context, bitmap: Bitmap) : List<Rect> {
        val imgProcessor = ImageProcessor()
        val borderImageDrawable = R.drawable.border_smaller
        val borderImageSmallDrawable = R.drawable.border_smallest
        val borderImage = imgProcessor.loadDrawableImage(context, borderImageDrawable)
        val borderImageSmall = imgProcessor.loadDrawableImage(context, borderImageSmallDrawable)

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

    fun processAndCropImage(bitmap: Bitmap, borderContainer : List<Rect>): Bitmap {
        val imgProcessor = com.sioptik.main.processors.ImageProcessor()

        // Initial Mat
        val originalMat = imgProcessor.convertBitmapToMat(bitmap)
        val resultImage = imgProcessor.visualizeContoursAndBorders(originalMat, borderContainer, Scalar(0.0, 255.0, 255.0), 6) // Comment out this line to see processed image

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

            croppedResultImage = Mat(resultImage, Rect(tlx, tly, (brx - tlx), (bry - tly)))
        }
        return imgProcessor.convertMatToBitmap(croppedResultImage)
    }

}