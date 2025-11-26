package com.example.kameraku.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.QualitySelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// X.5.5 Orientasi & Rasio Aspek

// set rotasi EXIF
fun setImageCaptureRotation(
    imageCapture: ImageCapture,
    previewView: androidx.camera.view.PreviewView
) {
    imageCapture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
}

// pilih rasio aspek (4:3 / 16:9)
fun chooseAspectRatio(width: Int, height: Int): Int {
    val ratio = max(width, height).toDouble() / min(width, height)
    val diff43 = abs(ratio - (4.0 / 3.0))
    val diff169 = abs(ratio - (16.0 / 9.0))
    return if (diff43 <= diff169) AspectRatio.RATIO_4_3 else AspectRatio.RATIO_16_9
}

// build preview sesuai rasio
fun buildPreviewWithAspect(
    context: Context,
    previewView: androidx.camera.view.PreviewView
): Preview {

    val metrics = DisplayMetrics().also {
        previewView.display?.getRealMetrics(it)
    }

    val ar = chooseAspectRatio(metrics.widthPixels, metrics.heightPixels)

    return Preview.Builder()
        .setTargetAspectRatio(ar)
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }
}

// build ImageCapture sesuai rasio + rotasi
fun buildImageCaptureWithAspect(
    aspectRatio: Int,
    rotation: Int
): ImageCapture {
    return ImageCapture.Builder()
        .setTargetAspectRatio(aspectRatio)
        .setTargetRotation(rotation)
        .build()
}

// build ImageCapture sesuai rasio + rotasi
fun buildPreviewWithAspect(
    aspectRatio: Int,
    previewView: PreviewView
): Preview {
    return Preview.Builder()
        .setTargetAspectRatio(aspectRatio)
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }
}

// Logging orientasi untuk pengujian EXIF
fun logOrientation(tag: String, rotation: Int) {
    val label = when (rotation) {
        Surface.ROTATION_0 -> "0° Portrait"
        Surface.ROTATION_90 -> "90° Landscape Left"
        Surface.ROTATION_180 -> "180° Portrait Reverse"
        Surface.ROTATION_270 -> "270° Landscape Right"
        else -> "Unknown"
    }
    Log.d("ORIENT_TEST", "$tag = $label")
}

fun logExifOrientation(context: Context, uri: Uri) {
    val input = context.contentResolver.openInputStream(uri)
    val exif = androidx.exifinterface.media.ExifInterface(input!!)

    val ori = exif.getAttributeInt(
        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
        androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED
    )

    val label = when (ori) {
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> "ROTATE 90"
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> "ROTATE 180"
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> "ROTATE 270"
        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL -> "NORMAL"
        else -> "UNDEFINED"
    }

    Log.d("EXIF_TEST", "EXIF Orientation = $label")
}

@SuppressLint("ServiceCast")
fun observeRotation(
    context: Context,
    previewView: PreviewView,
    imageCapture: ImageCapture
) {
    val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}

        override fun onDisplayRemoved(displayId: Int) {}

        override fun onDisplayChanged(displayId: Int) {
            val display = previewView.display ?: return

            val rot = when (display.rotation) {
                Surface.ROTATION_0 -> Surface.ROTATION_0
                Surface.ROTATION_90 -> Surface.ROTATION_90
                Surface.ROTATION_180 -> Surface.ROTATION_180
                Surface.ROTATION_270 -> Surface.ROTATION_270
                else -> Surface.ROTATION_0
            }

            imageCapture.targetRotation = rot
            logOrientation("DisplayManager Rotation", rot)
        }
    }

    dm.registerDisplayListener(listener, null)
}

fun observeDeviceOrientationForUI(
    context: Context,
    onDegreesChanged: (Int) -> Unit
) {
    val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    dm.registerDisplayListener(object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) {}
        override fun onDisplayRemoved(id: Int) {}

        override fun onDisplayChanged(id: Int) {
            val display = dm.getDisplay(id) ?: return

            val degrees = when (display.rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            onDegreesChanged(degrees)
        }
    }, null)
}

// X.5.6 Flash & Switch Kamera

// torch ON/OFF
fun toggleTorch(camera: Camera, enable: Boolean) {
    camera.cameraControl.enableTorch(enable)
}

// flash mode ON / OFF / AUTO
fun setImageCaptureFlashMode(imageCapture: ImageCapture, mode: Int) {
    imageCapture.flashMode = mode
}

// ganti kamera depan / belakang
fun switchCamera(
    provider: ProcessCameraProvider,
    owner: LifecycleOwner,
    preview: Preview,
    imageCapture: ImageCapture?,
    useFront: Boolean
): Camera {

    val selector =
        if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA
        else CameraSelector.DEFAULT_BACK_CAMERA

    provider.unbindAll()

    return if (imageCapture != null)
        provider.bindToLifecycle(owner, selector, preview, imageCapture)
    else
        provider.bindToLifecycle(owner, selector, preview)
}

// X.5.7 VideoCapture (sekilas)

// build VideoCapture
fun buildVideoCapture(): VideoCapture<Recorder> {
    val recorder = Recorder.Builder()
        .setQualitySelector(
            QualitySelector.from(Quality.FHD)
        )
        .build()

    return VideoCapture.withOutput(recorder)
}

// mulai rekam video
fun startRecording(
    videoCapture: VideoCapture<Recorder>,
    file: File,
    context: Context,
    onEvent: (VideoRecordEvent) -> Unit
): Recording {

    val output = FileOutputOptions.Builder(file).build()

    val pending: PendingRecording = videoCapture.output
        .prepareRecording(context, output)

    return pending.start(ContextCompat.getMainExecutor(context)) { e ->
        onEvent(e) // callback event recording (Start/Finalize/Error)
    }
}

// stop rekam
fun stopRecording(recording: Recording) {
    recording.stop()
}
