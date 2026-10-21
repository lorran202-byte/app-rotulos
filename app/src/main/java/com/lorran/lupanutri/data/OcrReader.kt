package com.lorran.lupanutri.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lorran.lupanutri.domain.OcrLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Lê o texto de uma foto no próprio aparelho (sem internet). */
object OcrReader {
    suspend fun read(context: Context, uri: Uri): List<OcrLine> {
        val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
        return suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    val lines = text.textBlocks.flatMap { block ->
                        block.lines.mapNotNull { line ->
                            line.boundingBox?.let { r -> OcrLine(line.text, r.left, r.top, r.right, r.bottom) }
                        }
                    }
                    recognizer.close()
                    if (cont.isActive) cont.resume(lines)
                }
                .addOnFailureListener { e ->
                    recognizer.close()
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }
}
