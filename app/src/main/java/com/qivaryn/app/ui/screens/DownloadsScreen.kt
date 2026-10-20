package com.qivaryn.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qivaryn.app.data.DownloadEntry
import com.qivaryn.app.data.DownloadFilter
import com.qivaryn.app.data.DownloadStatus
import com.qivaryn.app.data.formatBytes
import com.qivaryn.app.data.formatEntryDate
import com.qivaryn.app.ui.components.BrandGlyph
import com.qivaryn.app.ui.components.DownloadErrorDialog
import com.qivaryn.app.ui.components.DownloadThumbnail
import com.qivaryn.app.ui.components.EmptyDownloads
import com.qivaryn.app.ui.components.HeaderBar
import com.qivaryn.app.ui.components.Platform
import com.qivaryn.app.ui.components.PrimaryButton
import com.qivaryn.app.ui.components.QFilterChip
import com.qivaryn.app.ui.components.QIcon
import com.qivaryn.app.ui.components.StatusPill
import com.qivaryn.app.ui.theme.QivarynColors
import kotlin.math.roundToInt

@Composable
fun DownloadsScreen(
    entries: List<DownloadEntry>,
    filter: DownloadFilter,
    onFilterChange: (DownloadFilter) -> Unit,
    onOpenHistory: () -> Unit,
    onRemove: (String) -> Unit,
    onShare: (DownloadEntry) -> Unit,
    onOpenEntry: (DownloadEntry) -> Unit,
    onManageCookies: () -> Unit,
    onGoInstagram: () -> Unit,
    modifier: Modifier = Modifier
) {
    var errorEntry by remember { mutableStateOf<DownloadEntry?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QivarynColors.current.Background)
    ) {
        HeaderBar(
            title = "Downloads",
            titleSize = 24.sp,
            action = {
                IconButton(onClick = onOpenHistory, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = QIcon.History,
                        contentDescription = "Download history",
                        tint = QivarynColors.current.Text,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            DownloadFilter.entries.forEach { option ->
                QFilterChip(
                    text = option.label,
                    selected = filter == option,
                    onClick = { onFilterChange(option) }
                )
            }
        }

        androidx.compose.material3.HorizontalDivider(
            thickness = 1.dp,
            color = QivarynColors.current.Outline.copy(alpha = 0.7f)
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(QivarynColors.current.Surface)
            ) {
                EmptyDownloads(
                    title = when (filter) {
                        DownloadFilter.ALL -> "No downloads yet"
                        DownloadFilter.ACTIVE -> "Nothing downloading"
                        DownloadFilter.COMPLETED -> "No completed downloads"
                        DownloadFilter.FAILED -> "No failed downloads"
                    },
                    body = "Paste an Instagram link and Qivaryn will save the media to Downloads/Qivaryn.",
                    modifier = Modifier.align(Alignment.Center),
                    action = {
                        PrimaryButton(
                            text = "Start a download",
                            onClick = onGoInstagram,
                            height = 46.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(QivarynColors.current.Surface),
                contentPadding = PaddingValues(bottom = 122.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    DownloadRow(
                        entry = entry,
                        onRemove = { onRemove(entry.id) },
                        onShare = { onShare(entry) },
                        onOpen = {
                            if (entry.errorMessage != null) {
                                errorEntry = entry
                            } else {
                                onOpenEntry(entry)
                            }
                        }
                    )
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

@Composable
private fun DownloadRow(
    entry: DownloadEntry,
    onRemove: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        DownloadThumbnail(
            entry = entry,
            modifier = Modifier.size(56.dp)
        )
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
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 1.dp)
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.platform,
                color = QivarynColors.current.Text,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(5.dp))
            if (entry.isActive) {
                ActiveProgress(entry)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandGlyph(
                        platform = Platform.INSTAGRAM,
                        tint = QivarynColors.current.TextSecondary,
                        plate = QivarynColors.current.Surface,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatEntryDate(entry.timestamp) +
                            if ((entry.totalBytes ?: 0L) > 0L) {
                                "  " + formatBytes(entry.totalBytes)
                            } else {
                                ""
                            },
                        color = QivarynColors.current.TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(5.dp))
                    StatusPillFor(entry, fontSize = 10)
                    if (entry.status == DownloadStatus.COMPLETED) {
                        Spacer(Modifier.width(5.dp))
                        SharePill(onClick = onShare)
                    }
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
private fun ActiveProgress(entry: DownloadEntry) {
    val progress by animateFloatAsState(
        targetValue = entry.progress.coerceIn(0f, 1f),
        label = "download-progress"
    )
    val started = progress > 0.01f
    if (started) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp, bottom = 4.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(QivarynColors.current.PanelDeep)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(QivarynColors.current.Teal)
            )
        }
    } else {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = QivarynColors.current.Teal,
            trackColor = QivarynColors.current.PanelDeep,
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(4.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (started) "${(progress * 100).roundToInt()}%" else "Starting…",
            color = QivarynColors.current.TextSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Downloading…  (" + formatBytes(entry.downloadedBytes) + " / " +
                formatBytes(entry.totalBytes) + ")",
            color = QivarynColors.current.TextSecondary,
            fontSize = 11.5.sp,
            maxLines = 1
        )
    }
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
private fun SharePill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(QivarynColors.current.Mint)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = null,
            tint = QivarynColors.current.OnMint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "Share",
            color = QivarynColors.current.OnMint,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}