package com.sioptik.main.box_processor

import android.graphics.Bitmap
import android.util.Log
import com.sioptik.main.image_processor.ImageProcessor
import com.sioptik.main.processing_result.json_parser.model.BoxMetadata
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class DetectionResults(
    val testingRects: List<Rect>,
    val finalRects: List<Rect>,
    val allBoxesDetected : Boolean,
)
class BoxProcessor {

    fun cropBoxes(bitmap: Bitmap, boxes: List<Rect?>): List<Bitmap?> {
        val imgProcessor = ImageProcessor()
        val originalMat = imgProcessor.convertBitmapToMat(bitmap)
        return boxes.map { box ->
            if (box == null) {
                null
            } else {
                val croppedMat = Mat(originalMat, box)
                Imgproc.cvtColor(croppedMat, croppedMat, Imgproc.COLOR_BGR2GRAY)
                Imgproc.GaussianBlur(croppedMat, croppedMat, Size(5.0, 5.0), 0.0) // Applying Gaussian Blur to reduce noise
                croppedMat.convertTo(croppedMat, -1, 1.5, 10.0)
                Imgproc.adaptiveThreshold(croppedMat, croppedMat, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0)

                val bounds = findTightestBlackBounds(croppedMat)
                if (bounds.width > 0 && bounds.height > 0) { // Ensure bounds are valid
                    val tightCroppedMat = Mat(croppedMat, bounds)
                    imgProcessor.convertMatToBitmap(tightCroppedMat)
                } else {
                    imgProcessor.convertMatToBitmap(croppedMat) // Fallback to the original cropped area
                }
            }
        }
    }


