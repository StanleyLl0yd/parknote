package com.sl.parknote.location

import android.location.Location
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OneShotLocationTest {
    private val now = 1_800_000_000_000L

    private fun candidate(
        ageMillis: Long = 0,
        latitude: Double = 59.94,
        longitude: Double = 30.32,
        accuracy: Float? = 9f
    ) = Location("gps").apply {
        time = now - ageMillis
        this.latitude = latitude
        this.longitude = longitude
        if (accuracy != null) this.accuracy = accuracy
    }

    @Test fun currentGpsFixAccepted() {
        val result = OneShotLocation.validFix(candidate(), now)
        assertNotNull(result)
        assertEquals(59.94, result!!.latitude, 0.0)
        assertEquals(9f, result.accuracy!!, 0f)
        assertEquals("gps", result.provider)
    }

    @Test fun staleAndFutureFixRejected() {
        assertNull(OneShotLocation.validFix(candidate(ageMillis = 60_001), now))
        assertNull(OneShotLocation.validFix(candidate(ageMillis = -60_001), now))
        assertNotNull(OneShotLocation.validFix(candidate(ageMillis = 60_000), now))
    }

    @Test fun invalidCoordinatesRejected() {
        assertNull(OneShotLocation.validFix(candidate(latitude = 91.0), now))
        assertNull(OneShotLocation.validFix(candidate(longitude = -181.0), now))
        assertNull(OneShotLocation.validFix(candidate(latitude = Double.NaN), now))
        assertNull(OneShotLocation.validFix(candidate(longitude = Double.POSITIVE_INFINITY), now))
    }

    @Test fun unusableAccuracyDoesNotCorruptValidCoordinates() {
        assertNull(OneShotLocation.validFix(null, now))
        assertNull(OneShotLocation.validFix(candidate(ageMillis = now + 1), now))
        assertNull(OneShotLocation.validFix(candidate(accuracy = null), now)?.accuracy)
        assertNull(OneShotLocation.validFix(candidate(accuracy = Float.NaN), now)?.accuracy)
        assertNull(OneShotLocation.validFix(candidate(accuracy = -1f), now)?.accuracy)
    }
}
