package com.alish10e.img3ddxf

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF061018)
private val Surface = Color(0xFF0B1D2A)
private val SurfaceRaised = Color(0xFF102B3D)
private val Line = Color(0xFF1B3C50)
private val Cyan = Color(0xFF5CE1CE)
private val CyanSoft = Color(0xFFB6FFF2)
private val White = Color(0xFFF3FAFD)
private val Muted = Color(0xFF8FAABC)
private val Warning = Color(0xFFFFC86B)

@Composable
fun NativeStudioScreen(
    bitmap: Bitmap?, map: HeightMap?, settings: ReliefSettings, processing: Boolean, error: String?,
    onPick: () -> Unit, onCamera: () -> Unit, onSettings: (ReliefSettings) -> Unit,
    onExport: (HeightMap, ExportFormat) -> Unit, onClear: () -> Unit, onDismissError: () -> Unit
) {
    val selectedPreset = remember(settings.depth, settings.contrast, settings.edgeBoost) {
        when { settings.depth <= 9f -> "دقيق"; settings.depth >= 17f -> "بارز"; else -> "متوازن" }
    }
    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Surface, primary = Cyan, onPrimary = Ink, onBackground = White, onSurface = White, error = Color(0xFFFF8A8A))) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Box(Modifier.fillMaxSize().background(Ink)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppHeader(bitmap != null, onClear)
                    WorkflowSteps(bitmap != null, map != null, processing)
                    PreviewCard(bitmap, map, settings, processing)
                    SourceActions(onPick, onCamera)
                    ProcessingCard(settings, selectedPreset, onSettings)
                    ExportCard(map, onExport)
                    PrivacyNote()
                    Spacer(Modifier.height(8.dp))
                }
                if (error != null) ErrorBanner(error, onDismissError)
            }
        }
    }
}

@Composable private fun AppHeader(hasImage: Boolean, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("VISION LAB  /  ANDROID", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Image to 3D", color = White, fontSize = 31.sp, fontWeight = FontWeight.Black)
            Text("حوّل الصور إلى نماذج بروز جاهزة للتصنيع", color = Muted, fontSize = 13.sp)
        }
        if (hasImage) TextButton(onClick = onClear) { Text("مشروع جديد", color = Cyan, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun WorkflowSteps(hasImage: Boolean, hasMap: Boolean, processing: Boolean) {
    val active = when { processing -> 1; hasMap -> 3; hasImage -> 2; else -> 1 }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(14.dp)).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Step("01", "مصدر", active >= 1); StepLine(active >= 2); Step("02", "تحليل", active >= 2); StepLine(active >= 3); Step("03", "تصدير", active >= 3)
    }
}
@Composable private fun Step(number: String, label: String, active: Boolean) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(number, color = if (active) Cyan else Muted, fontWeight = FontWeight.Black, fontSize = 12.sp); Text(label, color = if (active) White else Muted, fontSize = 10.sp) } }
@Composable private fun StepLine(active: Boolean) { Box(Modifier.padding(top = 7.dp).width(36.dp).height(2.dp).background(if (active) Cyan else Line)) }

@Composable private fun PreviewCard(bitmap: Bitmap?, map: HeightMap?, settings: ReliefSettings, processing: Boolean) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(22.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("المعاينة الحية", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); Text(if (map != null) "خريطة ارتفاعات • isometric mesh" else "أدخل صورة واضحة للبدء", color = Muted, fontSize = 11.sp) }
            StatusPill(if (processing) "جارٍ التحليل" else if (map != null) "جاهز للتصدير" else "بانتظار صورة", processing)
        }
        Box(Modifier.fillMaxWidth().height(266.dp).padding(top = 12.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF07131C)), contentAlignment = Alignment.Center) {
            bitmap?.let { Image(it.asImageBitmap(), "الصورة المصدر", Modifier.fillMaxSize(), alpha = if (map == null) .65f else .16f) }
            Canvas(Modifier.fillMaxSize().padding(18.dp)) { if (map != null) drawRelief(map) else drawGrid() }
            if (processing) Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = Cyan, strokeWidth = 3.dp); Spacer(Modifier.height(9.dp)); Text("تحليل الإضاءة والحواف...", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            else if (map == null) Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("استيراد صورة", color = White, fontWeight = FontWeight.Bold); Text("PNG  •  JPG  •  WEBP", color = Muted, fontSize = 10.sp) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) { Metric(map?.let { "${it.width}×${it.height}" } ?: "—", "شبكة الارتفاع"); Metric(if (map != null) "${settings.depth.toInt()} mm" else "—", "أقصى عمق"); Metric(map?.let { "${(it.edgeEnergy * 100).toInt()}%" } ?: "—", "تفاصيل الحواف") }
    }
}
@Composable private fun StatusPill(text: String, busy: Boolean) { Surface(color = if (busy) Color(0xFF44351D) else Color(0xFF10352F), shape = RoundedCornerShape(99.dp)) { Text("●  $text", color = if (busy) Warning else CyanSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) } }
@Composable private fun Metric(value: String, label: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = CyanSoft, fontWeight = FontWeight.Black, fontSize = 14.sp); Text(label, color = Muted, fontSize = 10.sp) } }

