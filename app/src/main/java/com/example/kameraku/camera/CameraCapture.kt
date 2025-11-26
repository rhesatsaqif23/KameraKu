package com.example.kameraku.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale

fun bindWithImageCapture(
    provider: ProcessCameraProvider,
    owner: LifecycleOwner,
    preview: Preview,
    selector: CameraSelector,
    previewView: PreviewView,
    qualityMode: Boolean = false
): ImageCapture {

    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
    logOrientation("Bind ImageCapture", rotation)

    val ic = ImageCapture.Builder()
        .setCaptureMode(
            if (qualityMode)
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            else
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        )
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .setTargetRotation(rotation)
        .build()

    provider.unbindAll()
    provider.bindToLifecycle(owner, selector, preview, ic)

    return ic
}

fun outputOptions(ctx: Context, name: String): ImageCapture.OutputFileOptions {
    val v = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KameraKu")
    }

    val r = ctx.contentResolver
    val uri = r.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!

    return ImageCapture.OutputFileOptions.Builder(r, uri, v).build()
}

fun takePhoto(
    context: Context,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    onSaved: (Uri?) -> Unit
) {
    // start stopwatch
    val startTime = System.currentTimeMillis()

    // update rotasi EXIF sebelum mengambil foto
    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
    imageCapture.targetRotation = rotation
    logOrientation("Capture Rotation", rotation)

    // nama file
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .format(System.currentTimeMillis())

    // lokasi penyimpanan MediaStore
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/KameraKu"
        )
    }

    // build output
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        .build()

    // ambil foto
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                // hitung waktu
                val duration = System.currentTimeMillis() - startTime
                Log.d("TEST_CAPTURE", "Capture time: $duration ms")

                val uri = outputFileResults.savedUri
                if (uri != null) {

                    // ukuran file
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    var size = 0L
                    cursor?.use {
                        val idx = it.getColumnIndex(MediaStore.Images.Media.SIZE)
                        if (idx != -1 && it.moveToFirst()) {
                            size = it.getLong(idx)
                        }
                    }
                    Log.d("TEST_CAPTURE", "File size: $size bytes")

                    // EXIF
                    logExifOrientation(context, uri)
                }

                onSaved(uri)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onSaved(null)
            }
        }
    )
}

