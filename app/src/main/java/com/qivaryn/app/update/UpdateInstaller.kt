package com.qivaryn.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.IOException

/**
 * Validates a downloaded APK and hands it to Android's package installer.
 *
 * The checks here are deliberately redundant with what the platform already
 * does, because each one turns an opaque installer failure into a message the
 * user can act on, and they close the gap before the installer UI appears:
 *
 *  * package name — refuses an APK for a different application;
 *  * version code — refuses a downgrade, so a stale or swapped file cannot
 *    replace an already-updated installation;
 *  * minimum SDK — refuses an APK this device cannot run;
 *  * signing certificate — refuses an APK not signed with the same key as the
 *    installed app. Android enforces this too, but failing early gives a clear
 *    explanation instead of a generic "App not installed" screen.
 */
object UpdateInstaller {

    private const val TAG = "QivarynUpdate"

    data class Validation(
        val ok: Boolean,
        val reason: String? = null,
        val versionCode: Long = 0L,
        val versionName: String? = null
    )

    /** The version currently installed, read from the platform. */
    fun installedVersion(context: Context): Pair<Long, String> {
        @Suppress("DEPRECATION")
        val info: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info) to (info.versionName ?: "0")
    }

    /**
     * True when Qivaryn is allowed to install packages. On Android 8+ this is
     * the per-app "install unknown apps" switch, not the old global setting.
     */
    fun canInstall(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /** Sends the user to the system screen where install permission is granted. */
    fun manageUnknownSourcesIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(android.net.Uri.parse("package:" + context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    fun validate(context: Context, apk: File): Validation {
        if (!apk.exists() || apk.length() == 0L) {
            return Validation(false, "The downloaded update file is missing or empty.")
        }

        val packageManager = context.packageManager
        @Suppress("DEPRECATION")
        val archive = packageManager.getPackageArchiveInfo(apk.absolutePath, archiveFlags())
            ?: return Validation(false, "The downloaded file is not a valid Android package.")

        val archivePackage = archive.packageName
        if (archivePackage != context.packageName) {
            return Validation(
                false,
                "The downloaded package ($archivePackage) is not Qivaryn. The update was rejected."
            )
        }

        val archiveCode = PackageInfoCompat.getLongVersionCode(archive)
        val (installedCode, installedName) = installedVersion(context)
        if (archiveCode <= installedCode) {
            return Validation(
                false,
                "The published build ($archiveCode) is not newer than the installed " +
                    "build ($installedCode). Nothing was changed.",
                archiveCode,
                archive.versionName
            )
        }

        val minimumSdk = archive.applicationInfo?.minSdkVersion ?: 0
        if (minimumSdk > Build.VERSION.SDK_INT) {
            return Validation(
                false,
                "This update needs Android API $minimumSdk or newer; this device runs " +
                    "API ${Build.VERSION.SDK_INT}."
            )
        }

        val signatureError = compareSignatures(context, packageManager, archive)
        if (signatureError != null) return Validation(false, signatureError)

        Log.d(
            TAG,
            "Update $installedName ($installedCode) -> ${archive.versionName} ($archiveCode) validated"
        )
        return Validation(true, null, archiveCode, archive.versionName)
    }

    /**
     * Returns null when the archive is signed by the same key as the installed
     * app. When the platform will not disclose the archive's signers, the check
     * is skipped rather than blocking the update — Android's installer performs
     * the same comparison and will refuse a mismatched APK.
     */
    private fun compareSignatures(
        context: Context,
        packageManager: PackageManager,
        archive: PackageInfo
    ): String? {
        val archiveSigners = signerDigests(archive)
        if (archiveSigners.isEmpty()) {
            Log.w(TAG, "Archive signatures unavailable; relying on the system installer")
            return null
        }

        @Suppress("DEPRECATION")
        val installed = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
        } catch (throwable: Throwable) {
            Log.w(TAG, "Could not read installed signatures: " + throwable.message)
            return null
        }

        val installedSigners = signerDigests(installed)
        if (installedSigners.isEmpty()) return null

        return if (installedSigners == archiveSigners) {
            null
        } else {
            "The update is not signed with the same key as the installed Qivaryn. " +
                "It was rejected to protect your data."
        }
    }

    private fun signerDigests(info: PackageInfo): List<String> {
        val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return emptyList()
            if (signing.hasMultipleSigners()) {
                signing.apkContentsSigners
            } else {
                signing.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return signatures
            ?.map { signature -> sha256Hex(signature.toByteArray()) }
            ?.sorted()
            ?: emptyList()
    }

    private fun archiveFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    // -------------------------------------------------------------------------
    // Installation
    // -------------------------------------------------------------------------

    /**
     * Streams [apk] into a package-installer session and commits it.
     *
     * The session API is used instead of an `ACTION_VIEW` intent so Android
     * reports progress and a specific failure reason back to
     * [InstallResultReceiver]. Installing over the same package with the same
     * signing key is an *upgrade*, so Android keeps `filesDir` (cookies and the
     * engine database), `SharedPreferences`, and the public Downloads folder.
     */
    fun commitInstall(context: Context, apk: File): Result<Unit> = try {
        val installer = context.packageManager.packageInstaller
        val parameters = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(context.packageName)
        }

        val sessionId = installer.createSession(parameters)
        val session = installer.openSession(sessionId)
        try {
            session.openWrite(SESSION_NAME, 0L, apk.length()).use { output ->
                apk.inputStream().use { input -> input.copyTo(output) }
                session.fsync(output)
            }
            session.commit(resultIntent(context).intentSender)
        } finally {
            session.close()
        }
        Result.success(Unit)
    } catch (throwable: Throwable) {
        Log.e(TAG, "Could not start the installer", throwable)
        Result.failure(
            if (throwable is IOException) throwable else IOException(
                throwable.message ?: throwable.javaClass.simpleName
            )
        )
    }

    private fun resultIntent(context: Context): PendingIntent {
        val intent = Intent(context, InstallResultReceiver::class.java)
            .setAction(ACTION_INSTALL_RESULT)
        // The system fills in the status extras, so the PendingIntent has to be
        // mutable on Android 12+.
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private const val SESSION_NAME = "qivaryn-update"
    private const val REQUEST_CODE = 0x51A2
}
