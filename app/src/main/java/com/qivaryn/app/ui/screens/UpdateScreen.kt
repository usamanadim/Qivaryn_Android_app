package com.qivaryn.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qivaryn.app.BuildConfig
import com.qivaryn.app.data.formatBytes
import com.qivaryn.app.ui.components.HeaderBar
import com.qivaryn.app.ui.components.PrimaryButton
import com.qivaryn.app.ui.components.QIcon
import com.qivaryn.app.ui.components.SectionLabel
import com.qivaryn.app.ui.theme.QivarynColors
import com.qivaryn.app.update.ReleaseInfo
import com.qivaryn.app.update.UpdateInstaller
import com.qivaryn.app.update.UpdateManager
import com.qivaryn.app.update.UpdateStage
import com.qivaryn.app.update.UpdateUiState
import com.qivaryn.app.update.VersionSnapshot
import com.qivaryn.app.update.releaseOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Settings → Software update.
 *
 * Checks the app's own GitHub repository for a newer release on the build's
 * channel, then downloads and installs the matching APK through Android's own
 * installer. Because the Python engine is packaged inside the APK, an engine fix
 * arrives as a normal app update; the screen shows both versions so it is obvious
 * which engine a build carries.
 */
@Composable
fun UpdateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by UpdateManager.state.collectAsState()
    var snapshot by remember { mutableStateOf(UpdateManager.snapshot()) }

    // Returning from the "install unknown apps" screen re-checks permission and
    // continues the install the user already asked for.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (UpdateInstaller.canInstall(context)) {
            UpdateManager.downloadAndInstall(context)
        } else {
            UpdateManager.onInstallPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        snapshot = withContext(Dispatchers.IO) { UpdateManager.loadSnapshot(context) }
        UpdateManager.refresh(context)
        if (UpdateManager.state.value is UpdateUiState.Idle) {
            UpdateManager.check(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QivarynColors.current.Background)
    ) {
        HeaderBar(title = "Software update", titleSize = 24.sp, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 40.dp)
        ) {
            VersionCard(snapshot = snapshot)
            Spacer(Modifier.height(18.dp))

            StatusBlock(
                state = state,
                onCheckAgain = { UpdateManager.check(context, force = true) },
                onInstall = { UpdateManager.downloadAndInstall(context) },
                onGrantPermission = {
                    permissionLauncher.launch(UpdateInstaller.manageUnknownSourcesIntent(context))
                },
                onDismissInstalled = { UpdateManager.dismissResult() }
            )

            state.releaseOrNull()?.let { release ->
                if (release.notes.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    ReleaseNotes(release)
                }
            }

            Spacer(Modifier.height(20.dp))
            SourceNote()
        }
    }
}

// -----------------------------------------------------------------------------
// Version card
// -----------------------------------------------------------------------------

