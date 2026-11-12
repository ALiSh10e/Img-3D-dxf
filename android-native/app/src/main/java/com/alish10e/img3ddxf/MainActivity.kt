package com.alish10e.img3ddxf

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { Image3DApp() } }

    @Composable private fun Image3DApp() {
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var settings by remember { mutableStateOf(ReliefSettings()) }
        var map by remember { mutableStateOf<HeightMap?>(null) }
        var processing by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(bitmap, settings) {
            val source = bitmap ?: run { map = null; return@LaunchedEffect }
            processing = true
            map = withContext(Dispatchers.Default) { VisionEngine.buildHeightMap(source, settings) }
            processing = false
        }
        val importImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
            try { contentResolver.openInputStream(uri)?.use { bitmap = decodeOptimized(it.readBytes()) } ?: run { error = "تعذر قراءة الصورة" } }
            catch (_: Exception) { error = "تعذر استيراد الصورة. جرّب ملفًا بصيغة JPG أو PNG أو WEBP." }
        }
        val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { shot -> if (shot != null) bitmap = shot else error = "لم يتم التقاط صورة" }
        val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) camera.launch(null) else error = "يجب السماح بالكاميرا لالتقاط صورة" }
        NativeStudioScreen(bitmap, map, settings, processing, error,
            onPick = { importImage.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
            onCamera = { if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(null) else requestCamera.launch(Manifest.permission.CAMERA) },
            onSettings = { settings = it },
            onExport = { current, format -> export(current, settings, format) },
            onClear = { bitmap = null; map = null; error = null },
            onDismissError = { error = null })
    }

    private fun decodeOptimized(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val maxDimension = 1800
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 })
    }

    private fun export(map: HeightMap, settings: ReliefSettings, format: ExportFormat) {
        try {
            val file = File(cacheDir, "exports/relief-${System.currentTimeMillis()}.${format.extension}").apply {
                parentFile?.mkdirs(); writeText(if (format == ExportFormat.DXF) DxfExporter.build(map, settings) else ObjExporter.build(map, settings))
            }
            val uri = FileProvider.getUriForFile(this, "com.alish10e.img3ddxf.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = if (format == ExportFormat.DXF) "application/dxf" else "model/obj"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "تصدير نموذج البروز"))
        } catch (_: Exception) { }
    }
}
