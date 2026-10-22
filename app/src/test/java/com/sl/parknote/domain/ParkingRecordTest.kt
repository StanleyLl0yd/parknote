package com.sl.parknote.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ParkingRecordTest {
    private fun photo() = "${UUID.randomUUID()}.jpg"
    private fun invalid(block: () -> Unit) {
        try { block(); fail("Expected rejection") }
        catch (_: IllegalArgumentException) { /* expected */ }
    }

    @Test fun undergroundWithoutGps() {
        ParkingRecord(placeName = "ТЦ Галерея", floor = "-2", sector = "C", spaceNumber = "147",
            note = "Лифт №3").validate()
    }

    @Test fun letterLevelAndZero() {
        listOf("-2", "0", "P1", "B2").forEach { ParkingRecord(floor = it).validate() }
    }

    @Test fun oneDetailIsEnough() {
        ParkingRecord(note = "Лифт").validate()
        ParkingRecord(photos = listOf(photo())).validate()
        ParkingRecord(latitude = 0.0, longitude = 0.0).validate()
        ParkingRecord(spaceNumber = "A7").validate()
    }

    @Test fun blankRecordRejected() = invalid { ParkingRecord().validate() }
    @Test fun coordinatePairsRequired() = invalid { ParkingRecord(latitude = 57.1).validate() }
    @Test fun coordinateRangesEnforced() {
        invalid { ParkingRecord(latitude = -91.0, longitude = 0.0).validate() }
        invalid { ParkingRecord(latitude = 0.0, longitude = 181.0).validate() }
        invalid { ParkingRecord(latitude = Double.NaN, longitude = 0.0).validate() }
        invalid { ParkingRecord(latitude = Double.POSITIVE_INFINITY, longitude = 0.0).validate() }
        ParkingRecord(latitude = -90.0, longitude = 180.0).validate()
    }

    @Test fun accuracyRequiresValidFix() {
        invalid { ParkingRecord(note = "X", locationAccuracyMeters = 5f).validate() }
        invalid { ParkingRecord(latitude = 1.0, longitude = 1.0, locationAccuracyMeters = -1f).validate() }
        invalid { ParkingRecord(note = "X", locationCapturedAt = 1234).validate() }
    }

    @Test fun maxTwoPhotosAndNoDuplicates() {
        val a = photo(); val b = photo(); val c = photo()
        ParkingRecord(photos = listOf(a, b)).validate()
        invalid { ParkingRecord(photos = listOf(a, b, c)).validate() }
        invalid { ParkingRecord(photos = listOf(a, a)).validate() }
        invalid { ParkingRecord(photos = listOf("../../data.txt")).validate() }
    }

    @Test fun textLimitsUnicode() {
        ParkingRecord(note = "Паркинг 🔑").validate()
        invalid { ParkingRecord(note = "x".repeat(501)).validate() }
        invalid { ParkingRecord(floor = "P".repeat(41)).validate() }
    }

    @Test fun updatesPreserveCreationAndIdentifier() {
        val original = ParkingRecord(note = "Old")
        val updated = original.copy(note = "New", updatedAt = original.updatedAt + 1)
        updated.validate()
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(original.id, updated.id)
    }

    @Test fun providerRequiresCoordinates() {
        invalid { ParkingRecord(note = "X", locationProvider = "gps").validate() }
        ParkingRecord(latitude = 58.0, longitude = 30.0, locationProvider = "gps").validate()
    }

    @Test fun timestampsMonotonic() {
        invalid { ParkingRecord(note = "X", createdAt = -1).validate() }
        invalid { ParkingRecord(note = "X", createdAt = 200, updatedAt = 100).validate() }
    }
}
