package com.alish10e.img3ddxf

import java.util.Locale

object ObjExporter {
    fun build(map: HeightMap, settings: ReliefSettings, modelWidth: Float = 100f): String {
        val sx = modelWidth / (map.width - 1); val sy = modelWidth * map.height / (map.width - 1) / map.height
        val out = StringBuilder("# Image to 3D Extrusion\n# Local relief mesh\no relief\n")
        fun vertex(x: Int, y: Int, z: Float) { out.append("v ${fmt(x * sx)} ${fmt((map.height - 1 - y) * sy)} ${fmt(z * settings.depth)}\n") }
        for (y in 0 until map.height) for (x in 0 until map.width) vertex(x, y, map.values[y * map.width + x])
        fun idx(x: Int, y: Int) = y * map.width + x + 1
        for (y in 0 until map.height - 1) for (x in 0 until map.width - 1) { val a = idx(x, y); val b = idx(x + 1, y); val c = idx(x + 1, y + 1); val d = idx(x, y + 1); out.append("f $a $b $c\nf $a $c $d\n") }
        return out.toString()
    }
    private fun fmt(value: Float) = String.format(Locale.US, "%.3f", value)
}
