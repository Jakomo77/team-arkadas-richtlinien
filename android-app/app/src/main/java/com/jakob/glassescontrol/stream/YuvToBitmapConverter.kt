package com.jakob.glassescontrol.stream

import android.graphics.Bitmap
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/** Converts a planar I420 (YUV 4:2:0) video frame from the glasses' camera into an ARGB bitmap. */
object YuvToBitmapConverter {

  fun convert(buffer: ByteBuffer, width: Int, height: Int): Bitmap? {
    if (width <= 0 || height <= 0) return null

    val data = ByteArray(buffer.remaining())
    buffer.duplicate().get(data)

    val frameSize = width * height
    if (data.size < frameSize + frameSize / 2) return null

    val pixels = IntArray(frameSize)
    val uOffset = frameSize
    val vOffset = frameSize + frameSize / 4

    for (row in 0 until height) {
      val yRowStart = row * width
      val uvRow = row / 2
      for (col in 0 until width) {
        val y = (data[yRowStart + col].toInt() and 0xFF) - 16
        val uvCol = col / 2
        val u = (data[uOffset + uvRow * (width / 2) + uvCol].toInt() and 0xFF) - 128
        val v = (data[vOffset + uvRow * (width / 2) + uvCol].toInt() and 0xFF) - 128

        val yy = max(0, y) * 1192
        var r = (yy + 1634 * v) shr 10
        var g = (yy - 833 * v - 400 * u) shr 10
        var b = (yy + 2066 * u) shr 10

        r = clamp(r)
        g = clamp(g)
        b = clamp(b)

        pixels[yRowStart + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
      }
    }

    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
  }

  private fun clamp(value: Int): Int = min(255, max(0, value))
}
