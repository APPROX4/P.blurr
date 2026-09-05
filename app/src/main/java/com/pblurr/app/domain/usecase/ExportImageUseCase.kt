package com.pblurr.app.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.model.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

class ExportImageUseCase(
    private val context: Context
) {
    suspend operator fun invoke(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filename = "Pblurr_${System.currentTimeMillis()}.${if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"}"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/P.blurr")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

        try {
            val outputStream: OutputStream = resolver.openOutputStream(uri)
                ?: return@withContext Result.failure(Exception("Failed to open output stream for Uri: $uri"))

            outputStream.use { stream ->
                bitmap.compress(format, quality, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            val duration = System.currentTimeMillis() - startTime
            AppLogger.i(
                LogCategory.EXPORT,
                "Image saved successfully to gallery: $uri",
                "Resolution: ${bitmap.width}x${bitmap.height}, Duration: ${duration}ms"
            )

            Result.success(uri)
        } catch (e: Exception) {
            AppLogger.e(LogCategory.EXPORT, "Failed to export image", e.stackTraceToString())
            Result.failure(e)
        }
    }
}
