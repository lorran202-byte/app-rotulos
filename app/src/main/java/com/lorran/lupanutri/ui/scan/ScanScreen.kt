@file:OptIn(ExperimentalMaterial3Api::class)

package com.lorran.lupanutri.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lorran.lupanutri.ui.components.CenterMessage
import com.lorran.lupanutri.ui.theme.Space
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private fun isValidCode(s: String) = s.length in 8..14 && s.all { it.isDigit() }

@Composable
fun ScanScreen(onCode: (String) -> Unit, onOpenProfile: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        askedOnce = true
    }
    LaunchedEffect(Unit) {
        if (!granted && !askedOnce) launcher.launch(Manifest.permission.CAMERA)
    }
    LifecycleResumeEffect(Unit) {
        granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }
    var showManual by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            CameraPreview(
                torchOn = torch,
                onBarcode = { code ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCode(code)
                },
            )
            ScanOverlay()
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                CenterMessage(
                    icon = Icons.Outlined.NoPhotography,
                    title = "Câmera desligada",
                    body = "Para ler o código de barras, o app precisa da câmera. Você também pode digitar o código.",
                    actionLabel = "Permitir câmera",
                    onAction = {
                        val activity = context as? Activity
                        val canAsk = !askedOnce || (activity != null &&
                            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA))
                        if (canAsk) {
                            launcher.launch(Manifest.permission.CAMERA)
                        } else {
                            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(i) }
                        }
                    },
                )
            }
        }

        // Topo
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = Space.lg, vertical = Space.lg),
        ) {
            Text("Lupa Nutri", style = MaterialTheme.typography.titleLarge, color = if (granted) Color.White else MaterialTheme.colorScheme.onBackground)
            Text(
                "Aponte para o código de barras do alimento",
                style = MaterialTheme.typography.bodyMedium,
                color = if (granted) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Base
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(Space.lg),
            horizontalArrangement = Arrangement.spacedBy(Space.md, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (granted) {
                FilledTonalIconButton(onClick = { torch = !torch }, modifier = Modifier.size(52.dp)) {
                    Icon(
                        if (torch) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                        contentDescription = if (torch) "Desligar lanterna" else "Ligar lanterna",
                    )
                }
            }
            FilledTonalButton(onClick = { showManual = true }, modifier = Modifier.height(52.dp)) {
                Icon(Icons.Outlined.Keyboard, null)
                Spacer(Modifier.width(Space.sm))
                Text("Digitar código")
            }
            OutlinedButton(onClick = onOpenProfile, modifier = Modifier.height(52.dp)) {
                Text("Meu perfil", color = if (granted) Color.White else MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showManual) {
        ManualCodeDialog(onDismiss = { showManual = false }, onConfirm = { showManual = false; onCode(it) })
    }
}

@Composable
private fun ScanOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 280.dp, height = 170.dp)
                .border(3.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp)),
        )
    }
}

@Composable
private fun ManualCodeDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val valid = isValidCode(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Digitar código de barras") },
        text = {
            Column {
                Text("Os números embaixo das barras (8 a 14 dígitos).", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(Space.md))
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(14) },
                    singleLine = true,
                    label = { Text("Código") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = text.isNotEmpty() && !valid,
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(text) }, enabled = valid) { Text("Consultar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(torchOn: Boolean, onBarcode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val currentOnBarcode by rememberUpdatedState(onBarcode)
    var camera by remember { mutableStateOf<Camera?>(null) }
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build()
        )
        val done = AtomicBoolean(false)
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media == null || done.get()) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    scanner.process(input)
                        .addOnSuccessListener { codes ->
                            val value = codes.mapNotNull { it.rawValue }.firstOrNull { isValidCode(it) }
                            if (value != null && done.compareAndSet(false, true)) currentOnBarcode(value)
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                p.unbindAll()
                camera = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                failed = true
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            done.set(true)
            provider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }

    LaunchedEffect(torchOn, camera) {
        camera?.let { if (it.cameraInfo.hasFlashUnit()) it.cameraControl.enableTorch(torchOn) }
    }

    if (failed) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            CenterMessage(
                icon = Icons.Outlined.NoPhotography,
                title = "Não deu para abrir a câmera",
                body = "Feche outros apps que usam a câmera ou digite o código manualmente.",
            )
        }
    } else {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}
