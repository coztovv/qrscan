package com.abusegpt.qrscan.util

import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

object BarcodeDecoder {

    fun decode(context: Context, uri: Uri, onResult: (List<String>) -> Unit) {
        val main = ContextCompat.getMainExecutor(context)
        Thread {
            val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
            if (image == null) {
                main.execute { onResult(emptyList()) }
                return@Thread
            }
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { codes ->
                    val values = codes.mapNotNull { it.rawValue?.takeIf(String::isNotBlank) }
                    main.execute { onResult(values) }
                }
                .addOnFailureListener { main.execute { onResult(emptyList()) } }
        }.apply { name = "qr-image-decode" }.start()
    }
}
