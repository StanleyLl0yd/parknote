package com.sl.parknote.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sl.parknote.domain.ParkingRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ParkingRepositoryTest {
    private lateinit var testDir: File
    private lateinit var storage: ParkingRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        testDir = File(context.cacheDir, "parking-test-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) {
            override fun getFilesDir(): File = testDir
        }
        storage = ParkingRepository(isolated)
    }

    @After fun teardown() { testDir.deleteRecursively() }

    @Test fun saveReloadEditAndFinish() = runBlocking {
        assertNull(storage.load())
        val original = ParkingRecord(floor = "-2", sector = "Б", note = "Лифт №3")
        storage.save(original)
        assertEquals(original, storage.load())
        val edited = original.copy(spaceNumber = "147", updatedAt = original.updatedAt + 1)
        storage.save(edited)
        assertEquals(edited, storage.load())
        storage.clear()
        assertNull(storage.load())
    }

    @Test fun invalidUpdateDoesNotDestroyPreviousRecord() = runBlocking {
        val original = ParkingRecord(note = "Keep this")
        storage.save(original)
        runCatching { storage.save(ParkingRecord()) }.onSuccess { fail("Invalid record accepted") }
        assertEquals(original, storage.load())
    }

    @Test fun damagedStorageNotMistakenForEmpty() = runBlocking {
        storage.save(ParkingRecord(note = "Saved"))
        File(testDir, "active_parking.json").writeText("not json")
        runCatching { storage.load() }.onSuccess { fail("Corrupt record silently ignored") }
        assertTrue(File(testDir, "active_parking.json").isFile)
    }
}
