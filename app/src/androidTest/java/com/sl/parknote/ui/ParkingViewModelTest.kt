package com.sl.parknote.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sl.parknote.data.ParkingRepository
import com.sl.parknote.domain.ParkingRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ParkingViewModelTest {
    private lateinit var folder: File
    private lateinit var isolated: Context
    private lateinit var model: ParkingViewModel

    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        folder = File(app.cacheDir, "vm-test-${UUID.randomUUID()}").apply { mkdirs() }
        isolated = object : ContextWrapper(app) {
            override fun getFilesDir() = File(folder, "files").apply { mkdirs() }
            override fun getCacheDir() = File(folder, "cache").apply { mkdirs() }
        }
        model = ParkingViewModel(isolated)
        runBlocking { withTimeout(10_000) { model.ready.first { it } } }
    }

    @After fun teardown() {
        folder.deleteRecursively()
    }

    @Test fun finishCannotDeleteRecordDuringPendingSave() {
        val record = ParkingRecord(note = "Keep pending save")
        val completed = CountDownLatch(1)
        val blockedFinish = AtomicInteger(0)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            model.save(record) { success ->
                assertTrue(success)
                completed.countDown()
            }
            model.finish { blockedFinish.incrementAndGet() }
        }
        assertTrue(completed.await(10, TimeUnit.SECONDS))
        runBlocking {
            withTimeout(10_000) { model.working.first { !it } }
            assertEquals(record, ParkingRepository(isolated).load())
        }
        assertEquals(0, blockedFinish.get())
    }

    @Test fun rapidDuplicateSaveDoesNotReplaceFirstCommittedRecord() {
        val first = ParkingRecord(note = "First tap")
        val second = ParkingRecord(note = "Second tap")
        val completed = CountDownLatch(1)
        val laterCallback = AtomicInteger(0)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            model.save(first) { success ->
                assertTrue(success)
                completed.countDown()
            }
            model.save(second) { laterCallback.incrementAndGet() }
            assertTrue("Operation must be busy before the first coroutine dispatch", model.working.value)
        }
        assertTrue("First write did not complete", completed.await(10, TimeUnit.SECONDS))
        runBlocking {
            withTimeout(10_000) { model.working.first { !it } }
            assertEquals(first, ParkingRepository(isolated).load())
        }
        assertEquals("A queued second tap must never overwrite the first", 0, laterCallback.get())
    }
}
