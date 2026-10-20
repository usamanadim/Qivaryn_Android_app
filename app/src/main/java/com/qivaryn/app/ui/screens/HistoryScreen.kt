package com.qivaryn.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qivaryn.app.data.DownloadEntry
import com.qivaryn.app.data.DownloadStatus
import com.qivaryn.app.data.formatBytes
import com.qivaryn.app.data.formatEntryDate
import com.qivaryn.app.ui.components.BrandGlyph
import com.qivaryn.app.ui.components.DownloadErrorDialog
import com.qivaryn.app.ui.components.DownloadThumbnail
import com.qivaryn.app.ui.components.EmptyDownloads
import com.qivaryn.app.ui.components.HeaderBar
import com.qivaryn.app.ui.components.Platform
import com.qivaryn.app.ui.components.QIcon
import com.qivaryn.app.ui.components.QSearchField
import com.qivaryn.app.ui.components.StatusPill
import com.qivaryn.app.ui.theme.QivarynColors
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    entries: List<DownloadEntry>,
    onBack: () -> Unit,
    onOpenMedia: (DownloadEntry) -> Unit,
    onShareFile: (DownloadEntry) -> Unit,
    onViewSource: (DownloadEntry) -> Unit,
    onCopyLink: (DownloadEntry) -> Unit,
    onManageCookies: () -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var menuEntryId by remember { mutableStateOf<String?>(null) }
    var errorEntry by remember { mutableStateOf<DownloadEntry?>(null) }

    val filtered = remember(entries, query) {
        if (query.isBlank()) entries else entries.filter { entry ->
            entry.title.contains(query, ignoreCase = true) ||
                entry.platform.contains(query, ignoreCase = true) ||
                formatEntryDate(entry.timestamp).contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QivarynColors.current.Background)
    ) {
        HeaderBar(
            title = "History",
            titleSize = 24.sp,
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            QSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search"
            )
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(QivarynColors.current.Surface)
            ) {
                EmptyDownloads(
                    title = if (query.isBlank()) "No download history" else "No matching files",
                    body = if (query.isBlank()) {
                        "Downloads you complete will appear here with their file details."
                    } else {
                        "Try a different file name, platform, or date."
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(QivarynColors.current.Surface)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                items(filtered, key = { it.id }) { entry ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HistoryRow(
                            entry = entry,
                            onRemove = { onRemove(entry.id) },
                            onOpen = {
                                if (entry.errorMessage != null) {
                                    errorEntry = entry
                                } else {
                                    onOpenMedia(entry)
                                }
                            },
                            onLongPress = { menuEntryId = entry.id }
                        )
                        HistoryMenu(
                            expanded = menuEntryId == entry.id,
                            onDismiss = { menuEntryId = null },
                            onOpenMedia = {
                                menuEntryId = null
                                onOpenMedia(entry)
                            },
                            onShareFile = {
                                menuEntryId = null
                                onShareFile(entry)
                            },
                            onViewSource = {
                                menuEntryId = null
                                onViewSource(entry)
                            },
                            onCopyLink = {
                                menuEntryId = null
                                onCopyLink(entry)
                            }
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(
                        thickness = 1.dp,
                        color = QivarynColors.current.Outline.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }

    errorEntry?.let { entry ->
        val message = entry.errorMessage ?: "This download could not be completed."
        val lower = message.lowercase()
        DownloadErrorDialog(
            message = message,
            showCookieAction = "cookie" in lower || "session" in lower ||
                "instagram rejected" in lower || "rate-limit" in lower ||
                "private" in lower,
            onManageCookies = onManageCookies,
            onDismiss = { errorEntry = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    entry: DownloadEntry,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongPress
            )
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        DownloadThumbnail(entry = entry, modifier = Modifier.size(56.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = entry.title,
                    color = QivarynColors.current.Text,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (entry.isActive) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss download",
                            tint = QivarynColors.current.Text,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
            if (entry.isActive) {
                ActiveHistoryProgress(entry)
            } else {
                Text(
                    text = entry.platform,
                    color = QivarynColors.current.Text,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandGlyph(
                        platform = Platform.INSTAGRAM,
                        tint = QivarynColors.current.TextSecondary,
                        plate = QivarynColors.current.Surface,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    val fileSize = formatBytes(entry.totalBytes ?: entry.downloadedBytes)
                    Text(
                        text = formatEntryDate(entry.timestamp) +
                            if (fileSize != "—") "   " + fileSize else "",
                        color = QivarynColors.current.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(5.dp))
                    StatusPillFor(entry, fontSize = 10)
                }
                if (entry.errorMessage != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.errorMessage,
                        color = QivarynColors.current.Danger,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveHistoryProgress(entry: DownloadEntry) {
    Spacer(Modifier.height(5.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(QivarynColors.current.PanelDeep)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(entry.progress.coerceIn(0f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(QivarynColors.current.Teal)
        )
    }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${(entry.progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
            color = QivarynColors.current.TextSecondary,
            fontSize = 11.5.sp
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = formatBytes(entry.totalBytes),
            color = QivarynColors.current.TextSecondary,
            fontSize = 11.5.sp
        )
    }
    Text(
        text = "Downloading…  (" + formatBytes(entry.downloadedBytes) + " / " +
            formatBytes(entry.totalBytes) + ")",
        color = QivarynColors.current.TextSecondary,
        fontSize = 11.5.sp,
        maxLines = 1
    )
}

@Composable
private fun StatusPillFor(entry: DownloadEntry, fontSize: Int = 11) {
    when (entry.status) {
        DownloadStatus.COMPLETED -> StatusPill(
            text = "Completed",
            container = QivarynColors.current.SuccessBg,
            contentColor = QivarynColors.current.Success,
            fontSize = fontSize
        )
        DownloadStatus.FAILED -> StatusPill(
            text = "Failed",
            container = QivarynColors.current.DangerBg,
            contentColor = QivarynColors.current.Danger,
            fontSize = fontSize
        )
        DownloadStatus.EXPIRED -> StatusPill(
            text = "Expired",
            container = QivarynColors.current.DangerBg,
            contentColor = QivarynColors.current.Danger,
            fontSize = fontSize
        )
        DownloadStatus.FILE_REMOVED -> StatusPill(
            text = "File Removed",
            container = QivarynColors.current.DangerBg,
            contentColor = QivarynColors.current.Danger,
            fontSize = fontSize
        )
        DownloadStatus.DOWNLOADING -> StatusPill(
            text = "Active",
            container = QivarynColors.current.Mint,
            contentColor = QivarynColors.current.OnMint,
            fontSize = fontSize
        )
    }
}

@Composable
private fun HistoryMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpenMedia: () -> Unit,
    onShareFile: () -> Unit,
    onViewSource: () -> Unit,
    onCopyLink: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 136.dp, y = 82.dp),
        modifier = Modifier
            .width(204.dp)
            .background(QivarynColors.current.SurfaceAlt)
    ) {
        MenuItem("Open Media", QIcon.Photo, onOpenMedia)
        MenuItem("Share File", Icons.Filled.Share, onShareFile)
        MenuItem("View Source", QIcon.Globe, onViewSource)
        MenuItem("Copy Link", QIcon.Link, onCopyLink)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun MenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = QivarynColors.current.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = QivarynColors.current.Text,
                modifier = Modifier.size(19.dp)
            )
        }
    )
}