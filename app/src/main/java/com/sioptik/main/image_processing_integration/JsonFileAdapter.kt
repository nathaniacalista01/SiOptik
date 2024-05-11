package com.sioptik.main.image_processing_integration

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class JsonFileAdapter {
    fun saveJsonFile(jsonTemplate: JsonTemplate, context: Context): Uri {
        val dir = File(context.filesDir, "result-json")
        if (!dir.exists()) {
            dir.mkdir()
        }

        val fileName = generateFileName()
        val file = File(dir, fileName)
        val jsonString = jsonTemplate.toString()
        val fileOutputStream = file.outputStream()
        fileOutputStream.use {
            it.write(jsonString.toByteArray())
        }
        return file.toUri()
    }

    fun readJsonFile(fileName: String, context: Context): JsonTemplate? {
        val dir = File(context.filesDir, "result-json")
        if (!dir.exists()) {
            return null
        }

        val file = File(dir, fileName)
        if (!file.exists() || !file.isFile) {
            return null
        }

        val gsonBuilder = GsonBuilder().create()
        val type = object: TypeToken<LinkedHashMap<String, Int>>(){}.type
        val jsonString = file.readText(Charsets.UTF_8)
        val result = gsonBuilder.fromJson<LinkedHashMap<String, Int>>(jsonString, type)
        val apriltagId = result["apriltagId"] ?: throw Exception("apriltagId field not found in json file")

        val jsonTemplate = JsonTemplateFactory(context).jsonTemplate(apriltagId)
        for (entry in result) {
            if (entry.key != "apriltagId") {
                jsonTemplate.entry(entry.key, entry.value)
            }
        }

        return jsonTemplate
    }

    fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        return "$currentTime.json"
    }
}