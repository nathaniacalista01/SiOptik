package com.sioptik.main.processing_result.json_parser.builder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.sioptik.main.R
import com.sioptik.main.image_processing_integration.JsonTemplate

object UiComponentBuilder {
    fun buildUi(jsonTemplate: JsonTemplate, context: Context): View {
        val rootView = LayoutInflater.from(context).inflate(R.layout.layout_json, null, false)
        val apriltagIdTextView = rootView.findViewById<TextView>(R.id.aprilTagIdTextView)
        val apriltagIdText = "Apriltag ID: ${jsonTemplate.apriltagId}"
        apriltagIdTextView.text = apriltagIdText

        val container = rootView.findViewById<LinearLayout>(R.id.resultsContainer)

        for (field in jsonTemplate.fieldNames) {
            addFieldView(field, jsonTemplate.get(field), container, context)
        }

        return rootView
    }

    private fun addFieldView(field: String, value: Int, container: LinearLayout, context: Context) {
        val titleTextView = LayoutInflater.from(context).inflate(R.layout.field_title_text_view, container, false) as TextView
        titleTextView.text = field

        val valueTextView = LayoutInflater.from(context).inflate(R.layout.field_value_text_view, container, false) as TextView
        valueTextView.text = value.toString()

        container.addView(titleTextView)
        container.addView(valueTextView)
    }
}