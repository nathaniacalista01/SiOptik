package com.sioptik.main.processing_result

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sioptik.main.processing_result.json_parser.builder.UiComponentBuilder

class DynamicContentFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val jsonTemplate = viewModel.jsonTemplate
        return jsonTemplate?.let { UiComponentBuilder.buildUi(jsonTemplate, requireContext()) }
    }
}
