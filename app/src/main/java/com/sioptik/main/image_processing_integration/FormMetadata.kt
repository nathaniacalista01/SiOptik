package com.sioptik.main.image_processing_integration

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sioptik.main.R
import java.io.InputStreamReader

data class FormInformation (
    val fieldName: String,
    val boxes: Int
)

typealias FormMetadata = LinkedHashMap<String, List<FormInformation>>

class FormMetadataHolder(private val context: Context) {
    private val metadata: FormMetadata get() {
        if (_metadata == null) {
            val gson = Gson();
            val inputStream = context.resources.openRawResource(R.raw.metadata)
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<FormMetadata>() {}.type
            _metadata = gson.fromJson(reader, type)
            reader.close()
        }

        return _metadata!!
    }

    fun print() {
        metadata.forEach { (formId, formInfos) ->
            println("Form ID: $formId")
            formInfos.forEach {
                println("Field name: ${it.fieldName}, Boxes: ${it.boxes}")
            }
        }
    }

    fun getFieldNames(apriltagId: Int): Array<String>? {
        return metadata[apriltagId.toString()]?.map {
            it.fieldName
        }?.toTypedArray()
    }
    companion object {
        private var _metadata: FormMetadata? = null;
    }
}