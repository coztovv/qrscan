package com.abusegpt.qrscan.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abusegpt.qrscan.util.BarcodeDecoder
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.math.min

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onDetected: (String) -> Unit
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (granted) {
            CameraScanner(onBack = onBack, onDetected = onDetected)
        } else {
            PermissionDenied(
                onBack = onBack,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        }
    }
}

@Composable
private fun CameraScanner(
    onBack: () -> Unit,
    onDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val currentOnDetected by rememberUpdatedState(onDetected)
    val handled = remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }
    var galleryEmpty by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(analysisExecutor) { proxy ->
            val media = proxy.image
            if (media == null || handled.value) {
                proxy.close()
                return@setAnalyzer
            }
            val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
            scanner.process(input)
                .addOnSuccessListener { codes ->
                    if (handled.value) return@addOnSuccessListener
                    val value = codes
                        .firstOrNull { !it.rawValue.isNullOrBlank() }
                        ?.rawValue
                        ?.trim()
                    if (!value.isNullOrBlank()) {
                        handled.value = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnDetected(value)
                    }
                }
                .addOnCompleteListener { proxy.close() }
        }

        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.unbindAll()
                val bound = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                camera = bound
                hasTorch = bound.cameraInfo.hasFlashUnit()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { analysis.clearAnalyzer() }
            runCatching { providerFuture.get().unbindAll() }
            runCatching { scanner.close() }
            runCatching { analysisExecutor.shutdown() }
        }
    }

    LaunchedEffect(torchOn) {
        runCatching { camera?.cameraControl?.enableTorch(torchOn) }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            BarcodeDecoder.decode(context, uri) { values ->
                val value = values.firstOrNull()
                if (value.isNullOrBlank()) {
                    galleryEmpty = true
                } else {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDetected(value)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        ScannerOverlay()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundGlassButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "закрыть",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "сканирование",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.weight(1f))
            if (hasTorch) {
                RoundGlassButton(onClick = { torchOn = !torchOn }) {
                    Icon(
                        imageVector = if (torchOn) Icons.Rounded.FlashlightOff else Icons.Rounded.FlashlightOn,
                        contentDescription = "фонарик",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 34.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (galleryEmpty) "на картинке не нашлось кода" else "держите код в рамке",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = if (galleryEmpty) 1f else 0.78f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                onClick = {
                    galleryEmpty = false
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "из галереи",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black.copy(alpha = 0.35f),
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        modifier = modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun ScannerOverlay() {
    val transition = rememberInfiniteTransition(label = "scanline")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val frameFraction = 0.66f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = min(size.width, size.height) * frameFraction
            val left = (size.width - side) / 2f
            val top = (size.height - side) / 2f
            val right = left + side
            val bottom = top + side
            val corner = 34.dp.toPx()

            val hole = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(corner, corner)
                    )
                )
            }
            drawPath(hole, Color.Black.copy(alpha = 0.55f))

            val len = side * 0.13f
            val strokeWidth = 4.5.dp.toPx()
            val bracket = Color.White

            fun line(start: Offset, end: Offset) = drawLine(
                color = bracket,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            line(Offset(left, top + len), Offset(left, top))
            line(Offset(left, top), Offset(left + len, top))

            line(Offset(right - len, top), Offset(right, top))
            line(Offset(right, top), Offset(right, top + len))

            line(Offset(left, bottom - len), Offset(left, bottom))
            line(Offset(left, bottom), Offset(left + len, bottom))

            line(Offset(right - len, bottom), Offset(right, bottom))
            line(Offset(right, bottom), Offset(right, bottom - len))

            val lineY = top + side * (0.08f + 0.84f * progress)
            val brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.9f),
                    Color.Transparent
                ),
                startX = left,
                endX = right
            )
            drawLine(
                brush = brush,
                start = Offset(left + side * 0.05f, lineY),
                end = Offset(right - side * 0.05f, lineY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PermissionDenied(onBack: () -> Unit, onRequest: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp)
    ) {
        RoundGlassButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart),
            containerColor = scheme.surfaceContainerHigh
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "закрыть",
                tint = scheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.NoPhotography,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "нужна камера",
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "без доступа к камере сканирование не работает",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Button(onClick = onRequest, shape = RoundedCornerShape(50)) {
                Text("Разрешить доступ")
            }
        }
    }
}