    private fun findTightestBlackBounds(mat: Mat, buffer: Int = 5): Rect {
        var minX = mat.cols()
        var maxX = 0
        var minY = mat.rows()
        var maxY = 0

        for (y in 0 until mat.rows()) {
            for (x in 0 until mat.cols()) {
                if (mat.get(y, x)[0].toInt() == 255) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        minX = (minX - buffer).coerceAtLeast(0)
        minY = (minY - buffer).coerceAtLeast(0)
        maxX = (maxX + buffer).coerceAtMost(mat.cols() - 1)
        maxY = (maxY + buffer).coerceAtMost(mat.rows() - 1)

        // Return the adjusted bounds
        if (minX <= maxX && minY <= maxY) {
            return Rect(minX, minY, maxX - minX + 1, maxY - minY + 1)
        }

        // Fallback to full matrix if no bounds were detected
        return Rect(0, 0, mat.cols(), mat.rows())
    }
    fun detectBoxes(bitmap: Bitmap, boxesData: BoxMetadata): DetectionResults {
        val imgProcessor = ImageProcessor()
        val originalMat = imgProcessor.convertBitmapToMat(bitmap)
        val processedMat = imgProcessor.preprocessImage(originalMat)

        // Detect Boxes
        val tempRect = mutableListOf<Rect>()
        var boxesContainer = mutableListOf<Rect>()
        val w = bitmap.width
        val h = bitmap.height
        val ref_w = boxesData.w_ref
        val ref_h = boxesData.h_ref
        val w_ratio = w.toFloat() / ref_w.toFloat()
        val h_ratio = h.toFloat() / ref_h.toFloat()

        val margin = 12
        val searchPadding = 130
        val boxes = boxesData.data.boxes
        boxes.forEach { boxData ->
            val temp_x = (boxData.x * w_ratio).toInt()
            val temp_y = (boxData.y * h_ratio).toInt()
            val temp_w = (boxData.w * w_ratio).toInt()
            val temp_h = temp_w
            val testingRect = Rect(temp_x, temp_y, temp_w, temp_h) // Only for testing
            tempRect.add(testingRect)
            val adjusted_x = if (temp_x - searchPadding > 0) (temp_x - searchPadding) else 0
            val adjusted_y = if (temp_y - searchPadding > 0) (temp_y - searchPadding) else 0
            val adjusted_w = if (temp_x + temp_w + searchPadding > w) (w - adjusted_x) else (temp_w + 2 * searchPadding)
            val adjusted_h = if (temp_y + temp_h + searchPadding > h) (h - adjusted_y) else (temp_h + 2 * searchPadding)

            val searchingRect = Rect(adjusted_x, adjusted_y, adjusted_w, adjusted_h)

            // Crop the searching area
            val searchingMat = Mat(processedMat, searchingRect)
            val detectedBoxes = imgProcessor.detectBoxes(searchingMat)

            // All-in
            detectedBoxes.forEach { rect ->
                val finalX = max(rect.x + margin, 0)
                val finalY = max(rect.y + margin, 0)
                val finalW = min(rect.width - 2 * margin, searchingMat.width() - finalX)
                val finalH = min(rect.height - 2 * margin, searchingMat.height() - finalY)
                val finalRect = Rect(finalX + adjusted_x, finalY + adjusted_y, finalW, finalH)
                boxesContainer.add(finalRect)
            }
        }

        // Eliminate Redundant Boxes
        val similarityCleansedBoxesContainer = eliminateRedundantBoxes(boxesContainer)
        // Eliminate y_level isolated Boxes
        val isolatedCleansedBoxesContainer = eliminateIsolatedBoxes(similarityCleansedBoxesContainer)
        // Eliminate inside boxes
        val insiderCleansedBoxesContainer = eliminateInsideBoxes(isolatedCleansedBoxesContainer)

        return DetectionResults(tempRect,insiderCleansedBoxesContainer,
            insiderCleansedBoxesContainer.size == boxes.size)
    }

    private fun eliminateRedundantBoxes(boxes :List<Rect>) : List<Rect>{
        val boxesContainer = mutableListOf<Rect>()
        val differenceThreshold = 20
        boxes.forEach { checkingRect ->
            var foundSimilar = false
            boxesContainer.forEachIndexed { index, iteratingRect ->
                val checkSimilarity = abs(iteratingRect.x - checkingRect.x) < differenceThreshold && abs(iteratingRect.y - checkingRect.y) < differenceThreshold
                if (checkSimilarity){
                    val newWidth = if (checkingRect.width > iteratingRect.width) checkingRect.width else iteratingRect.width
                    val newHeight = if (checkingRect.height > iteratingRect.height) checkingRect.height else iteratingRect.height
                    val newRect = Rect((iteratingRect.x + checkingRect.x)/2, (iteratingRect.y + checkingRect.y)/2, newWidth, newHeight)
                    boxesContainer[index] = newRect
                    foundSimilar = true
                }
            }
            // If there are no boxes with similarity
            if (!foundSimilar) {
                boxesContainer.add(checkingRect)
            }
        }
        return boxesContainer
    }

    private fun eliminateIsolatedBoxes(boxes : List<Rect>) : List<Rect> {
        val boxesContainer = mutableListOf<Rect>()
        val differenceThreshold = 15
        val notIsolatedThreshold = 1
        boxes.forEachIndexed { checkingIdx, checkingRect ->
            var notIsolatedCount = 0
            boxes.forEachIndexed{ iteratingIdx, iteratingRect ->
                // Make sure that those Rects are not the same Rect
                if (iteratingIdx != checkingIdx){
                    val checkNotIsolated = abs(iteratingRect.y - checkingRect.y) < differenceThreshold
                    if (checkNotIsolated){
                        notIsolatedCount++
                    }
                }
            }

            if (notIsolatedCount >= notIsolatedThreshold){
                boxesContainer.add(checkingRect)
            }
        }
        return boxesContainer
    }

    private fun eliminateInsideBoxes(boxes: List<Rect>) : List<Rect> {
        val distThreshold = 5;
        val boxesContainer = mutableListOf<Rect>()
        boxes.forEachIndexed { checkingIdx, checkingRect ->
            var foundInside = false;
            boxes.forEachIndexed { iteratingIdx, iteratingRect ->
                if (checkingIdx != iteratingIdx){
                    val lBound = iteratingRect.x - distThreshold;
                    val rBound = iteratingRect.x + iteratingRect.width + distThreshold;
                    val tBound = iteratingRect.y - distThreshold;
                    val bBound = iteratingRect.y + iteratingRect.height + distThreshold;
                    var horizontalInside = (checkingRect.x in lBound..rBound) && ((checkingRect.x + checkingRect.width) in lBound..rBound)
                    var verticalInside = (checkingRect.y in tBound..bBound) && ((checkingRect.y + checkingRect.height) in tBound..bBound)
                    if (horizontalInside && verticalInside){
                        foundInside = true
                    }
                }
            }
            if (!foundInside) {
                boxesContainer.add(checkingRect)
            }
        }
        return boxesContainer
    }
}