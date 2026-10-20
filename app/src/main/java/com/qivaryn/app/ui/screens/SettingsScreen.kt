package com.qivaryn.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qivaryn.app.BuildConfig
import com.qivaryn.app.ui.components.BrandGlyph
import com.qivaryn.app.ui.components.HeaderBar
import com.qivaryn.app.ui.components.Platform
import com.qivaryn.app.ui.components.QIcon
import com.qivaryn.app.ui.components.SectionLabel
import com.qivaryn.app.ui.theme.QivarynColors
import com.qivaryn.app.update.UpdateManager

@Composable
fun SettingsScreen(
    cookieStatus: String,
    cookiesImported: Boolean,
    darkTheme: Boolean,
    storageUsedLabel: String,
    onManageCookies: () -> Unit,
    onClearHistory: () -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onShowWelcome: () -> Unit,
    onShowPrivacy: () -> Unit,
    onOpenUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QivarynColors.current.Background)
    ) {
        HeaderBar(title = "Settings", titleSize = 24.sp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 132.dp)
        ) {
            SettingsSection(label = "DOWNLOADING") {
                SettingsRow(
                    icon = {
                        Icon(
                            imageVector = QIcon.Cookie,
                            contentDescription = null,
                            tint = QivarynColors.current.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = "Manage cookies",
                    subtitle = if (cookiesImported) cookieStatus else "Not imported",
                    onClick = onManageCookies,
                    trailingChevron = true
                )
                RowDivider()
                SettingsRow(
                    icon = {
                        Icon(
                            imageVector = QIcon.DownloadOutline,
                            contentDescription = null,
                            tint = QivarynColors.current.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = "Local storage",
                    subtitle = storageUsedLabel + " in Downloads/Qivaryn"
                )
            }

            Spacer(Modifier.height(22.dp))
            SettingsSection(label = "APPEARANCE") {
                SettingsRow(
                    icon = {
                        Icon(
                            imageVector = QIcon.Moon,
                            contentDescription = null,
                            tint = QivarynColors.current.TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    title = "Dark mode",
                    subtitle = if (darkTheme) "On · dark surfaces and light text" else "Off · warm paper theme",
                    trailing = {
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = QivarynColors.current.White,
                                checkedTrackColor = QivarynColors.current.TealButton,
                                uncheckedThumbColor = QivarynColors.current.White,
                                uncheckedTrackColor = QivarynColors.current.OutlineStrong,
                                uncheckedBorderColor = QivarynColors.current.OutlineStrong
                            )
                        )
                    }
                )
            }

            Spacer(Modifier.height(22.dp))
            SettingsSection(label = "HISTORY") {
                SettingsRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = QivarynColors.current.TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    title = "Clear download records",
                    subtitle = "Media files are never deleted",
                    onClick = onClearHistory
                )
            }

            Spacer(Modifier.height(22.dp))
            SettingsSection(label = "UPDATES") {
                SettingsRow(
                    icon = {
                        Icon(
                            imageVector = QIcon.Sync,
                            contentDescription = null,
                            tint = QivarynColors.current.TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    title = "Software update",
                    subtitle = "Check GitHub for a newer Qivaryn " +
                        UpdateManager.channel.description,
                    onClick = onOpenUpdates,
                    trailingChevron = true
                )
            }

            Spacer(Modifier.height(22.dp))
            SettingsSection(label = "ABOUT") {
                SettingsRow(
                    icon = {
                        BrandGlyph(
                            platform = Platform.INSTAGRAM,
                            tint = QivarynColors.current.TextSecondary,
                            plate = QivarynColors.current.Surface,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    title = "Instagram engine",
                    subtitle = "Python · on-device downloads"
                )
                RowDivider()
                SettingsRow(
                    icon = {
                        Icon(
                            imageVector = QIcon.History,
                            contentDescription = null,
                            tint = QivarynColors.current.TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    title = "Show welcome screen",
                    subtitle = "Revisit the privacy introduction",
                    onClick = onShowWelcome,
                    trailingChevron = true
                )
                RowDivider()
                SettingsRow(
                    icon = {
                        Icon(
                            imageVector = QIcon.Shield,
                            contentDescription = null,
                            tint = QivarynColors.current.TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    title = "Privacy policy",
                    subtitle = "How cookies and media stay on-device",
                    onClick = onShowPrivacy,
                    trailingChevron = true
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "Qivaryn " + BuildConfig.VERSION_NAME + "  ·  serverless by design",
                color = QivarynColors.current.TextMuted,
                fontSize = 11.5.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsSection(
    label: String,
    content: @Composable () -> Unit
) {
    Column {
        SectionLabel(text = label, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = QivarynColors.current.Surface,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailingChevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = QivarynColors.current.Text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = QivarynColors.current.TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (trailingChevron) {
            Icon(
                imageVector = QIcon.ChevronRight,
                contentDescription = null,
                tint = QivarynColors.current.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RowDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 55.dp)
            .height(1.dp)
            .background(QivarynColors.current.Outline.copy(alpha = 0.5f))
    )
}