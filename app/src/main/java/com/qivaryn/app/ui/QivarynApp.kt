package com.qivaryn.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.qivaryn.app.data.AppScreen
import com.qivaryn.app.data.DownloadDestination
import com.qivaryn.app.data.DownloadEntry
import com.qivaryn.app.data.DownloadFilter
import com.qivaryn.app.data.DownloadScope
import com.qivaryn.app.data.QivarynState
import com.qivaryn.app.data.formatBytes
import com.qivaryn.app.ui.components.CookiesDialog
import com.qivaryn.app.ui.components.PrivacyPolicyDialog
import com.qivaryn.app.ui.components.QIcon
import com.qivaryn.app.ui.screens.DownloadsScreen
import com.qivaryn.app.ui.screens.HistoryScreen
import com.qivaryn.app.ui.screens.HomeScreen
import com.qivaryn.app.ui.screens.InstagramScreen
import com.qivaryn.app.ui.screens.PrivacyScreen
import com.qivaryn.app.ui.screens.SettingsScreen
import com.qivaryn.app.ui.screens.UpdateScreen
import com.qivaryn.app.ui.theme.QivarynColors
import com.qivaryn.app.ui.theme.QivarynTheme
import java.io.File

@Composable
fun QivarynApp() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val state = remember { QivarynState(context) }
    var filter by rememberSaveable { mutableStateOf(DownloadFilter.ALL) }
    var privacyOpen by rememberSaveable { mutableStateOf(false) }

    QivarynTheme(darkTheme = state.isDarkTheme) {
        val view = LocalView.current
        val activity = view.context.findActivity()
        LaunchedEffect(state.isDarkTheme, activity) {
            if (activity is ComponentActivity) {
                val dark = state.isDarkTheme
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                activity.enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
                val window = activity.window
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = if (dark) {
                    Color.rgb(18, 19, 17)
                } else {
                    Color.rgb(247, 244, 237)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }

        val cookiePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                val error = state.importCookies(uri)
                Toast.makeText(
                    context,
                    error ?: "Instagram cookies imported",
                    if (error == null) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                ).show()
            }
        }

        BackHandler(
            enabled = state.cookiesOpen || state.screen == AppScreen.INSTAGRAM ||
                state.screen == AppScreen.HISTORY || state.screen == AppScreen.SETTINGS ||
                state.screen == AppScreen.DOWNLOADS || state.screen == AppScreen.UPDATE
        ) {
            state.handleBack()
        }

        Box(modifier = Modifier.fillMaxSize().background(QivarynColors.current.Background)) {
            AnimatedContent(
                targetState = state.screen,
                transitionSpec = {
                    val forward = targetState.depth() > initialState.depth()
                    if (targetState.depth() == initialState.depth()) {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                    } else if (forward) {
                        (slideInHorizontally(tween(260)) { it / 5 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(220)) { -it / 10 } + fadeOut(tween(160)))
                    } else {
                        (slideInHorizontally(tween(220)) { -it / 10 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(260)) { it / 5 } + fadeOut(tween(160)))
                    }
                },
                label = "qivaryn-screen"
            ) { screen ->
                when (screen) {
                    AppScreen.PRIVACY -> PrivacyScreen(
                        onContinue = { state.continueFromPrivacy() }
                    )

                    AppScreen.HOME -> HomeScreen(
                        onOpenInstagram = { state.navigate(AppScreen.INSTAGRAM) },
                        onPlatformTap = { platform ->
                            Toast.makeText(
                                context,
                                platform.label + " support is coming soon",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    AppScreen.INSTAGRAM -> InstagramScreen(
                        cookiesImported = state.cookiesImported,
                        hasActiveDownload = state.activeCount() > 0,
                        onBack = { state.navigate(AppScreen.HOME) },
                        onManageCookies = { state.openCookies() },
                        onDownload = { url, scope, mediaType, count ->
                            val error = state.startDownload(
                                rawUrl = url,
                                scope = scope,
                                mediaType = mediaType,
                                mediaCount = count,
                                onFinished = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            )
                            if (error == null) {
                                filter = DownloadFilter.ALL
                                state.navigate(AppScreen.DOWNLOADS)
                            }
                            error
                        }
                    )

                    AppScreen.DOWNLOADS -> DownloadsScreen(
                        entries = state.entries(filter),
                        filter = filter,
                        onFilterChange = { filter = it },
                        onOpenHistory = { state.navigate(AppScreen.HISTORY) },
                        onRemove = { state.removeEntry(it) },
                        onShare = { shareEntry(context, it) },
                        onOpenEntry = { openEntry(context, it) },
                        onManageCookies = { state.openCookies() },
                        onGoInstagram = { state.navigate(AppScreen.INSTAGRAM) }
                    )

                    AppScreen.HISTORY -> HistoryScreen(
                        entries = state.allEntries(),
                        onBack = { state.navigate(AppScreen.DOWNLOADS) },
                        onOpenMedia = { openEntry(context, it) },
                        onShareFile = { shareEntry(context, it) },
                        onViewSource = { entry ->
                            val url = entry.sourceUrl
                            if (url.isNullOrBlank()) {
                                Toast.makeText(
                                    context,
                                    "No source link for this item",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "No app can open this link",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onCopyLink = { entry ->
                            val url = entry.sourceUrl
                            if (url.isNullOrBlank()) {
                                Toast.makeText(
                                    context,
                                    "No source link for this item",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                clipboard.setText(AnnotatedString(url))
                                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onManageCookies = { state.openCookies() },
                        onRemove = { state.removeEntry(it) }
                    )

                    AppScreen.SETTINGS -> SettingsScreen(
                        cookieStatus = state.cookieStatusLabel(),
                        cookiesImported = state.cookiesImported,
                        darkTheme = state.isDarkTheme,
                        storageUsedLabel = if (state.storageUsedBytes > 0L) {
                            formatBytes(state.storageUsedBytes)
                        } else {
                            "0 MB"
                        },
                        onManageCookies = { state.openCookies() },
                        onClearHistory = {
                            state.clearHistory()
                            Toast.makeText(context, "Download records cleared", Toast.LENGTH_SHORT)
                                .show()
                        },
                        onToggleDarkTheme = { state.updateDarkTheme(it) },
                        onShowWelcome = { state.showWelcomeAgain() },
                        onShowPrivacy = { privacyOpen = true },
                        onOpenUpdates = { state.navigate(AppScreen.UPDATE) }
                    )

                    AppScreen.UPDATE -> UpdateScreen(
                        onBack = { state.navigate(AppScreen.SETTINGS) }
                    )
                }
            }

            if (state.screen == AppScreen.HOME ||
                state.screen == AppScreen.DOWNLOADS ||
                state.screen == AppScreen.SETTINGS
            ) {
                BottomNavBar(
                    current = state.screen,
                    onSelect = { target -> state.navigate(target) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        if (state.cookiesOpen) {
            CookiesDialog(
                cookiesImported = state.cookiesImported,
                cookieStatus = state.cookieStatusLabel(),
                checkResult = state.cookieCheckResult,
                onImportClick = { cookiePicker.launch("*/*") },
                onCheckStatus = { state.checkCookieSession() },
                onClearCookies = {
                    state.clearCookies()
                    Toast.makeText(context, "Imported cookies cleared", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { state.closeCookies() }
            )
        }

        if (privacyOpen) {
            PrivacyPolicyDialog(onDismiss = { privacyOpen = false })
        }
    }
}

private fun AppScreen.depth(): Int = when (this) {
    AppScreen.PRIVACY -> 0
    AppScreen.HOME -> 1
    AppScreen.DOWNLOADS, AppScreen.SETTINGS -> 1
    AppScreen.INSTAGRAM -> 2
    AppScreen.HISTORY -> 2
    AppScreen.UPDATE -> 2
}

private data class NavItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun BottomNavBar(
    current: AppScreen,
    onSelect: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(AppScreen.HOME, "Home", Icons.Filled.Home),
        NavItem(AppScreen.DOWNLOADS, "Downloads", QIcon.Download),
        NavItem(AppScreen.SETTINGS, "Settings", Icons.Filled.Settings)
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = QivarynColors.current.Surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = current == item.screen
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(item.screen) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) QivarynColors.current.Mint
                                else QivarynColors.current.Surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = QivarynColors.current.Text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = QivarynColors.current.Text,
                        fontSize = 10.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun openEntry(context: android.content.Context, entry: DownloadEntry) {
    val uri = entryUri(context, entry)
    if (uri == null) {
        Toast.makeText(context, "This download has no local file", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeForEntry(entry))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
        }
}

private fun shareEntry(context: android.content.Context, entry: DownloadEntry) {
    val uri = entryUri(context, entry)
    if (uri == null) {
        Toast.makeText(context, "This download has no local file", Toast.LENGTH_SHORT).show()
        return
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType(mimeForEntry(entry))
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching {
        context.startActivity(Intent.createChooser(send, "Share file"))
    }.onFailure {
        Toast.makeText(context, "No app can share this file", Toast.LENGTH_SHORT).show()
    }
}

private fun entryUri(context: android.content.Context, entry: DownloadEntry): Uri? {
    val content = entry.contentUri
    if (!content.isNullOrBlank()) return Uri.parse(content)
    val path = entry.localPath
    if (path.isNullOrBlank()) return null
    val file = File(path)
    if (!file.exists()) return null
    // Always share through FileProvider: file:// URIs throw
    // FileUriExposedException on Android 7+ when handed to another app.
    return runCatching {
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }.getOrNull()
}

private fun mimeForEntry(entry: DownloadEntry): String =
    entry.mimeType
        ?: entry.localPath?.let { DownloadDestination.mimeForName(it) }
        ?: "application/octet-stream"

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}