package com.qivaryn.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.IntentCompat

/**
 * Receives the outcome of an install session started by [UpdateInstaller].
 *
 * Declared with `exported="false"`: the system can still reach it because it is
 * addressed through a PendingIntent created by this app, which carries this
 * app's identity.
 */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        when (status) {
            // Android is asking the user to confirm. Without starting this
            // activity the install would silently never appear.
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_INTENT,
                    Intent::class.java
                )
                if (confirm == null) {
                    UpdateManager.onInstallFinished(
                        success = false,
                        message = "Android did not provide an installer confirmation."
                    )
                    return
                }
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirm) }
                    .onFailure {
                        Log.e(TAG, "Could not open the installer UI", it)
                        UpdateManager.onInstallFinished(
                            success = false,
                            message = "The system installer could not be opened."
                        )
                    }
            }

            PackageInstaller.STATUS_SUCCESS ->
                UpdateManager.onInstallFinished(success = true, message = null)

            else ->
                UpdateManager.onInstallFinished(success = false, message = describe(status, message))
        }
    }

    private fun describe(status: Int, message: String?): String {
        val reason = when (status) {
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                "Android blocked the install. Allow Qivaryn to install unknown apps, then retry."
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                "The install was cancelled, so Qivaryn is unchanged."
            PackageInstaller.STATUS_FAILURE_INVALID ->
                "The downloaded file is not a valid Qivaryn update."
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                "A different build of Qivaryn is already installed. Uninstall it first, " +
                    "then install the update."
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                "This update is not compatible with this device or Android version."
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                "There is not enough free storage to install the update."
            else -> "The update could not be installed."
        }
        return if (message.isNullOrBlank()) reason else reason + " (" + message + ")"
    }

    private companion object {
        const val TAG = "QivarynUpdate"
    }
}

/**
 * Action for the PendingIntent handed to the package installer. Kept next to
 * the receiver that answers it so the two can never drift apart.
 */
internal const val ACTION_INSTALL_RESULT = "com.qivaryn.app.INSTALL_RESULT"
