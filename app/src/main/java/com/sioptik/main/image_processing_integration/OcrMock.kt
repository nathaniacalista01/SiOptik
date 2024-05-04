package com.sioptik.main.image_processing_integration

import android.content.Context
import android.graphics.Bitmap
import kotlin.random.Random

class OcrMock(private val context: Context) {
    fun detect(bitmap: Bitmap?, apriltagId: Int): JsonTemplate {
        val jsonTemplate = JsonTemplateFactory(context).jsonTemplate(apriltagId)
        for (field in jsonTemplate.fieldNames) {
            jsonTemplate.entry(field, Random.nextInt(1000))
        }
        return jsonTemplate
    }
}