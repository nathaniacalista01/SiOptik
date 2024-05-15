package com.sioptik.main.image_processing_integration

import android.content.Context

class JsonTemplateFactory(private val context: Context) {
    private val formMetadataHolder = FormMetadataHolder(context)
    fun jsonTemplate(apriltagId: Int): JsonTemplate {
        val formInformations = formMetadataHolder.getAllFormInformations(apriltagId)
            ?: throw Exception("Form with apriltag ID $apriltagId not found in metadata.json")
        return JsonTemplate(formInformations, apriltagId)
    }

}