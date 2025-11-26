package com.example.kameraku.ui

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.kameraku.camera.*
import com.example.kameraku.camera.AspectOption
import com.example.kameraku.ui.component.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun CameraScreen() {

    var hasPermission by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lastImageUri by remember { mutableStateOf<Uri?>(null) }
    var isFront by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var aspectOption by remember { mutableStateOf(AspectOption.RATIO_4_3) }

    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    // UI ketika IZIN DITOLAK
    if (!hasPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Izin kamera tidak aktif. Izinkan akses kamera melalui Pengaturan untuk melanjutkan.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text("Buka Pengaturan")
                }
            }
        }
        return
    }

    // Orientation
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val rotation = config.orientation

    val isLeftLandscape = rotation == Configuration.ORIENTATION_LANDSCAPE &&
            context.display.rotation == Surface.ROTATION_270

    val isRightLandscape = rotation == Configuration.ORIENTATION_LANDSCAPE &&
            context.display.rotation == Surface.ROTATION_90

    Box(Modifier.fillMaxSize()) {

        CameraPreview { pv -> previewView = pv }

        // Bind kamera
        if (previewView != null) {

            LaunchedEffect(previewView, isFront, aspectOption) {
                owner.lifecycleScope.launch {

                    val selector = if (isFront)
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    else
                        CameraSelector.DEFAULT_BACK_CAMERA

                    val provider = ProcessCameraProvider.getInstance(context).get()
                    provider.unbindAll()

                    val rotationValue = previewView!!.display?.rotation ?: Surface.ROTATION_0

                    val preview = buildPreviewWithAspect(aspectOption.ratio, previewView!!)
                    val ic = buildImageCaptureWithAspect(aspectOption.ratio, rotationValue)

                    camera = provider.bindToLifecycle(owner, selector, preview, ic)
                    imageCapture = ic
                }
            }

            LaunchedEffect(imageCapture, previewView) {
                val ic = imageCapture ?: return@LaunchedEffect
                val pv = previewView ?: return@LaunchedEffect

                observeRotation(context, pv, ic)

                pv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    val rot = when (pv.display?.rotation) {
                        Surface.ROTATION_0 -> Surface.ROTATION_0
                        Surface.ROTATION_90 -> Surface.ROTATION_90
                        Surface.ROTATION_180 -> Surface.ROTATION_180
                        Surface.ROTATION_270 -> Surface.ROTATION_270
                        else -> Surface.ROTATION_0
                    }
                    ic.targetRotation = rot
                    logOrientation("Live Rotation Update", rot)
                }
            }
        }

        // Layout tombol
        when {

            // Landscape right
            isLandscape && isRightLandscape -> {

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 24.dp)
                        .align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlashTorchColumn(
                        torchOn = torchOn,
                        flashMode = flashMode,
                        onToggleTorch = {
                            torchOn = !torchOn
                            camera?.let { toggleTorch(it, torchOn) }
                        },
                        onCycleFlash = {
                            flashMode = nextFlash(flashMode)
                            imageCapture?.flashMode = flashMode
                        }
                    )

                    Spacer(Modifier.height(20.dp))

                    AspectRatioButton(
                        current = aspectOption,
                        onToggle = { aspectOption = it }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 24.dp)
                        .align(Alignment.CenterEnd),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Thumbnail(lastImageUri)
                    Spacer(Modifier.height(20.dp))
                    ShutterButton(imageCapture, previewView, context) { lastImageUri = it }
                    Spacer(Modifier.height(20.dp))
                    SwitchButton { isFront = !isFront }
                }
            }

            // Landscape left
            isLandscape && isLeftLandscape -> {

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 24.dp)
                        .align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Thumbnail(lastImageUri)
                    Spacer(Modifier.height(20.dp))
                    ShutterButton(imageCapture, previewView, context) { lastImageUri = it }
                    Spacer(Modifier.height(20.dp))
                    SwitchButton { isFront = !isFront }
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 24.dp)
                        .align(Alignment.CenterEnd),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlashTorchColumn(
                        torchOn = torchOn,
                        flashMode = flashMode,
                        onToggleTorch = {
                            torchOn = !torchOn
                            camera?.let { toggleTorch(it, torchOn) }
                        },
                        onCycleFlash = {
                            flashMode = nextFlash(flashMode)
                            imageCapture?.flashMode = flashMode
                        }
                    )

                    Spacer(Modifier.height(20.dp))

                    AspectRatioButton(
                        current = aspectOption,
                        onToggle = { aspectOption = it }
                    )
                }
            }

            // Portrait
            else -> {

                Row(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlashButton(
                        flashMode = flashMode,
                        onCycle = {
                            flashMode = nextFlash(flashMode)
                            imageCapture?.flashMode = flashMode
                        }
                    )

                    TorchButton(
                        torchOn = torchOn,
                        onToggle = {
                            torchOn = !torchOn
                            camera?.let { toggleTorch(it, torchOn) }
                        }
                    )

                    AspectRatioButton(
                        current = aspectOption,
                        onToggle = { aspectOption = it }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Thumbnail(lastImageUri)
                        ShutterButton(imageCapture, previewView, context) { lastImageUri = it }
                        SwitchButton { isFront = !isFront }
                    }
                }
            }
        }
    }
}
