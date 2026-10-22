package com.sl.parknote.photos

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
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
