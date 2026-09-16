package com.alish10e.img3ddxf

import android.graphics.Bitmap
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip

private val Ink = Color(0xFF07111A)
private val Panel = Color(0xFF0C1C2A)
private val Panel2 = Color(0xFF102435)
private val Mint = Color(0xFF55D6BE)
private val TextMain = Color(0xFFF6FBFF)
private val TextMuted = Color(0xFF8BA2B7)

@Composable
fun NativeStudioScreen(bitmap: Bitmap?, map: HeightMap?, settings: ReliefSettings, onPick: () -> Unit, onCamera: () -> Unit, onSettings: (ReliefSettings) -> Unit, onExport: (HeightMap) -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Mint, onPrimary = Ink, onBackground = TextMain, onSurface = TextMain)) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column { Text("VISION LAB / NATIVE ANDROID", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp); Text("Image to 3D", color = TextMain, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold); Text("حوّل الصورة إلى بروز قابل للتصنيع", color = TextMuted, fontSize = 14.sp) }
                    Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFF0E292E), modifier = Modifier.padding(top = 5.dp)) { Text("● محرك أصلي", color = Color(0xFF9DF5E2), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
                }
                ReliefPreview(bitmap, map, settings)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onPick, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink), shape = RoundedCornerShape(13.dp)) { Text("استيراد الصورة", fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = onCamera, modifier = Modifier.weight(.72f).height(52.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26485A)), shape = RoundedCornerShape(13.dp)) { Text("الكاميرا", fontWeight = FontWeight.Bold) }
                }
                Controls(settings, onSettings)
                Button(onClick = { map?.let(onExport) }, enabled = map != null, modifier = Modifier.fillMaxWidth().height(66.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7FFF1), contentColor = Ink), shape = RoundedCornerShape(15.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("تصدير النموذج إلى DXF", fontWeight = FontWeight.Black, fontSize = 15.sp); Text("شبكة 3D Face جاهزة لـ CAD / CNC", fontSize = 10.sp) } }
                Text("المعالجة تتم محليًا على الجهاز • لا يتم رفع صورك", color = Color(0xFF526B7C), fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable private fun ReliefPreview(bitmap: Bitmap?, map: HeightMap?, settings: ReliefSettings) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).border(1.dp, Color(0xFF1A3343), RoundedCornerShape(20.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("المعاينة ثلاثية الأبعاد", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp); Text("ISOMETRIC / RELIEF", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        Box(Modifier.fillMaxWidth().height(260.dp).padding(top = 10.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF09131D)), contentAlignment = Alignment.Center) {
            bitmap?.let { Image(it.asImageBitmap(), contentDescription = "الصورة المصدر", modifier = Modifier.fillMaxSize(), alpha = .25f) }
            Canvas(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) { map?.let { drawRelief(it) } ?: drawPlaceholder() }
            if (map == null) Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("ابدأ بصورة واضحة", color = TextMain, fontWeight = FontWeight.Bold); Text("صورة أمامية بإضاءة متوازنة تعطي نتيجة أفضل", color = TextMuted, fontSize = 12.sp) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceAround) { Stat(map?.let { "${it.width}×${it.height}" } ?: "32×32", "الدقة"); Stat("${settings.depth.toInt()} mm", "العمق"); Stat("${map?.let { (it.edgeEnergy * 100).toInt() } ?: 18}%", "الحواف") }
    }
}

@Composable private fun Stat(value: String, label: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Color(0xFFB9D8EC), fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(label, color = Color(0xFF668198), fontSize = 10.sp) } }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRelief(map: HeightMap) {
    val cellW = size.width / (map.width - 1); val cellH = size.height / (map.height - 1)
    for (y in 0 until map.height - 1) for (x in 0 until map.width - 1) {
        val z = (map.values[y * map.width + x] + map.values[y * map.width + x + 1] + map.values[(y + 1) * map.width + x] + map.values[(y + 1) * map.width + x + 1]) / 4f
        val ox = z * 28f; val oy = z * 15f
        val p = Path().apply { moveTo(x * cellW + ox, y * cellH - oy); lineTo((x + 1) * cellW + ox, y * cellH - oy); lineTo((x + 1) * cellW + ox, (y + 1) * cellH - oy); lineTo(x * cellW + ox, (y + 1) * cellH - oy); close() }
        drawPath(p, Color(0xFF55D6BE).copy(alpha = .16f + z * .26f), style = androidx.compose.ui.graphics.drawscope.Fill); drawPath(p, Color(0xFFB7FFF1).copy(alpha = .12f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = .7f))
    }
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlaceholder() { for (x in 0..18) drawLine(Color(0xFF55D6BE).copy(alpha = .1f), Offset(x * size.width / 18, 0f), Offset(x * size.width / 18, size.height)); for (y in 0..18) drawLine(Color(0xFF55D6BE).copy(alpha = .1f), Offset(0f, y * size.height / 18), Offset(size.width, y * size.height / 18)) }

@Composable private fun Controls(settings: ReliefSettings, onSettings: (ReliefSettings) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF0B1926)).border(1.dp, Color(0xFF1A3343), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("معالجة الرؤية الحاسوبية", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp); Text("إضاءة → حواف → شبكة ارتفاعات", color = TextMuted, fontSize = 12.sp) }; Text("NATIVE CV", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("ناعم" to 8f, "متوازن" to 12f, "حاد" to 18f).forEach { (label, depth) -> Surface(Modifier.width(100.dp).clickable { onSettings(settings.copy(depth = depth)) }, color = Panel2, shape = RoundedCornerShape(10.dp)) { Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = Color(0xFFC9D9E7), fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${depth.toInt()}mm", color = Color(0xFF6F9DB4), fontSize = 10.sp) } } } }
        SliderRow("عمق البروز", "${settings.depth.toInt()} mm", settings.depth, 2f..24f) { onSettings(settings.copy(depth = it)) }
        SliderRow("تباين الارتفاع", "${"%.1f".format(settings.contrast)}×", settings.contrast, .5f..2f) { onSettings(settings.copy(contrast = it)) }
        SliderRow("تقوية الحواف", "${(settings.edgeBoost * 100).toInt()}%", settings.edgeBoost, 0f..1f) { onSettings(settings.copy(edgeBoost = it)) }
    }
}
@Composable private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) { Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = TextMuted, fontSize = 12.sp); Text(value, color = Mint, fontWeight = FontWeight.Bold, fontSize = 12.sp) }; Slider(value = current, onValueChange = onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint, inactiveTrackColor = Color(0xFF1D3547))) } }
