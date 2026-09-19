package com.cinemaparfi.jatim.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream

data class SelectedImagePayload(
    val previewBitmap: Bitmap,
    val base64: String,
    val fileName: String,
    val mimeType: String
)

data class SelectedFilePayload(
    val base64: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Int
)

fun readSelectedImage(context: Context, uri: Uri): Result<SelectedImagePayload> {
    return runCatching {
        val inputStream = context.contentResolver.openInputStream(uri) 
            ?: throw IllegalStateException("Gagal membuka file gambar.")
        val bytes = inputStream.readBytes()
        inputStream.close()

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) 
            ?: throw IllegalStateException("Format gambar tidak didukung.")
        
        // Scale down if too large to avoid OOM and heavy payloads
        val scaledBitmap = if (bitmap.width > 1200 || bitmap.height > 1200) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (targetWidth, targetHeight) = if (ratio > 1) {
                1200 to (1200 / ratio).toInt()
            } else {
                (1200 * ratio).toInt() to 1200
            }
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val compressedBytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

        var fileName = "image.jpg"
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        SelectedImagePayload(
            previewBitmap = scaledBitmap,
            base64 = base64,
            fileName = fileName,
            mimeType = mimeType
        )
    }
}

fun readSelectedFile(context: Context, uri: Uri, maxBytes: Int = 15 * 1024 * 1024): Result<SelectedFilePayload> {
    return runCatching {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Gagal membuka file.")
        val bytes = inputStream.readBytes()
        inputStream.close()

        if (bytes.isEmpty()) throw IllegalStateException("File kosong.")
        if (bytes.size > maxBytes) {
            throw IllegalStateException("Ukuran trailer terlalu besar. Maksimal ${maxBytes / (1024 * 1024)} MB.")
        }

        var fileName = "trailer.mp4"
        val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        SelectedFilePayload(
            base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = bytes.size
        )
    }
}
