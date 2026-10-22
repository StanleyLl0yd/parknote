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

/** No location calls on startup, no background subscriptions, no stale fallback. */
class OneShotLocation(private val context: Context) {
    data class Fix(val latitude: Double, val longitude: Double, val accuracy: Float?, val time: Long, val provider: String?)

    @SuppressLint("MissingPermission") // checked immediately before call; button grants session permission
    suspend fun capture(): Fix? {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val preferred = buildList {
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }
        val provider = preferred.firstOrNull {
            (it != LocationManager.GPS_PROVIDER || fine) &&
                runCatching { manager.isProviderEnabled(it) }.getOrDefault(false)
        }
            ?: return null
        val fix: Location = withTimeoutOrNull(14_000) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val cancellation = CancellationSignal()
                continuation.invokeOnCancellation { cancellation.cancel() }
                try {
                    LocationManagerCompat.getCurrentLocation(
                        manager, provider, cancellation, ContextCompat.getMainExecutor(context)
                    ) { location -> if (continuation.isActive) continuation.resume(location) }
                } catch (e: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                } catch (e: IllegalArgumentException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        } ?: return null
        // Some providers may return recently cached fixes; never silently use stale coordinates.
        if (fix.time <= 0 || kotlin.math.abs(System.currentTimeMillis() - fix.time) > 60_000) return null
        if (!fix.latitude.isFinite() || !fix.longitude.isFinite() ||
            fix.latitude !in -90.0..90.0 || fix.longitude !in -180.0..180.0) return null
        return Fix(fix.latitude, fix.longitude, if (fix.hasAccuracy()) fix.accuracy else null, fix.time, fix.provider)
    }
}
