package com.alish10e.img3ddxf

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Image3DApp() }
    }

    @Composable
    private fun Image3DApp() {
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var settings by remember { mutableStateOf(ReliefSettings()) }
        var map by remember { mutableStateOf<HeightMap?>(null) }
        val importImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
                contentResolver.openInputStream(it)?.use { stream ->
                    bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    map = bitmap?.let { image -> VisionEngine.buildHeightMap(image, settings) }
                }
            }
        }
        val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { shot -> shot?.let { bitmap = it; map = VisionEngine.buildHeightMap(it, settings) } }
        val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) camera.launch(null) }
        NativeStudioScreen(bitmap, map, settings, onPick = { importImage.launch(arrayOf("image/jpeg", "image/png", "image/webp")) }, onCamera = { if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(null) else requestCamera.launch(Manifest.permission.CAMERA) }, onSettings = { next -> settings = next; bitmap?.let { map = VisionEngine.buildHeightMap(it, next) } }, onExport = { current ->
            val file = File(cacheDir, "exports/relief-${System.currentTimeMillis()}.dxf").apply { parentFile?.mkdirs(); writeText(DxfExporter.build(current, settings)) }
            val uri = FileProvider.getUriForFile(this, "com.alish10e.img3ddxf.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/dxf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "تصدير نموذج البروز"))
        })
    }
}
