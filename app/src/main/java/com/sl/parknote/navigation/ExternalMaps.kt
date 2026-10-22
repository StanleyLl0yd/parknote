package com.sl.parknote.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object ExternalMaps {
    fun uri(latitude: Double, longitude: Double): Uri {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        val lat = String.format(Locale.ROOT, "%.7f", latitude)
        val lon = String.format(Locale.ROOT, "%.7f", longitude)
        return Uri.parse("geo:$lat,$lon?q=$lat,$lon")
    }

    fun open(context: Context, latitude: Double, longitude: Double): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri(latitude, longitude))
            if (intent.resolveActivity(context.packageManager) == null) false
            else {
                context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }
        } catch (_: ActivityNotFoundException) { false }
          catch (_: SecurityException) { false }
    }
}
