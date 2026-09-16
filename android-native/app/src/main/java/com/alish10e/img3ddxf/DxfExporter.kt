package com.alish10e.img3ddxf

import java.util.Locale

object DxfExporter {
    fun build(map: HeightMap, settings: ReliefSettings, modelWidth: Float = 100f): String {
        val sx = modelWidth / (map.width - 1)
        val sy = modelWidth * map.height / (map.width - 1) / map.height
        val out = StringBuilder("0\nSECTION\n2\nENTITIES\n")
        fun face(points: List<FloatArray>) {
            out.append("0\n3DFACE\n8\nRELIEF\n")
            val groups = intArrayOf(10, 11, 12, 13)
            points.take(4).forEachIndexed { index, p -> out.append("${groups[index]}\n${fmt(p[0])}\n${groups[index] + 10}\n${fmt(p[1])}\n${groups[index] + 20}\n${fmt(p[2])}\n") }
        }
        fun point(x: Int, y: Int): FloatArray = floatArrayOf(x * sx, (map.height - 1 - y) * sy, map.values[y * map.width + x] * settings.depth)
        for (y in 0 until map.height - 1) for (x in 0 until map.width - 1) face(listOf(point(x, y), point(x + 1, y), point(x + 1, y + 1), point(x, y + 1)))
        out.append("0\nENDSEC\n0\nEOF\n")
        return out.toString()
    }
    private fun fmt(value: Float) = String.format(Locale.US, "%.3f", value)
}
