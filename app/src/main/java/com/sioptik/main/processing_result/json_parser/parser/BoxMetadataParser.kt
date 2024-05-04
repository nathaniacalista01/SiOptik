package com.sioptik.main.processing_result.json_parser.parser


import com.sioptik.main.processing_result.json_parser.model.BoxData
import com.sioptik.main.processing_result.json_parser.model.BoxMetadata
import com.sioptik.main.processing_result.json_parser.model.FormBoxData
import org.json.JSONObject

object BoxMetadataParser {
    fun parse(jsonString: String, aprilTag: String): BoxMetadata {
        val jsonObject = JSONObject(jsonString)
        val w_ref = jsonObject.getInt("w_ref")
        val h_ref = jsonObject.getInt("h_ref")

        val formData = jsonObject.getJSONObject(aprilTag)
        val file_name = formData.getString("original_file_name")
        val num_of_boxes = formData.getInt("num_of_boxes")

        val boxesArray = formData.getJSONArray("boxes")
        val boxesList = mutableListOf<BoxData>()

        for (i in 0 until boxesArray.length()) {
            val boxJsonObject = boxesArray.getJSONObject(i)
            val box = BoxData(
                id = boxJsonObject.getInt("id"),
                x = boxJsonObject.getInt("x"),
                y = boxJsonObject.getInt("y"),
                w = boxJsonObject.getInt("w")
            )
            boxesList.add(box)
        }

        val finalizedFormBoxData = FormBoxData (
            original_file_name = file_name,
            num_of_boxes = num_of_boxes,
            april_tag = aprilTag,
            boxes = boxesList,
        )

        return BoxMetadata(
            w_ref = w_ref,
            h_ref = h_ref,
            data = finalizedFormBoxData
        )
    }
}