package com.qivaryn.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qivaryn.app.ui.theme.QivarynColors

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogFrame(onDismiss = onDismiss, widthPadding = 18.dp) {
            DialogHeader(title = "Privacy Policy", onDismiss = onDismiss)
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = "Qivaryn is a serverless media downloader. Imported cookies and app " +
                        "data stay in this app's private storage, while downloaded media is " +
                        "saved to the public Downloads/Qivaryn folder on this device.",
                    color = QivarynColors.current.Text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Cookies act like your Instagram login credentials. They are used only " +
                        "on-device to request protected media and are never uploaded to a Qivaryn " +
                        "server. If you sign out of Instagram or clear browser data, you must " +
                        "import fresh cookies.",
                    color = QivarynColors.current.Text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Qivaryn does not collect analytics, passwords, or personal files. " +
                        "Uninstalling the app removes imported cookies and app data, but files " +
                        "already saved to Downloads/Qivaryn stay on your device until you " +
                        "delete them.",
                    color = QivarynColors.current.Text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(22.dp))
                PrimaryButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CookiesDialog(
    cookiesImported: Boolean,
    cookieStatus: String,
    checkResult: String?,
    onImportClick: () -> Unit,
    onCheckStatus: () -> Unit,
    onClearCookies: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogFrame(onDismiss = onDismiss, widthPadding = 14.dp) {
            DialogHeader(title = "Manage Cookies", onDismiss = onDismiss)
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Cookies act like your Instagram login credentials.",
                    color = QivarynColors.current.Text,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Qivaryn uses them to access protected content on-device. They are not " +
                        "stored on any server.\nIf you sign out of Instagram or clear browser data, " +
                        "you must re-import.",
                    color = QivarynColors.current.Text,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Only instagram.com cookies are kept. Cookies for other sites in " +
                        "the same export are discarded.",
                    color = QivarynColors.current.TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .clip(RoundedCornerShape(33.dp))
                        .background(QivarynColors.current.TealButton)
                        .clickable(onClick = onImportClick)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = QIcon.Upload,
                            contentDescription = null,
                            tint = QivarynColors.current.OnTeal,
                            modifier = Modifier.size(23.dp)
                        )
                        Spacer(Modifier.width(15.dp))
                        Column {
                            Text(
                                text = "Import cookies.txt file",
                                color = QivarynColors.current.OnTeal,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "(via System File Picker)",
                                color = QivarynColors.current.OnTeal.copy(alpha = 0.9f),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                DialogActionRow(
                    icon = {
                        Icon(
                            imageVector = QIcon.Sync,
                            contentDescription = null,
                            tint = QivarynColors.current.TextSecondary,
                            modifier = Modifier.size(23.dp)
                        )
                    },
                    title = "Check Cookie Status",
                    subtitle = "(Current Status: " + cookieStatus + ")",
                    onClick = onCheckStatus
                )
                Spacer(Modifier.height(12.dp))
                DialogActionRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = QivarynColors.current.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = "Clear Imported Cookies",
                    subtitle = "(Sign Out)",
                    onClick = onClearCookies
                )
                Spacer(Modifier.height(16.dp))
                if (!checkResult.isNullOrBlank()) {
                    Text(
                        text = checkResult,
                        color = QivarynColors.current.TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (cookiesImported) {
                    Text(
                        text = "Cookies are stored only in this app's private folder.",
                        color = QivarynColors.current.TextSecondary,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun DownloadErrorDialog(
    message: String,
    showCookieAction: Boolean,
    onManageCookies: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogFrame(onDismiss = onDismiss, widthPadding = 18.dp) {
            DialogHeader(title = "Download failed", onDismiss = onDismiss)
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = message,
                    color = QivarynColors.current.Text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(20.dp))
                if (showCookieAction) {
                    PrimaryButton(
                        text = "Manage cookies",
                        onClick = {
                            onDismiss()
                            onManageCookies()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(QivarynColors.current.Panel)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close",
                        color = QivarynColors.current.Text,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DialogFrame(
    onDismiss: () -> Unit,
    widthPadding: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QivarynColors.current.Scrim)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = widthPadding, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = QivarynColors.current.SurfaceAlt,
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(30.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DialogHeader(title: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = title,
            color = QivarynColors.current.Text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = QivarynColors.current.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DialogActionRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(31.dp))
            .background(QivarynColors.current.Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(15.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                color = QivarynColors.current.Text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = QivarynColors.current.TextSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}