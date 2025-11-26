package com.example.kameraku.camera

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine

// bind preview ke lifecycle + pakai selector yang dikirim
suspend fun bindPreview(
    context: Context,
    owner: LifecycleOwner,
    view: PreviewView,
    selector: CameraSelector
): Pair<Preview, Camera> {

    // dapatkan CameraProvider secara suspend
    val provider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { cont.resume(future.get()) {} },
            ContextCompat.getMainExecutor(context)
        )
    }

    // build preview dan hubungkan ke surface
    val preview = Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .build()
        .apply { setSurfaceProvider(view.surfaceProvider) }

    // unbind semua sebelum bind ulang
    provider.unbindAll()

    // bind ke lifecycle sesuai selector (front/back)
    val camera = provider.bindToLifecycle(owner, selector, preview)

    return preview to camera
}
