package com.sioptik.main.image_processor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc



class ImageProcessor {
    val WIDTH = 2400;
    val HEIGHT = (1.573 * WIDTH).toInt(); // Based on observation, the expected ratio is 1.573

    fun preprocessImage(srcMat: Mat): Mat {
        val grayMat = convertToGray(srcMat)
        val blurredMat = applyGaussianBlur(grayMat)
        val thresholdMat = applyAdaptiveThreshold(blurredMat)
        val morphMat = applyMorphologicalOperations(thresholdMat)
//        val cannyMat = applyCannyDetection(morphMat)
        return morphMat
    }

    fun convertToGray(colorMat: Mat): Mat {
        val grayMat = Mat()
        Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_RGB2GRAY)
        return grayMat
    }

    private fun applyGaussianBlur(grayMat: Mat): Mat {
        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(3.0, 3.0), 0.0)
        return blurredMat
    }

    private fun applyAdaptiveThreshold(blurredMat: Mat): Mat {
        val thresholdMat = Mat()
        Imgproc.adaptiveThreshold(
            blurredMat,
            thresholdMat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,
            2.0
        )
        return thresholdMat
    }

    private fun applyMorphologicalOperations(thresholdMat: Mat): Mat {
        val morphMat = Mat()
        val element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        Imgproc.morphologyEx(
            thresholdMat,
            morphMat,
            Imgproc.MORPH_CLOSE,
            element
        ) // fill holes
        Imgproc.morphologyEx(
            morphMat,
            morphMat,
            Imgproc.MORPH_OPEN,
            element
        ) //remove noise

        // Might be interchangable (https://docs.opencv.org/3.4/d3/dbe/tutorial_opening_closing_hats.html)
        Imgproc.erode(morphMat, morphMat, element)
        Imgproc.dilate(morphMat, morphMat, element)
        return morphMat
    }

    private fun applyCannyDetection (mat: Mat): Mat {
        val edges = Mat(mat.rows(), mat.cols(), mat.type())
//        Imgproc.Canny(mat, edges, 75.0,  200.0)
        Imgproc.Canny(mat, edges, 00.0,  200.0) // Ini threshold kinda trial and error, cari yang bagus
        return edges
    }

    fun convertMatToBitmap(processedMat: Mat): Bitmap {
        val resultBitmap =
            Bitmap.createBitmap(processedMat.cols(), processedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(processedMat, resultBitmap)
        return resultBitmap
    }

    fun convertBitmapToMat(bitmap: Bitmap): Mat {
        val originalMat = Mat()
        Utils.bitmapToMat(bitmap, originalMat)
        return originalMat
    }

    fun detectBoxes(processedMat: Mat): List<Rect> {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            processedMat,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        val squares = mutableListOf<Rect>()

        val verticeNumTarget = 8; // Trial and Error

        contours.forEach {
            // Minimum size allowed for consideration
            val approxCurve = MatOfPoint2f()
            val contour2f = MatOfPoint2f(*it.toArray())

            // Processing on mMop2f1 which is in type of MatOfPoint2f
            val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
//            val approxDistance = 3.0
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true)

            val numberVertices = approxCurve.total().toInt()
            if (numberVertices in 4..verticeNumTarget) {
                val rect = Imgproc.boundingRect(MatOfPoint(*approxCurve.toArray()))
                if (checkBoxesSelection(processedMat, rect)) {
                    squares.add(rect)
                }
            }

        }
        if (squares.size == 0){
            Log.i("TEST DETECT BOX", "NOT FOUND")
        }
        return squares
    }

    fun checkBoxesSelection(mat: Mat, rect: Rect) : Boolean{
        val aspect_threshold = 0.25

        // wlt = width lower threshold, wut = width upper threshold
        // By Experience
        val wlt = 50
        val wut = 100

        val aspectRatio = rect.width.toDouble() / rect.height.toDouble()

        // Check one by one condition
        // Check aspect ratio -> To Avoid Rectangle
        if (aspectRatio <= (1 - aspect_threshold) || aspectRatio >= (1 + aspect_threshold)){
            return false
        }
        // Check size -> To avoid Noises and Big Box
        if (rect.width < wlt || rect.width > wut){
            return false
        }
        
        return true
    }

    fun detectColorSpace(mat: Mat): String {
        val numChannels = mat.channels()

        return when (numChannels) {
            1 -> "Grayscale"
            3 -> "BGR"
            4 -> "BGRA"
            else -> "Unknown"
        }
    }


    fun visualizeContoursAndRectangles(
        processedMat: Mat,
        rectangles: List<Rect>,
        colorScale: Scalar,
        fontColorScale: Scalar,
        ocrResults: List<String>,
        width: Int
    ): Mat {

        if (detectColorSpace(processedMat) == "Grayscale") {
            Imgproc.cvtColor(processedMat, processedMat, Imgproc.COLOR_GRAY2BGR)
        }

        rectangles.forEachIndexed { index, rect ->
            Imgproc.rectangle(
                processedMat,
                rect.tl(),
                rect.br(),
                colorScale,
                width
            )
            // Draw OCR result text
            Imgproc.putText(
                processedMat,
                ocrResults.getOrNull(index) ?: "",
                rect.tl(),
                Imgproc.FONT_HERSHEY_DUPLEX,
                2.0,
                fontColorScale,
                4
            )
        }

        return processedMat
    }

    fun visualizeContoursAndBorders(
        processedMat: Mat,
        rectangles: List<Rect>,
        colorScale: Scalar,
        width: Int
    ): Mat {

        if (detectColorSpace(processedMat) == "Grayscale") {
            Imgproc.cvtColor(processedMat, processedMat, Imgproc.COLOR_GRAY2BGR)
        }

        rectangles.forEachIndexed { index, rect ->
            Imgproc.rectangle(
                processedMat,
                rect.tl(),
                rect.br(),
                colorScale,
                width
            )
        }

        return processedMat
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    fun loadDrawableImage(context: Context, drawableId: Int): Mat {
        // Load the drawable image as a Bitmap
        val drawable = context.resources.getDrawable(drawableId, null)
        val bitmap = (drawable as BitmapDrawable).bitmap

        // Convert Bitmap to OpenCV Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        return mat
    }

    fun splitImage (image:Mat) : List<Mat> {
        // Split itu n x n parts
        val n = 3
        val w = image.width()
        val h = image.height()
        val part_w = (w / n).toInt()
        val part_h = (h / n).toInt()
        val image_parts = mutableListOf<Mat>()

        val tl : Mat = Mat(image, Rect(0,0, part_w, part_h))        // val tl_rect : Rect
        image_parts.add(tl)
        val tr : Mat = Mat(image, Rect((n-1)*part_w, 0, part_w, part_h)) // val tr_rect : Rect
        image_parts.add(tr)
        val bl : Mat = Mat(image, Rect(0, (n-1)*part_h, part_w, part_h)) // val bl_rect: Rect
        image_parts.add(bl)
        val br : Mat = Mat(image, Rect((n-1)*part_w, (n-1)*part_h, part_w, part_h))  //val br_rect : Rect
        image_parts.add(br)

        return image_parts
    }

    fun splitImageRects (image:Mat) : List<Rect> {
        // Split itu n x n parts
        val n = 3
        val w = image.width()
        val h = image.height()
        val part_w = (w / n)
        val part_h = (h / n)
        val rects = mutableListOf<Rect>()

        val tl : Rect =  Rect(0,0, part_w, part_h)        // val tl_rect : Rect
        rects.add(tl)
        val tr : Rect = Rect((n-1)*part_w, 0, part_w, part_h) // val tr_rect : Rect
        rects.add(tr)
        val bl : Rect = Rect(0, (n-1)*part_h, part_w, part_h) // val bl_rect: Rect
        rects.add(bl)
        val br : Rect = Rect((n-1)*part_w, (n-1)*part_h, part_w, part_h)  //val br_rect : Rect
        rects.add(br)

        return rects
    }

    fun detectBorder(mainImage: Mat, templateImage: Mat): Rect? {
        val result = Mat()
        Imgproc.matchTemplate(mainImage, templateImage, result, Imgproc.TM_CCOEFF_NORMED)

        // Set a threshold for the matching result
        val threshold = 0.7
//        val matches = mutableListOf<Rect>()

        val w = templateImage.cols()
        val h = templateImage.rows()

        // Find template matches
        val mmr = Core.minMaxLoc(result)
        if (mmr.maxVal >= threshold) {
            // Draw rectangle around best match
            val matchLoc = mmr.maxLoc
            return Rect(matchLoc.x.toInt(), matchLoc.y.toInt(), w, h)

        }
        return null
    }

//    fun resizeImage(bitmap: Bitmap, width: Int): Bitmap {
//        val w = bitmap.width
//        val h = bitmap.height
//        val aspRat = h.toFloat() / w.toFloat()
//        val W = width
//        val H = (W * aspRat).toInt()
//        val b = Bitmap.createScaledBitmap(bitmap, W, H, false)
//        return b
//    }

    fun resizeImage(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)
    }
}
