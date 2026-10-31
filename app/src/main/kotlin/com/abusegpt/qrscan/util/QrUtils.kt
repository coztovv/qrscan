package com.abusegpt.qrscan.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrUtils {

    fun encode(
        text: String,
        sizePx: Int,
        foreground: Int = 0xFF111318.toInt(),
        background: Int = 0xFFFFFFFF.toInt(),
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.Q
    ): Bitmap? {
        if (text.isBlank() || sizePx <= 0) return null
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to errorCorrection,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        return runCatching {
            val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(sizePx * sizePx)
            for (y in 0 until sizePx) {
                val row = y * sizePx
                for (x in 0 until sizePx) {
                    pixels[row + x] = if (matrix.get(x, y)) foreground else background
                }
            }
            Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }

    fun wifiPayload(ssid: String, password: String, security: String = "WPA"): String {
        val esc = { s: String -> s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:") }
        return buildString {
            append("WIFI:T:").append(if (password.isBlank()) "nopass" else security).append(';')
            append("S:").append(esc(ssid)).append(';')
            if (password.isNotBlank()) append("P:").append(esc(password)).append(';')
            append(";;")
        }
    }

    fun vcard(name: String, phone: String): String = buildString {
        append("BEGIN:VCARD\nVERSION:3.0\n")
        append("FN:").append(name).append('\n')
        if (phone.isNotBlank()) append("TEL;TYPE=CELL:").append(phone).append('\n')
        append("END:VCARD")
    }
}
