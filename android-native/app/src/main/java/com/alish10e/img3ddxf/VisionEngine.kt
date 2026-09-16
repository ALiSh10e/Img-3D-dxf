package com.alish10e.img3ddxf

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class ReliefSettings(val depth: Float = 12f, val contrast: Float = 1.15f, val smoothing: Int = 1, val edgeBoost: Float = .55f)
data class HeightMap(val width: Int, val height: Int, val values: FloatArray, val edgeEnergy: Float)

object VisionEngine {
    fun buildHeightMap(source: Bitmap, settings: ReliefSettings, resolution: Int = 32): HeightMap {
        val gridW = min(resolution, max(12, source.width))
        val gridH = min(resolution, max(12, (source.height.toFloat() / source.width * gridW).toInt()))
        fun sample(x: Int, y: Int): Float {
            val px = (x.toFloat() / (gridW - 1) * (source.width - 1)).toInt().coerceIn(0, source.width - 1)
            val py = (y.toFloat() / (gridH - 1) * (source.height - 1)).toInt().coerceIn(0, source.height - 1)
            val c = source.getPixel(px, py)
            return (0.2126f * (c shr 16 and 255) + 0.7152f * (c shr 8 and 255) + 0.0722f * (c and 255)) / 255f
        }
        val raw = FloatArray(gridW * gridH)
        for (y in 0 until gridH) for (x in 0 until gridW) {
            val center = sample(x, y)
            val gradient = hypot(sample(min(gridW - 1, x + 1), y) - sample(max(0, x - 1), y), sample(x, min(gridH - 1, y + 1)) - sample(x, max(0, y - 1)))
            val shaped = center.coerceIn(0f, 1f).pow(1f / settings.contrast.coerceAtLeast(.25f))
            raw[y * gridW + x] = shaped * (1f - settings.edgeBoost * .35f) + gradient * settings.edgeBoost
        }
        val values = FloatArray(raw.size)
        for (i in raw.indices) {
            val x = i % gridW; val y = i / gridW
            var total = 0f; var count = 0
            for (oy in -settings.smoothing..settings.smoothing) for (ox in -settings.smoothing..settings.smoothing) {
                val nx = x + ox; val ny = y + oy
                if (nx in 0 until gridW && ny in 0 until gridH) { total += raw[ny * gridW + nx]; count++ }
            }
            values[i] = if (settings.smoothing == 0) raw[i] else total / count
        }
        val low = values.minOrNull() ?: 0f; val high = values.maxOrNull() ?: 1f
        for (i in values.indices) values[i] = ((values[i] - low) / (high - low).coerceAtLeast(.0001f)).coerceIn(0f, 1f)
        var energy = 0f
        for (i in values.indices) { val x = i % gridW; val y = i / gridW; if (x > 0) energy += abs(values[i] - values[i - 1]); if (y > 0) energy += abs(values[i] - values[i - gridW]) }
        return HeightMap(gridW, gridH, values, energy / values.size)
    }
}
