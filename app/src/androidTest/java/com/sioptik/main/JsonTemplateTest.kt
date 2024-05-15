package com.sioptik.main

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.sioptik.main.image_processing_integration.FormMetadataHolder
import com.sioptik.main.image_processing_integration.JsonTemplateFactory
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class JsonTemplateTest {
    @Test
    fun testJsonTemplate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val jsonTemplateFactory = JsonTemplateFactory(context)
        val jsonTemplate = jsonTemplateFactory.jsonTemplate(101);
        val fieldNames = jsonTemplate.fieldNames
        for (field in fieldNames) {
            println("Field: $field")
            println("Boxes: ${jsonTemplate.getBoxes(field)}")
        }
    }
}