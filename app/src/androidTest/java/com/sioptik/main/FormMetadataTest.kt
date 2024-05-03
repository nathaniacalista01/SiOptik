package com.sioptik.main

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.sioptik.main.image_processing_integration.FormMetadataHolder
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class FormMetadataTest {
    @Test
    fun print() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val formMetadataHolder = FormMetadataHolder(context)
        formMetadataHolder.print()
    }

    @Test
    fun getFieldNames() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val formMetadataHolder = FormMetadataHolder(context)
        formMetadataHolder.getFieldNames(101)?.forEach {
            println(it)
        }
    }

}