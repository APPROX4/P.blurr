package com.pblurr.app.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.model.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ExportImageUseCase(
    private val context: Context
) {
    suspend operator fun invoke(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95,
        stripExif: Boolean = true,
        sourceUri: Uri? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filename = "Pblurr_${System.currentTimeMillis()}.${if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"}"

        AppLogger.i(
            LogCategory.EXPORT,
            "Starting image export process. Format: ${format.name}, Quality: $quality, Strip EXIF: $stripExif"
        )

        val resolver = context.contentResolver
        var originalTimestampMs: Long? = null

        // Extract original photo's true capture date if saving with metadata (stripExif = false)
        if (!stripExif && sourceUri != null) {
            originalTimestampMs = extractOriginalPhotoDateMs(context, sourceUri)
        }

        val targetTimestamp = originalTimestampMs
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg")

            // Preserve original photo's date & time if NOT stripping EXIF
            if (!stripExif && targetTimestamp != null) {
                put(MediaStore.Images.Media.DATE_TAKEN, targetTimestamp)
                put(MediaStore.Images.Media.DATE_ADDED, targetTimestamp / 1000L)
                put(MediaStore.Images.Media.DATE_MODIFIED, targetTimestamp / 1000L)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/P.blurr")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

        try {
            val outputStream: OutputStream = resolver.openOutputStream(uri)
                ?: return@withContext Result.failure(Exception("Failed to open output stream for Uri: $uri"))

            outputStream.use { stream ->
                bitmap.compress(format, quality, stream)
            }

            if (stripExif) {
                AppLogger.i(
                    LogCategory.EXPORT,
                    "EXIF Privacy Stripping Applied: All GPS locations, timestamps, camera specs & EXIF tags stripped cleanly."
                )
            } else if (sourceUri != null) {
                try {
                    val inputPf = resolver.openFileDescriptor(sourceUri, "r")
                    val outputPf = resolver.openFileDescriptor(uri, "rw")
                    if (inputPf != null && outputPf != null) {
                        val inputExif = ExifInterface(inputPf.fileDescriptor)
                        val outputExif = ExifInterface(outputPf.fileDescriptor)

                        val tagsToCopy = arrayOf(
                            ExifInterface.TAG_DATETIME,
                            ExifInterface.TAG_DATETIME_ORIGINAL,
                            ExifInterface.TAG_DATETIME_DIGITIZED,
                            ExifInterface.TAG_SUBSEC_TIME,
                            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                            ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
                            ExifInterface.TAG_GPS_DATESTAMP,
                            ExifInterface.TAG_GPS_TIMESTAMP,
                            ExifInterface.TAG_GPS_LATITUDE,
                            ExifInterface.TAG_GPS_LATITUDE_REF,
                            ExifInterface.TAG_GPS_LONGITUDE,
                            ExifInterface.TAG_GPS_LONGITUDE_REF,
                            ExifInterface.TAG_GPS_ALTITUDE,
                            ExifInterface.TAG_GPS_ALTITUDE_REF,
                            ExifInterface.TAG_GPS_PROCESSING_METHOD,
                            ExifInterface.TAG_MAKE,
                            ExifInterface.TAG_MODEL,
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.TAG_FLASH,
                            ExifInterface.TAG_FOCAL_LENGTH,
                            ExifInterface.TAG_WHITE_BALANCE,
                            ExifInterface.TAG_EXPOSURE_TIME,
                            ExifInterface.TAG_F_NUMBER,
                            ExifInterface.TAG_ISO_SPEED_RATINGS
                        )

                        for (tag in tagsToCopy) {
                            val value = inputExif.getAttribute(tag)
                            if (value != null) {
                                outputExif.setAttribute(tag, value)
                            }
                        }

                        // Explicitly set original datetime EXIF attributes if parsed
                        if (targetTimestamp != null) {
                            val formattedDateStr = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(java.util.Date(targetTimestamp))
                            outputExif.setAttribute(ExifInterface.TAG_DATETIME, formattedDateStr)
                            outputExif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, formattedDateStr)
                            outputExif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, formattedDateStr)
                        }

                        outputExif.saveAttributes()
                        inputPf.close()
                        outputPf.close()
                    }
                    AppLogger.i(
                        LogCategory.EXPORT,
                        "EXIF Metadata Preserved: Original image metadata tags & capture date (${targetTimestamp ?: "extracted"}) copied to exported file."
                    )
                } catch (exifErr: Exception) {
                    AppLogger.w(LogCategory.EXPORT, "Note copying EXIF tags: ${exifErr.message}")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                if (!stripExif && targetTimestamp != null) {
                    contentValues.put(MediaStore.Images.Media.DATE_TAKEN, targetTimestamp)
                    contentValues.put(MediaStore.Images.Media.DATE_ADDED, targetTimestamp / 1000L)
                    contentValues.put(MediaStore.Images.Media.DATE_MODIFIED, targetTimestamp / 1000L)
                }
                resolver.update(uri, contentValues, null, null)
            }

            val duration = System.currentTimeMillis() - startTime
            AppLogger.i(
                LogCategory.EXPORT,
                "Image saved successfully to gallery: $uri",
                "Resolution: ${bitmap.width}x${bitmap.height}, Duration: ${duration}ms, EXIF Stripped: $stripExif"
            )

            Result.success(uri)
        } catch (e: Exception) {
            AppLogger.e(LogCategory.EXPORT, "Failed to export image", e.stackTraceToString())
            Result.failure(e)
        }
    }

    private fun extractOriginalPhotoDateMs(context: Context, sourceUri: Uri): Long? {
        val resolver = context.contentResolver

        // 1. First attempt: Try reading EXIF DATETIME_ORIGINAL directly from sourceUri input stream
        try {
            resolver.openInputStream(sourceUri)?.use { inputStream ->
                val inputExif = ExifInterface(inputStream)
                val dateTimeStr = inputExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: inputExif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: inputExif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)

                if (!dateTimeStr.isNullOrBlank()) {
                    val formats = arrayOf(
                        "yyyy:MM:dd HH:mm:ss",
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "yyyy-MM-dd HH:mm:ss",
                        "yyyy:MM:dd HH:mm"
                    )
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.US)
                            val parsed = sdf.parse(dateTimeStr)
                            if (parsed != null && parsed.time > 0) {
                                AppLogger.i(LogCategory.EXPORT, "Extracted original EXIF date from stream: $dateTimeStr -> ${parsed.time} ms")
                                return parsed.time
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(LogCategory.EXPORT, "Failed to read EXIF from inputStream: ${e.message}")
        }

        // 2. Second attempt: Try openFileDescriptor for EXIF
        try {
            resolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                val inputExif = ExifInterface(pfd.fileDescriptor)
                val dateTimeStr = inputExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: inputExif.getAttribute(ExifInterface.TAG_DATETIME)
                if (!dateTimeStr.isNullOrBlank()) {
                    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    val parsed = sdf.parse(dateTimeStr)
                    if (parsed != null && parsed.time > 0) return parsed.time
                }
            }
        } catch (_: Exception) {}

        // 3. Third attempt: Query MediaStore DATE_TAKEN, DATE_MODIFIED, or DATE_ADDED
        try {
            resolver.query(
                sourceUri,
                arrayOf(
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.DATE_ADDED
                ),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dateTakenIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                    if (dateTakenIdx != -1 && !cursor.isNull(dateTakenIdx)) {
                        val dt = cursor.getLong(dateTakenIdx)
                        if (dt > 0) return dt
                    }
                    val dateModIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                    if (dateModIdx != -1 && !cursor.isNull(dateModIdx)) {
                        val dm = cursor.getLong(dateModIdx)
                        if (dm > 0) return dm * 1000L
                    }
                    val dateAddedIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    if (dateAddedIdx != -1 && !cursor.isNull(dateAddedIdx)) {
                        val da = cursor.getLong(dateAddedIdx)
                        if (da > 0) return da * 1000L
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(LogCategory.EXPORT, "Failed to query MediaStore columns: ${e.message}")
        }

        return null
    }
}
