package com.sl.parknote

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Validate the installed merged manifest, not merely the source manifest. */
@RunWith(AndroidJUnit4::class)
class PrivacyManifestTest {
    @Test fun installedAppHasNoNetworkCameraOrMediaReadPermissions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val actual = info.requestedPermissions.orEmpty().toSet()
        val forbidden = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CAMERA",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.ACCESS_MEDIA_LOCATION",
            "android.permission.QUERY_ALL_PACKAGES",
        )
        assertTrue("Unexpected merged permissions: ${actual.intersect(forbidden)}",
            actual.intersect(forbidden).isEmpty())
    }

    @Test fun automaticAppBackupIsDisabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Application must not be automatically backed up", 0,
            context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}
