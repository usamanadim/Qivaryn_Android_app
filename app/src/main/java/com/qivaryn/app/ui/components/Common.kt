package com.qivaryn.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qivaryn.app.data.DownloadEntry
import com.qivaryn.app.ui.theme.QivarynColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 56.dp
) {
    val shape = RoundedCornerShape(height / 2)
    val background = if (enabled) QivarynColors.current.TealButton else QivarynColors.current.TealButton.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(background)
            .clickable(
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = QivarynColors.current.OnTeal,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = QivarynColors.current.OnTeal,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = QivarynColors.current.Text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.1).sp
    )
}

@Composable
fun QTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(11.dp)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        focused -> QivarynColors.current.Teal
        else -> QivarynColors.current.OutlineStrong
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(QivarynColors.current.Surface)
            .border(1.dp, borderColor, shape),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = TextStyle(
            color = QivarynColors.current.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        ),
        cursorBrush = SolidColor(QivarynColors.current.Teal),
        keyboardOptions = keyboardOptions,
        interactionSource = interaction,
        decorationBox = { innerField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .padding(start = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = QivarynColors.current.TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerField()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        }
    )
}

@Composable
fun HeaderBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleSize: TextUnit = 21.sp,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(QivarynColors.current.Header)
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = if (onBack != null) 10.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = QivarynColors.current.Text,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        Text(
            text = title,
            color = QivarynColors.current.Text,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.padding(start = if (onBack != null) 4.dp else 0.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (action != null) {
            Spacer(Modifier.weight(1f))
            action()
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    container: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    fontSize: Int = 11
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(container)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun QFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(if (selected) QivarynColors.current.TealButton else Color.Transparent)
            .border(1.dp, if (selected) QivarynColors.current.TealButton else QivarynColors.current.TextSecondary, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) QivarynColors.current.OnTeal else QivarynColors.current.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun QSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search"
) {
    val shape = RoundedCornerShape(24.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(QivarynColors.current.Panel),
        singleLine = true,
        textStyle = TextStyle(color = QivarynColors.current.Text, fontSize = 14.sp),
        cursorBrush = SolidColor(QivarynColors.current.Teal),
        decorationBox = { innerField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = QivarynColors.current.Text,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = QivarynColors.current.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    innerField()
                }
            }
        }
    )
}

@Composable
fun DownloadThumbnail(
    entry: DownloadEntry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cacheKey = entry.contentUri ?: entry.localPath ?: entry.id
    val bitmap by produceState<ImageBitmap?>(initialValue = null, cacheKey) {
        value = withContext(Dispatchers.IO) {
            when {
                !entry.contentUri.isNullOrBlank() ->
                    decodeThumbnailUri(context, Uri.parse(entry.contentUri))
                !entry.localPath.isNullOrBlank() ->
                    decodeThumbnail(File(entry.localPath))
                else -> null
            }
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(QivarynColors.current.Panel),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            DecorativeThumbnail(seed = entry.id, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun decodeThumbnailUri(context: Context, uri: Uri): ImageBitmap? {
    return try {
        val mime = context.contentResolver.getType(uri)
        if (mime?.startsWith("video") == true) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.asImageBitmap()
            } finally {
                retriever.release()
            }
        } else {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFileDescriptor(
                    descriptor.fileDescriptor,
                    null,
                    options
                )?.asImageBitmap()
            }
        }
    } catch (_: Throwable) {
        null
    }
}

private fun decodeThumbnail(file: File): ImageBitmap? {
    if (!file.exists() || file.length() == 0L) return null
    val extension = file.extension.lowercase()
    return try {
        when (extension) {
            "mp4", "mov", "webm" -> {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?.asImageBitmap()
                } finally {
                    retriever.release()
                }
            }
            "jpg", "jpeg", "png", "webp" -> {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                var sample = 1
                val target = 256
                while (bounds.outWidth / sample > target || bounds.outHeight / sample > target) {
                    sample *= 2
                }
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
            }
            else -> null
        }
    } catch (_: Throwable) {
        null
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(QivarynColors.current.Outline.copy(alpha = 0.55f))
    )
}

@Composable
fun EmptyDownloads(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(QivarynColors.current.MintSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = QIcon.DownloadOutline,
                contentDescription = null,
                tint = QivarynColors.current.OnMint,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = QivarynColors.current.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            color = QivarynColors.current.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}