package com.sl.parknote.photos

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PhotoStoreTest {
    private lateinit var root: File
    private lateinit var store: PhotoStore
    private lateinit var source: File

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "photo-test-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) {
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
        }
        store = PhotoStore(isolated)
        source = File(root, "source.png")
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try { FileOutputStream(source).use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
        finally { bitmap.recycle() }
    }

    @After fun teardown() { root.deleteRecursively() }

    @Test fun stagedPhotoSurvivesRestartGraceAndExpires() = runBlocking {
        val jpeg = store.path(store.importPhoto(Uri.fromFile(source)))
        store.removeUnreferenced(emptySet(), minimumAgeMillis = 24L * 60 * 60 * 1000)
        assertTrue(jpeg.isFile)
        assertTrue(jpeg.setLastModified(System.currentTimeMillis() - 48L * 60 * 60 * 1000))
        store.removeUnreferenced(emptySet(), minimumAgeMillis = 24L * 60 * 60 * 1000)
        assertFalse(jpeg.exists())
    }

    @Test fun committedPhotoSurvivesImmediateDraftPrune() = runBlocking {
        val saved = store.importPhoto(Uri.fromFile(source))
        val draft = store.importPhoto(Uri.fromFile(source))
        store.removeUnreferenced(setOf(saved))
        assertTrue(store.path(saved).isFile)
        assertFalse(store.path(draft).exists())
    }

    @Test fun partialFilesAreDiscardedWhileCommittedJpegRemains() = runBlocking {
        val saved = store.importPhoto(Uri.fromFile(source))
        val partial = File(store.path(saved).parentFile, "stale.jpg.part").apply { writeText("partial") }
        store.removeUnreferenced(setOf(saved), minimumAgeMillis = 24L * 60 * 60 * 1000)
        assertFalse(partial.exists())
        assertTrue(store.path(saved).isFile)
    }

    @Test fun badImageCannotLeaveJpegOrPartial() = runBlocking {
        val corrupt = File(root, "bad.jpg").apply { writeText("not a JPEG") }
        assertTrue(runCatching { store.importPhoto(Uri.fromFile(corrupt)) }.isFailure)
        assertTrue(File(root, "files/photos").listFiles().orEmpty().none {
            it.name.endsWith(".jpg") || it.name.endsWith(".part")
        })
    }

    @Test fun rotatedSourceIsUprightAndPrivateExifIsStripped() = runBlocking {
        val input = File(root, "orientation-gps.jpg")
        val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
        try {
            FileOutputStream(input).use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it))
            }
        } finally { bitmap.recycle() }
        ExifInterface(input.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setAttribute(ExifInterface.TAG_MAKE, "PRIVATE_CAMERA_MAKE")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "59/1,56/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "30/1,19/1,0/1")
            saveAttributes()
        }
        val imported = store.path(store.importPhoto(Uri.fromFile(input)))
        assertTrue(imported.isFile)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imported.absolutePath, options)
        assertEquals(40, options.outWidth)
        assertEquals(80, options.outHeight)
        ExifInterface(imported.absolutePath).apply {
            assertNull("Camera make must not leak into private JPEG", getAttribute(ExifInterface.TAG_MAKE))
            assertNull("GPS must not leak into private JPEG", getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            assertNull("GPS must not leak into private JPEG", getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
            assertEquals(ExifInterface.ORIENTATION_NORMAL, getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
        }
        // Import must not mutate the original provider-owned source file.
        assertEquals(ExifInterface.ORIENTATION_ROTATE_90, ExifInterface(input.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
    }

    @Test fun staleCameraCacheExpiresButRecentCaptureSurvives() = runBlocking {
        val dir = File(root, "cache/captures").apply { mkdirs() }
        val stale = File(dir, "old.jpg").apply { writeText("old") }
        val fresh = File(dir, "new.jpg").apply { writeText("new") }
        assertTrue(stale.setLastModified(System.currentTimeMillis() - 48L * 60 * 60 * 1000))
        store.cleanupStaleCameraFiles()
        assertFalse(stale.exists())
        assertTrue(fresh.exists())
    }
}
