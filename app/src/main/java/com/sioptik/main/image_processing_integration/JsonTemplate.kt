package com.sioptik.main.image_processing_integration

import com.google.gson.GsonBuilder

class JsonTemplate(
    var formInformations: Array<FormInformation>,
    val apriltagId: Int
) {
    private val dictionary =  linkedMapOf<String, Int>()
    private val gsonBuilder = GsonBuilder().apply {
        setPrettyPrinting()
    };
    private val gson = gsonBuilder.create();

    val fieldNames get() = formInformations.map {
        it.fieldName
    }.toTypedArray()

    init {
        dictionary["apriltagId"] = apriltagId
    }

    fun entry(fieldName: String, value: Int) {
        if (!fieldNames.contains(fieldName)) {
            throw Exception("Invalid Field Name")
        }
        dictionary[fieldName] = value
    }

    fun get(fieldName: String): Int {
        if (!fieldNames.contains(fieldName)) {
            throw Exception("Invalid Field Name")
        }
        return dictionary[fieldName] ?: throw Exception("$fieldName value has not been inserted")
    }

    fun getBoxes(fieldName: String): Int {
        if (!fieldNames.contains(fieldName)) {
            throw Exception("Invalid Field Name")
        }
        
        return formInformations.first {
            it.fieldName == fieldName
        }.boxes
    }

    override fun toString(): String {
        return gson.toJson(dictionary)
    }
}
