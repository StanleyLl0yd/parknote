package com.sl.parknote.domain

import java.util.UUID

/** Only one active record is stored. All timestamps are epoch milliseconds (UTC). */
data class ParkingRecord(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val placeName: String = "",
    val floor: String = "",
    val sector: String = "",
    val spaceNumber: String = "",
    val note: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val locationCapturedAt: Long? = null,
    val locationProvider: String? = null,
    val photos: List<String> = emptyList(),
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null

    fun validate() {
        require(id.isNotBlank())
        require(createdAt > 0 && updatedAt >= createdAt)
        require(placeName.length <= 120 && floor.length <= 40 && sector.length <= 40)
        require(spaceNumber.length <= 40 && note.length <= 500)
        require((latitude == null) == (longitude == null)) { "Coordinates must be a pair" }
        latitude?.let { require(it.isFinite() && it in -90.0..90.0) }
        longitude?.let { require(it.isFinite() && it in -180.0..180.0) }
        locationAccuracyMeters?.let { require(hasCoordinates && it.isFinite() && it >= 0f) }
        locationCapturedAt?.let { require(hasCoordinates && it > 0) }
        require(locationProvider == null || (hasCoordinates && locationProvider.length <= 100))
        require(photos.size <= 2 && photos.distinct() == photos)
        require(photos.all { PHOTO_ID.matches(it) })
        require(hasCoordinates || photos.isNotEmpty() || placeName.isNotBlank() ||
            floor.isNotBlank() || sector.isNotBlank() || spaceNumber.isNotBlank() || note.isNotBlank()) {
            "At least one useful parking detail must be provided"
        }
    }

    companion object { private val PHOTO_ID = Regex("[a-f0-9-]{36}\\.jpg") }
}
