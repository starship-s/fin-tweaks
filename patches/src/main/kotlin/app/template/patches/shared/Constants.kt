package app.template.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_YAHOO_FINANCE = Compatibility(
        name = "Yahoo Finance",
        packageName = "com.yahoo.mobile.client.android.finance",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x6001D2, // Yahoo purple, shown in Morphe Manager
        targets = listOf(
            // Fingerprints were rewritten for 26.19.3, whose member names are
            // obfuscated; they match on surviving class, SDK and string references.
            AppTarget(
                version = "26.19.3",
            ),
        ),
    )
}