@Composable
private fun VersionCard(snapshot: VersionSnapshot?) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = QivarynColors.current.Surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(QivarynColors.current.MintSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = QIcon.Sync,
                    contentDescription = null,
                    tint = QivarynColors.current.OnMint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column {
                Text(
                    text = "Qivaryn " + (snapshot?.versionName ?: "…"),
                    color = QivarynColors.current.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Engine " + (snapshot?.engineVersion ?: "…") +
                        "  ·  " + UpdateManager.channel.displayName + " channel" +
                        "  ·  build " + (snapshot?.versionCode?.toString() ?: "…"),
                    color = QivarynColors.current.TextSecondary,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// State-driven status block
// -----------------------------------------------------------------------------

@Composable
private fun StatusBlock(
    state: UpdateUiState,
    onCheckAgain: () -> Unit,
    onInstall: () -> Unit,
    onGrantPermission: () -> Unit,
    onDismissInstalled: () -> Unit
) {
    when (state) {
        UpdateUiState.Idle -> StatusCard(
            icon = QIcon.Sync,
            title = "Check for updates",
            body = "Qivaryn asks its own GitHub repository for the newest " +
                UpdateManager.channel.description + " and installs it here.",
            tone = Tone.NEUTRAL
        ) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Check now",
                onClick = onCheckAgain,
                modifier = Modifier.fillMaxWidth()
            )
        }

        UpdateUiState.Checking -> StatusCard(
            icon = QIcon.Sync,
            title = "Checking for updates…",
            body = "Reading the latest release from GitHub.",
            tone = Tone.NEUTRAL,
            busy = true
        )

        is UpdateUiState.UpToDate -> StatusCard(
            icon = QIcon.Check,
            title = "You're up to date",
            body = "Qivaryn " + state.installed + " is the newest " +
                UpdateManager.channel.description + ".",
            tone = Tone.SUCCESS
        ) {
            Spacer(Modifier.height(16.dp))
            GhostButton(text = "Check again", onClick = onCheckAgain)
        }

        is UpdateUiState.Available -> StatusCard(
            icon = QIcon.DownloadOutline,
            title = "Version " + state.release.version + " is available",
            body = "Installed: " + state.installed + "  ·  " +
                formatBytes(state.release.asset.sizeBytes) +
                ". Your downloads, cookies, and history are kept.",
            tone = Tone.ACCENT
        ) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Download and install",
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        is UpdateUiState.Downloading -> {
            StatusCard(
                icon = QIcon.DownloadOutline,
                title = "Downloading " + state.release.version + "…",
                body = if (state.totalBytes > 0L) {
                    formatBytes(state.bytesRead) + " of " + formatBytes(state.totalBytes)
                } else {
                    formatBytes(state.bytesRead)
                },
                tone = Tone.ACCENT
            ) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = QivarynColors.current.Teal,
                    trackColor = QivarynColors.current.Panel
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (state.progress * 100).toInt().toString() + "%  ·  " +
                        "keep the app open until it finishes",
                    color = QivarynColors.current.TextSecondary,
                    fontSize = 11.5.sp
                )
            }
        }

        is UpdateUiState.Verifying -> StatusCard(
            icon = QIcon.Shield,
            title = "Verifying the update…",
            body = "Checking the file signature and checksum before installing.",
            tone = Tone.ACCENT,
            busy = true
        )

        is UpdateUiState.NeedsInstallPermission -> StatusCard(
            icon = QIcon.Shield,
            title = "Allow Qivaryn to install updates",
            body = "Android needs your permission once. Qivaryn only uses it for its own updates.",
            tone = Tone.WARNING
        ) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Open install settings",
                onClick = onGrantPermission,
                modifier = Modifier.fillMaxWidth()
            )
        }

        is UpdateUiState.AwaitingInstall -> StatusCard(
            icon = QIcon.DownloadOutline,
            title = "Finish installing",
            body = "Confirm the update in the Android installer, then come back here.",
            tone = Tone.ACCENT,
            busy = true
        )

        is UpdateUiState.Installed -> StatusCard(
            icon = QIcon.Check,
            title = "Update installed",
            body = if (state.version.isBlank()) {
                "Qivaryn is up to date."
            } else {
                "Qivaryn " + state.version + " is now installed. Your data was kept."
            },
            tone = Tone.SUCCESS
        ) {
            Spacer(Modifier.height(16.dp))
            GhostButton(text = "Done", onClick = onDismissInstalled)
        }

        is UpdateUiState.Failed -> StatusCard(
            icon = QIcon.Shield,
            title = state.title ?: failureTitle(state.stage),
            body = state.message,
            tone = Tone.DANGER
        ) {
            if (!state.detail.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.detail,
                    color = QivarynColors.current.TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = state.advice ?: failureAdvice(state.stage),
                color = QivarynColors.current.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = if (state.stage == UpdateStage.CHECK) "Try again" else "Retry download",
                onClick = if (state.stage == UpdateStage.CHECK) onCheckAgain else onInstall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun failureTitle(stage: UpdateStage): String = when (stage) {
    UpdateStage.CHECK -> "Couldn't check for updates"
    UpdateStage.DOWNLOAD -> "Download failed"
    UpdateStage.VERIFY -> "Update rejected"
    UpdateStage.INSTALL -> "Install failed"
}

private fun failureAdvice(stage: UpdateStage): String = when (stage) {
    UpdateStage.CHECK ->
        "Check your internet connection and try again. Nothing on your device was changed."
    UpdateStage.DOWNLOAD ->
        "The partial file was deleted. Try again on a stable connection; if it keeps " +
            "failing, download the release from GitHub in a browser."
    UpdateStage.VERIFY ->
        "The file was discarded. Try again later or download the release directly from GitHub."
    UpdateStage.INSTALL ->
        "Qivaryn is unchanged and still works. You can install the release manually from GitHub."
}

// -----------------------------------------------------------------------------
// Release notes and source
// -----------------------------------------------------------------------------

@Composable
private fun ReleaseNotes(release: ReleaseInfo) {
    Column {
        SectionLabel(
            text = "WHAT'S NEW IN " + release.version,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = QivarynColors.current.Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = release.notes.trim(),
                color = QivarynColors.current.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)
            )
        }
    }
}

@Composable
private fun SourceNote() {
    Text(
        text = "Releases are published at github.com/" + BuildConfig.UPDATE_REPO +
            " (" + UpdateManager.channel.displayName + " channel). " +
            "Only signed releases from that repository are installed.",
        color = QivarynColors.current.TextMuted,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

// -----------------------------------------------------------------------------
// Building blocks
// -----------------------------------------------------------------------------

private enum class Tone { NEUTRAL, ACCENT, SUCCESS, WARNING, DANGER }

@Composable
private fun StatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    tone: Tone,
    busy: Boolean = false,
    footer: (@Composable () -> Unit)? = null
) {
    val palette = QivarynColors.current
    val accent: Color = when (tone) {
        Tone.NEUTRAL -> palette.TextSecondary
        Tone.ACCENT -> palette.Teal
        Tone.SUCCESS -> palette.Success
        Tone.WARNING -> palette.Amber
        Tone.DANGER -> palette.Danger
    }
    val container: Color = when (tone) {
        Tone.NEUTRAL -> palette.Surface
        Tone.ACCENT -> palette.Surface
        Tone.SUCCESS -> palette.SuccessBg
        Tone.WARNING -> palette.AmberBg
        Tone.DANGER -> palette.DangerBg
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    color = palette.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = body,
                color = palette.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            footer?.invoke()
        }
    }
}

@Composable
private fun GhostButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(QivarynColors.current.Panel)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = QivarynColors.current.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
