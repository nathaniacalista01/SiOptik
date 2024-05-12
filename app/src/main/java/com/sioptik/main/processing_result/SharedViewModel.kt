package com.sioptik.main.processing_result

import androidx.lifecycle.ViewModel
import com.sioptik.main.image_processing_integration.JsonTemplate

class SharedViewModel : ViewModel() {
    var jsonTemplate: JsonTemplate? = null
}
