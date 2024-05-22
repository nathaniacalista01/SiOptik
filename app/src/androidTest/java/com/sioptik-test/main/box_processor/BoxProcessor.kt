package com.`sioptik-test`.main.box_processor

import android.graphics.Bitmap
import android.util.Log
import com.`sioptik-test`.main.image_processor.ImageProcessor
import com.sioptik.main.processing_result.json_parser.model.BoxMetadata
import org.opencv.core.Mat
import org.opencv.core.Rect
import kotlin.math.abs

class BoxProcessor {

    fun cropBoxes(bitmap: Bitmap, boxes: List<Rect>): List<Bitmap> {
        val imgProcessor = ImageProcessor()
        val originalMat = imgProcessor.convertBitmapToMat(bitmap)
        return boxes.map { box ->
            val newMat = Mat(originalMat, box)
            imgProcessor.convertMatToBitmap(newMat)
        }
    }
    fun detectBoxes (bitmap: Bitmap, boxesData: BoxMetadata) : List<Rect> {

        val imgProcessor = ImageProcessor()
        val originalMat = imgProcessor.convertBitmapToMat(bitmap)
        val processedMat = imgProcessor.preprocessImage(originalMat)

        // Detect Boxes
        var boxesContainer = mutableListOf<Rect>()
        val w = bitmap.width
        val h = bitmap.height
        val ref_w = boxesData.w_ref
        val ref_h = boxesData.h_ref
        val w_ratio = w.toFloat() / ref_w.toFloat()
        val h_ratio = h.toFloat() / ref_h.toFloat()

        val boxesRectData = boxesData.data.boxes
        val searchPadding = 150;
        Log.i("TEST BOXES RECT DATA", boxesRectData.size.toString())
        boxesRectData.forEach { boxData ->
            val temp_x = (boxData.x * w_ratio).toInt()
            val temp_y = (boxData.y * h_ratio).toInt()
            val temp_w = (boxData.w * w_ratio).toInt()
            val temp_h = temp_w
            val testingRect = Rect(temp_x, temp_y, temp_w, temp_h) // Only for testing

            val adjusted_x = if (temp_x - searchPadding > 0) (temp_x - searchPadding) else 0
            val adjusted_y = if (temp_y - searchPadding > 0) (temp_y - searchPadding) else 0

            val end_x = temp_x + temp_w + searchPadding
            val adjusted_w = if (end_x > w) (w - adjusted_x) else (end_x - adjusted_x)
            val end_y = temp_y + temp_h + searchPadding
            val adjusted_h = if (end_y > h) (h - adjusted_y) else (end_y - adjusted_y)

            val searchingRect: Rect = Rect(adjusted_x, adjusted_y, adjusted_w, adjusted_h)
//            boxesContainer.add(testingRect)

            // Crop the searching area
            val searchingMat: Mat = Mat(processedMat, searchingRect)
            val boxes = imgProcessor.detectBoxes(searchingMat)

            // All-in
            boxes.forEach { rect ->
                val adjustingRect : Rect = Rect((rect.x + adjusted_x), (rect.y + adjusted_y), rect.width, rect.height)
                boxesContainer.add(adjustingRect)
            }
        }

        // Eliminate Redundant Boxes
        val similarityCleansedBoxesContainer = eliminateRedundantBoxes(boxesContainer)
        // Eliminate y_level isolated Boxes
        val isolatedCleansedBoxesContainer = eliminateIsolatedBoxes(similarityCleansedBoxesContainer)
        // Eliminate inside boxes
        val insiderCleansedBoxesContainer = eliminateInsideBoxes(isolatedCleansedBoxesContainer)

        return insiderCleansedBoxesContainer
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
        val boxesContainer = mutableListOf<Rect>()
        boxes.forEachIndexed { checkingIdx, checkingRect ->
            var foundInside = false;
            boxes.forEachIndexed { iteratingIdx, iteratingRect ->
                if (checkingIdx != iteratingIdx){
                    var horizontalInside = (checkingRect.x >= iteratingRect.x) && ((checkingRect.x + checkingRect.width) <= (iteratingRect.x + iteratingRect.width))
                    var verticalInside = (checkingRect.y >= iteratingRect.y) && ((checkingRect.y + checkingRect.height) <= (iteratingRect.y + iteratingRect.height))
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