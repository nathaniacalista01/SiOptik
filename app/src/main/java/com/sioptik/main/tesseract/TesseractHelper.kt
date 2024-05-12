package com.sioptik.main.tesseract

import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
class TesseractHelper {
    var tessBaseApi: TessBaseAPI? = null
    fun initTessBaseApi(dataPath: String, language: String) {
        tessBaseApi = TessBaseAPI()
        val initSuccess = tessBaseApi?.init(dataPath, language) ?: false
        if (!initSuccess) {
            tessBaseApi?.recycle();
            Log.e("Tesseract", "Could not initialize Tesseract API with language: $language")
        }
        tessBaseApi?.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
        tessBaseApi?.setVariable("tessedit_char_whitelist", "0123456789")
    }
    fun recognizeDigits(image: Bitmap): String? {
        tessBaseApi?.setImage(image)
        return tessBaseApi?.utF8Text
    }

    fun destroy() {
        tessBaseApi?.recycle()
    }
}