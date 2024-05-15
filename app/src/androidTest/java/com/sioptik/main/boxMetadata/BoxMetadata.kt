package com.sioptik.main.boxMetadata

data class BoxMetadata(
    val w_ref: Int,
    val h_ref: Int,
    val data: FormBoxData
)

data class FormBoxData(
    val original_file_name: String,
    val april_tag: String,
    val num_of_boxes: Int,
    val boxes: List<BoxData>
)

data class BoxData(
    val id: Int,
    val x: Int,
    val y: Int,
    val w: Int
)
