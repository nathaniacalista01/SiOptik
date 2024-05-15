package com.sioptik.main;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.sioptik.main.apriltag.AprilTagDetection;
import com.sioptik.main.apriltag.AprilTagNative;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class BoxTest {
    @Test
    public void testImport() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        AssetManager assetManager = context.getAssets();
        String[] files = null;

        try {
            files = assetManager.list("");
            if (files != null) {
                for (String file : files) {
                    InputStream inputStream = assetManager.open(file);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                }
            }

        } catch (IOException e) {
            Log.e("TEST", "MASUK SINI");
            e.printStackTrace();
        }
    }


}
