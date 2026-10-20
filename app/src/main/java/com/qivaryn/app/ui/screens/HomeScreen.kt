package com.qivaryn.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qivaryn.app.ui.components.BrandGlyph
import com.qivaryn.app.ui.components.Platform
import com.qivaryn.app.ui.theme.QivarynColors

@Composable
fun HomeScreen(
    onOpenInstagram: () -> Unit,
    onPlatformTap: (Platform) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(QivarynColors.current.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 142.dp)
            ) {
                InstagramCard(onClick = onOpenInstagram)
                Spacer(Modifier.height(13.dp))
                ComingSoonCard(Platform.FACEBOOK, onClick = { onPlatformTap(Platform.FACEBOOK) })
                Spacer(Modifier.height(13.dp))
                ComingSoonCard(Platform.TIKTOK, onClick = { onPlatformTap(Platform.TIKTOK) })
                Spacer(Modifier.height(13.dp))
                ComingSoonCard(Platform.YOUTUBE, onClick = { onPlatformTap(Platform.YOUTUBE) })
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 80.dp)
                .size(54.dp)
                .shadow(7.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(QivarynColors.current.Mint)
                .clickable(onClick = onOpenInstagram),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "New download",
                tint = QivarynColors.current.OnMint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QivarynColors.current.Header)
            .statusBarsPadding()
            .height(58.dp)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Qivaryn",
            color = QivarynColors.current.Text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        )
    }
}

@Composable
private fun InstagramCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = QivarynColors.current.Surface,
        border = BorderStroke(2.dp, QivarynColors.current.Teal),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandGlyph(
                platform = Platform.INSTAGRAM,
                tint = QivarynColors.current.BrandGray,
                plate = QivarynColors.current.Surface,
                modifier = Modifier.size(50.dp)
            )
            Spacer(Modifier.height(13.dp))
            Text(
                text = "Instagram",
                color = QivarynColors.current.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ComingSoonCard(platform: Platform, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = QivarynColors.current.ComingSoon,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BrandGlyph(
                platform = platform,
                tint = QivarynColors.current.ComingSoonPill,
                plate = QivarynColors.current.ComingSoon,
                modifier = Modifier
                    .size(64.dp)
                    .offset(y = (-12).dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.offset(y = 2.dp)
            ) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(QivarynColors.current.ComingSoonPill)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "COMING SOON",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.55.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = platform.label,
                    color = QivarynColors.current.ComingSoonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}