package com.sl.parknote.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Compresses into a private JPEG, normalizes EXIF rotation and removes source metadata. */
class PhotoStore(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun path(id: String): File {
        require(Regex("[a-f0-9-]{36}\\.jpg").matches(id))
        return File(dir, id)
    }

    suspend fun importPhoto(uri: Uri): String = withContext(Dispatchers.IO) {
        val id = "${UUID.randomUUID()}.jpg"
        val target = path(id)
        val part = File(dir, "$id.part")
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: error("Image cannot be opened")
            require(bounds.outWidth in 1..20_000 && bounds.outHeight in 1..20_000) { "Invalid image dimensions" }
            var sample = 1
            while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Invalid or unsupported image")
            val orientation = context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> preScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> preScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { preScale(-1f, 1f); postRotate(270f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { preScale(-1f, 1f); postRotate(90f) }
                }
            }
            val corrected = if (matrix.isIdentity()) original else Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            FileOutputStream(part).use { stream ->
                check(corrected.compress(Bitmap.CompressFormat.JPEG, 88, stream))
                stream.fd.sync()
            }
            if (corrected !== original) corrected.recycle()
            original.recycle()
            check(part.renameTo(target)) { "Photo could not be committed" }
            id
        } catch (e: OutOfMemoryError) {
            part.delete(); target.delete()
            throw IllegalStateException("Image is too large to process", e)
        } catch (e: Exception) {
            part.delete(); target.delete(); throw e
        }
    }

    suspend fun cleanupStaleCameraFiles() = withContext(Dispatchers.IO) {
        val captures = File(context.cacheDir, "captures")
        val oldestAllowed = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        captures.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() in 1 until oldestAllowed) file.delete()
        }
    }

    suspend fun removeUnreferenced(keep: Set<String>) = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { file ->
            if (file.name !in keep && (file.name.endsWith(".part") || file.name.endsWith(".jpg"))) file.delete()
        }
    }
}
