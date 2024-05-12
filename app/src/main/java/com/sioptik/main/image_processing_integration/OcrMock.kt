package com.sioptik.main.image_processing_integration

import android.content.Context
import android.graphics.Bitmap
import com.sioptik.main.tesseract.TesseractHelper
import kotlin.random.Random

class OcrMock(private val context: Context) {

    private val tesseractHelper : TesseractHelper = TesseractHelper();
    fun detect(bitmap: List<Bitmap>, apriltagId: Int): JsonTemplate {
        val jsonTemplate = JsonTemplateFactory(context).jsonTemplate(apriltagId)
        var currentIndex = 0;
        for (field in jsonTemplate.fieldNames) {
            val numOfBoxes = jsonTemplate.getBoxes(field)  // Number of boxes for the current field
            var digits = ""  // Initialize an empty string to concatenate digits

            // Process each box for the current field
            for (boxIndex in currentIndex until currentIndex + numOfBoxes) {
                if (boxIndex < bitmap.size) {
                    val digit = tesseractHelper.recognizeDigits(bitmap[boxIndex])  // Recognize digits from each box
                    digits += digit  // Concatenate the digit to the digits string
                } else {
                    // Handle the case where the bitmap list does not have enough entries
                    break
                }
            }

            // Convert concatenated digits string to an integer
            val number = digits.toIntOrNull() ?: 0  // Converts the digits string to an integer, defaulting to 0 if conversion fails

            // Store the integer value in the JSON template
            jsonTemplate.entry(field, number)

            // Update currentIndex to the next set of boxes
            currentIndex += numOfBoxes
        }
        return jsonTemplate
    }
}