package com.qivaryn.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qivaryn.app.data.DownloadScope
import com.qivaryn.app.ui.components.HeaderBar
import com.qivaryn.app.ui.components.PrimaryButton
import com.qivaryn.app.ui.components.QIcon
import com.qivaryn.app.ui.components.QTextField
import com.qivaryn.app.ui.components.SectionLabel
import com.qivaryn.app.ui.theme.QivarynColors

private data class MediaTypeOption(
    val label: String,
    val icon: ImageVector
)

// Story and Highlights are not exposed because the Python engine has no
// endpoint for those feeds; selecting them would silently download the whole
// profile instead.
private val mediaTypes = listOf(
    MediaTypeOption("Post", QIcon.Photo),
    MediaTypeOption("Reel", QIcon.Reel),
    MediaTypeOption("Profile", QIcon.Profile)
)

private data class ScopeOption(
    val scope: DownloadScope,
    val label: String,
    val icon: ImageVector
)

private val scopeOptions = listOf(
    ScopeOption(DownloadScope.SINGLE, "Single", QIcon.Photo),
    ScopeOption(DownloadScope.ALL_REELS, "All Reels", QIcon.Reel),
    ScopeOption(DownloadScope.ALL_POSTS, "All Posts", QIcon.Grid)
)

@Composable
fun InstagramScreen(
    cookiesImported: Boolean,
    hasActiveDownload: Boolean,
    onBack: () -> Unit,
    onManageCookies: () -> Unit,
    onDownload: (url: String, scope: DownloadScope, mediaType: String, count: Int?) -> String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var url by rememberSaveable { mutableStateOf("") }
    var mediaCount by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("Post") }
    var selectedScope by rememberSaveable { mutableStateOf(DownloadScope.SINGLE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QivarynColors.current.Background)
            .imePadding()
    ) {
        HeaderBar(
            title = "Download from Instagram",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            SectionLabel("Paste Instagram URL:")
            Spacer(Modifier.height(8.dp))
            QTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = "Paste Instagram URL:",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                trailing = {
                    Text(
                        text = "PASTE",
                        color = QivarynColors.current.Teal,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val pasted = clipboard.getText()?.text.orEmpty()
                                if (pasted.isBlank()) {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    url = pasted.trim()
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            )
            Spacer(Modifier.height(20.dp))
            SectionLabel("Download scope")
            Spacer(Modifier.height(8.dp))
            ScopeStrip(
                selected = selectedScope,
                onSelect = { selectedScope = it }
            )

            if (selectedScope == DownloadScope.SINGLE) {
                Spacer(Modifier.height(18.dp))
                MediaTypeStrip(
                    selected = selectedType,
                    onSelect = { selectedType = it }
                )
            } else {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (selectedScope == DownloadScope.ALL_REELS) {
                        "Downloads every reel from this account into Downloads/Qivaryn."
                    } else {
                        "Downloads every post, video and carousel from this account into Downloads/Qivaryn."
                    },
                    color = QivarynColors.current.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Media Count (Optional):")
            Spacer(Modifier.height(8.dp))
            QTextField(
                value = mediaCount,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } && value.length <= 5) mediaCount = value
                },
                placeholder = "Media Count (Optional):",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(28.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(QivarynColors.current.Background)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            CookiesPanel(
                cookiesImported = cookiesImported,
                onManageCookies = onManageCookies
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = if (hasActiveDownload) "Download in progress…" else "Download",
                enabled = !hasActiveDownload,
                onClick = {
                    if (url.isBlank()) {
                        Toast.makeText(context, "Paste an Instagram URL first", Toast.LENGTH_SHORT)
                            .show()
                        return@PrimaryButton
                    }
                    val count = mediaCount.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                    if (mediaCount.isNotBlank() && count == null) {
                        Toast.makeText(context, "Enter a valid media count", Toast.LENGTH_SHORT).show()
                        return@PrimaryButton
                    }
                    val error = onDownload(url, selectedScope, selectedType, count)
                    if (error != null) {
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun CookiesPanel(
    cookiesImported: Boolean,
    onManageCookies: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(QivarynColors.current.Panel)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Text(
            text = "Cookies:",
            color = QivarynColors.current.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint = QivarynColors.current.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (cookiesImported) "Imported." else "Not Imported.",
                color = QivarynColors.current.Text,
                fontSize = 13.5.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onManageCookies)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = QIcon.Key,
                contentDescription = null,
                tint = QivarynColors.current.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = if (cookiesImported) "Manage cookies.txt" else "Import from cookies.txt",
                    color = QivarynColors.current.Teal,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (cookiesImported) "(stored on this device)" else "(opens file picker)",
                    color = QivarynColors.current.Teal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ScopeStrip(
    selected: DownloadScope,
    onSelect: (DownloadScope) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(QivarynColors.current.Segment)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        scopeOptions.forEach { option ->
            val isSelected = option.scope == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) QivarynColors.current.MintStrong
                        else QivarynColors.current.Segment
                    )
                    .clickable { onSelect(option.scope) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = if (isSelected) QivarynColors.current.OnMint
                    else QivarynColors.current.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = option.label,
                    color = if (isSelected) QivarynColors.current.OnMint
                    else QivarynColors.current.TextSecondary,
                    fontSize = 9.8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MediaTypeStrip(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(QivarynColors.current.Segment)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        mediaTypes.forEach { option ->
            val isSelected = option.label == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) QivarynColors.current.MintStrong else QivarynColors.current.Segment)
                    .clickable { onSelect(option.label) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = if (isSelected) QivarynColors.current.OnMint else QivarynColors.current.TextSecondary,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = option.label,
                    color = if (isSelected) QivarynColors.current.OnMint else QivarynColors.current.TextSecondary,
                    fontSize = if (option.label == "Highlights") 8.6.sp else 9.8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}