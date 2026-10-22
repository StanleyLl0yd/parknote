package com.sl.parknote.data

import android.content.Context
import android.util.AtomicFile
import com.sl.parknote.domain.ParkingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A single atomic JSON file. Photo lifecycle is controlled by PhotoStore. */
class ParkingRepository(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "active_parking.json"))
    private val mutex = Mutex()

    suspend fun load(): ParkingRecord? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val raw = try { file.openRead().use { it.readBytes() } }
                catch (_: java.io.FileNotFoundException) { return@withLock null }
            val json = JSONObject(String(raw, Charsets.UTF_8))
            decode(json).also { it.validate() }
        }
    }

    suspend fun save(record: ParkingRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            record.validate()
            val encoded = encode(record).toString().toByteArray(Charsets.UTF_8)
            val stream = file.startWrite()
            try {
                stream.write(encoded)
                file.finishWrite(stream)
            } catch (e: Exception) {
                file.failWrite(stream)
                throw e
            }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            file.delete()
            check(!file.baseFile.exists()) { "Could not delete parking record" }
        }
    }

    private fun encode(r: ParkingRecord): JSONObject = JSONObject().apply {
        put("schema", 1)
        put("id", r.id); put("createdAt", r.createdAt); put("updatedAt", r.updatedAt)
        put("placeName", r.placeName); put("floor", r.floor); put("sector", r.sector)
        put("spaceNumber", r.spaceNumber); put("note", r.note)
        r.latitude?.let { put("latitude", it) }; r.longitude?.let { put("longitude", it) }
        r.locationAccuracyMeters?.let { put("locationAccuracyMeters", it.toDouble()) }
        r.locationCapturedAt?.let { put("locationCapturedAt", it) }
        r.locationProvider?.let { put("locationProvider", it) }
        put("photos", JSONArray(r.photos))
    }

    private fun decode(j: JSONObject): ParkingRecord {
        require(j.getInt("schema") == 1) { "Unknown storage schema" }
        val pictures = j.getJSONArray("photos")
        return ParkingRecord(
            id = j.getString("id"), createdAt = j.getLong("createdAt"),
            updatedAt = j.getLong("updatedAt"),
            placeName = j.optString("placeName"), floor = j.optString("floor"),
            sector = j.optString("sector"), spaceNumber = j.optString("spaceNumber"),
            note = j.optString("note"),
            latitude = if (j.has("latitude")) j.getDouble("latitude") else null,
            longitude = if (j.has("longitude")) j.getDouble("longitude") else null,
            locationAccuracyMeters = if (j.has("locationAccuracyMeters")) j.getDouble("locationAccuracyMeters").toFloat() else null,
            locationCapturedAt = if (j.has("locationCapturedAt")) j.getLong("locationCapturedAt") else null,
            locationProvider = j.optString("locationProvider").ifBlank { null },
            photos = (0 until pictures.length()).map { pictures.getString(it) },
        )
    }
}
