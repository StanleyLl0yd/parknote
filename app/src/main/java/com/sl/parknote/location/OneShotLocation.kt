package com.sl.parknote.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.location.Location
import android.os.CancellationSignal
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.abs

/** One-shot positioning, no startup/background requests or silent cached fallback. */
class OneShotLocation(private val context: Context) {
    data class Fix(val latitude: Double, val longitude: Double, val accuracy: Float?, val time: Long, val provider: String?)

    /**
     * One unavailable or stale provider must not suppress another available provider.
     * Each attempt gets at most 4.5 s, with a shared 14 s deadline.
     */
    @SuppressLint("MissingPermission") // permissions checked before every attempt
    suspend fun capture(): Fix? {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            if (fine) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.distinct().filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        return withTimeoutOrNull(14_000) {
            for (provider in candidates) {
                // LocationManagerCompat can return null or a cached result.
                val location = withTimeoutOrNull(4_500) {
                    suspendCancellableCoroutine<Location?> { continuation ->
                        val cancellation = CancellationSignal()
                        continuation.invokeOnCancellation { cancellation.cancel() }
                        try {
                            LocationManagerCompat.getCurrentLocation(
                                manager, provider, cancellation, ContextCompat.getMainExecutor(context)
                            ) { fix -> if (continuation.isActive) continuation.resume(fix) }
                        } catch (_: SecurityException) {
                            if (continuation.isActive) continuation.resume(null)
                        } catch (_: IllegalArgumentException) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                }
                validFix(location)?.let { return@withTimeoutOrNull it }
            }
            null
        }
    }

    companion object {
        /** Keep invalid accuracy out of persisted parking records. */
        internal fun validFix(fix: Location?, nowMillis: Long = System.currentTimeMillis()): Fix? {
            fix ?: return null
            if (fix.time <= 0 || abs(nowMillis - fix.time) > 60_000) return null
            if (!fix.latitude.isFinite() || fix.latitude !in -90.0..90.0) return null
            if (!fix.longitude.isFinite() || fix.longitude !in -180.0..180.0) return null
            val accuracy = if (fix.hasAccuracy() && fix.accuracy.isFinite() && fix.accuracy >= 0f)
                fix.accuracy else null
            val provider = fix.provider?.takeIf { it.length <= 100 }
            return Fix(fix.latitude, fix.longitude, accuracy, fix.time, provider)
        }
    }
}