@Composable private fun SourceActions(onPick: () -> Unit, onCamera: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onPick, Modifier.weight(1.35f).height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) { Text("استيراد صورة", fontWeight = FontWeight.Black) }
        OutlinedButton(onClick = onCamera, Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Line), colors = ButtonDefaults.outlinedButtonColors(contentColor = White)) { Text("التقاط بالكاميرا", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
    }
}

@Composable private fun ProcessingCard(settings: ReliefSettings, preset: String, onSettings: (ReliefSettings) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("تحسين الرؤية الحاسوبية", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp); Text("إضاءة → تباين → حواف → خريطة ارتفاع", color = Muted, fontSize = 11.sp) }; Text("LOCAL CV", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("دقيق" to 8f, "متوازن" to 12f, "بارز" to 18f).forEach { (name, depth) -> Preset(name, depth, name == preset) { onSettings(settings.copy(depth = depth)) } } }
        SliderRow("عمق البروز", "${settings.depth.toInt()} mm", settings.depth, 2f..24f) { onSettings(settings.copy(depth = it)) }
        SliderRow("تباين الإضاءة", "${"%.1f".format(settings.contrast)}×", settings.contrast, .5f..2f) { onSettings(settings.copy(contrast = it)) }
        SliderRow("تفاصيل الحواف", "${(settings.edgeBoost * 100).toInt()}%", settings.edgeBoost, 0f..1f) { onSettings(settings.copy(edgeBoost = it)) }
    }
}
@Composable private fun Preset(name: String, depth: Float, selected: Boolean, onClick: () -> Unit) { Surface(Modifier.width(103.dp).clickable(onClick = onClick), color = if (selected) Color(0xFF164C4B) else SurfaceRaised, shape = RoundedCornerShape(11.dp), border = if (selected) BorderStroke(1.dp, Cyan) else null) { Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(name, color = if (selected) CyanSoft else White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${depth.toInt()} mm", color = Muted, fontSize = 10.sp) } } }
@Composable private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) { Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted, fontSize = 12.sp); Text(value, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp) }; Slider(value = current, onValueChange = onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan, inactiveTrackColor = Line)) } }

@Composable private fun ExportCard(map: HeightMap?, onExport: (HeightMap, ExportFormat) -> Unit) {
    var format = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(ExportFormat.DXF) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF0F3A42), Color(0xFF102737)))).padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text("تصدير ملف التصنيع", color = White, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Text("اختر الصيغة المناسبة لسير عمل CAD أو النمذجة ثلاثية الأبعاد", color = CyanSoft, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ExportOption("DXF", "CAD / CNC", format.value == ExportFormat.DXF) { format.value = ExportFormat.DXF }; ExportOption("OBJ", "3D / Blender", format.value == ExportFormat.OBJ) { format.value = ExportFormat.OBJ } }
        Button(onClick = { map?.let { onExport(it, format.value) } }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = map != null, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = CyanSoft, contentColor = Ink)) { Text(if (map != null) "تصدير ${format.value.extension.uppercase()} الآن" else "استورد صورة لتفعيل التصدير", fontWeight = FontWeight.Black) }
    }
}
@Composable private fun ExportOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) { Surface(Modifier.width(125.dp).clickable(onClick = onClick), color = if (selected) Color(0xFF1C5552) else Color(0xFF173244), shape = RoundedCornerShape(11.dp), border = if (selected) BorderStroke(1.dp, Cyan) else null) { Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color = if (selected) CyanSoft else White, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 10.sp) } } }

@Composable private fun PrivacyNote() { Text("✓ المعالجة محلية بالكامل  •  لا يتم رفع صورك  •  جاهز للعمل دون اتصال", Modifier.fillMaxWidth(), color = Color(0xFF648294), fontSize = 10.sp, textAlign = TextAlign.Center) }
@Composable private fun ErrorBanner(message: String, onDismiss: () -> Unit) { Surface(Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), color = Color(0xFF562C35), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f), color = White, fontSize = 12.sp); TextButton(onClick = onDismiss) { Text("إغلاق", color = CyanSoft) } } } }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid() { for (x in 0..16) drawLine(Line.copy(alpha = .5f), Offset(x * size.width / 16, 0f), Offset(x * size.width / 16, size.height)); for (y in 0..12) drawLine(Line.copy(alpha = .5f), Offset(0f, y * size.height / 12), Offset(size.width, y * size.height / 12)) }
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRelief(map: HeightMap) { val cellW = size.width / (map.width - 1); val cellH = size.height / (map.height - 1); for (y in 0 until map.height - 1) for (x in 0 until map.width - 1) { val z = (map.values[y * map.width + x] + map.values[y * map.width + x + 1] + map.values[(y + 1) * map.width + x] + map.values[(y + 1) * map.width + x + 1]) / 4f; val ox = z * 25f; val oy = z * 14f; val p = Path().apply { moveTo(x * cellW + ox, y * cellH - oy); lineTo((x + 1) * cellW + ox, y * cellH - oy); lineTo((x + 1) * cellW + ox, (y + 1) * cellH - oy); lineTo(x * cellW + ox, (y + 1) * cellH - oy); close() }; drawPath(p, Cyan.copy(alpha = .12f + z * .28f)); drawPath(p, CyanSoft.copy(alpha = .15f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = .65f)) } }

enum class ExportFormat(val extension: String) { DXF("dxf"), OBJ("obj") }
