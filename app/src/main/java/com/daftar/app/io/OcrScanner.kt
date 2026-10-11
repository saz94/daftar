package com.daftar.app.io

import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.daftar.app.domain.OcrLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Reads the text on an invoice photo, entirely on the phone (Google ML Kit, Latin script).
 * Nothing is uploaded. Arabic script is not supported by this recogniser.
 */
object OcrScanner {
    /** Longest side, in pixels, that the photo is shrunk to before reading. Plenty for printed text. */
    private const val MAX_SIDE = 3000

    suspend fun read(file: File): List<OcrLine> {
        // Decoding a camera photo is slow and memory-hungry, so it stays off the main thread.
        val image = withContext(Dispatchers.IO) { load(file) }
        return suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val out = ArrayList<OcrLine>()
                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            val box = line.boundingBox ?: continue
                            out.add(OcrLine(line.text, box.left, box.top, box.right, box.bottom))
                        }
                    }
                    recognizer.close()
                    if (cont.isActive) cont.resume(out)
                }
                .addOnFailureListener { error ->
                    recognizer.close()
                    if (cont.isActive) cont.resumeWithException(error)
                }
        }
    }

    private fun load(file: File): InputImage {
        val path = file.absolutePath
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2

        val options = BitmapFactory.Options()
        options.inSampleSize = sample
        val bitmap = BitmapFactory.decodeFile(path, options) ?: throw IOException("Not a readable picture")

        // phones store "which way up" separately from the pixels
        val rotation = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        return InputImage.fromBitmap(bitmap, rotation)
    }
}
