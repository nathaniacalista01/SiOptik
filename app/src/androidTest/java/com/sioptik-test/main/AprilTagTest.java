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


public class AprilTagTest {
    public static byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); // You can choose other formats like JPEG or PNG
        return stream.toByteArray();
    }

    public static byte [] getNV21(int inputWidth, int inputHeight, Bitmap scaled) {
        int[] argb = new int[inputWidth * inputHeight];

        // Ensure that the bitmap is in ARGB_8888 format
        scaled = scaled.copy(Bitmap.Config.ARGB_8888, true);

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);

        scaled.recycle();

        return yuv;
    }

    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        try {
            final int frameSize = width * height;

            int yIndex = 0;
            int uvIndex = frameSize;

            int a, R, G, B, Y, U, V;
            int index = 0;
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {

                    a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                    R = (argb[index] & 0xff0000) >> 16;
                    G = (argb[index] & 0xff00) >> 8;
                    B = (argb[index] & 0xff) >> 0;

                    // well known RGB to YUV algorithm
                    Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                    U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                    V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                    // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                    //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                    //    pixel AND every other scanline.
                    yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                    if (j % 2 == 0 && index % 2 == 0) {
                        yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                        yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                    }

                    index ++;
                }
            }

        } catch (IndexOutOfBoundsException e) {

        }

    }
    @Test
    public void testAprilTagInit() {
        AprilTagNative.apriltag_init("tag36h10", 0, 8, 0, 4);
        assertTrue(true);
    }

    public static Bitmap scaleDownBitmap(Bitmap originalBitmap, int maxWidth) {
        try {
            // Calculate the aspect ratio of the original bitmap
            float aspectRatio = (float) originalBitmap.getWidth() / originalBitmap.getHeight();

            // Create a ByteArrayOutputStream to write the scaled bitmap
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Compress the bitmap to the OutputStream with a quality of 100 (maximum quality)
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            // Create an input stream from the ByteArrayOutputStream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

            // Decode the input stream to get the bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();

            // Set inJustDecodeBounds to true to get the dimensions of the bitmap without loading it into memory
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            // Close the InputStream
            inputStream.close();

            // Calculate the sample size based on the maximum width
            options.inSampleSize = calculateSampleSize(options, maxWidth);

            // Decode the input stream with the calculated sample size
            options.inJustDecodeBounds = false;
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            Bitmap scaledBitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Return the scaled bitmap
            return scaledBitmap;
        } catch (IOException e) {
            // Handle any exceptions that may occur
            e.printStackTrace();
            return null; // Return null in case of an error
        }
    }

    private static int calculateSampleSize(BitmapFactory.Options options, int maxWidth) {
        int width = options.outWidth;
        int inSampleSize = 1;

        if (width > maxWidth) {
            // Calculate the sample size to reduce the width to fit within the maximum width
            inSampleSize = (int) Math.ceil((double) width / maxWidth);
        }
        return inSampleSize;
    }

    public static void writeToInternalStorage(String fileName, String content) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
                    bitmap = scaleDownBitmap(bitmap, 1600);
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    byte[] byteArray = getNV21(width, height, bitmap);
                    AprilTagNative.apriltag_init("tag36h10", 2, 1, 0, 4);
                    ArrayList<AprilTagDetection> detections = AprilTagNative.apriltag_detect_yuv(byteArray, width, height);
                    Log.i(file, String.valueOf(detections.get(0).id));
                    inputStream.close();
                }
            }

        } catch (IOException e) {
            Log.e("TEST", "MASUK SINI");
            e.printStackTrace();
        }
    }


}
