package com.qivaryn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qivaryn.app.ui.components.PrimaryButton
import com.qivaryn.app.ui.components.PrivacyPolicyDialog
import com.qivaryn.app.ui.components.QivarynLogo
import com.qivaryn.app.ui.theme.QivarynColors

@Composable
fun PrivacyScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var policyOpen by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(QivarynColors.current.BackgroundTop, QivarynColors.current.Background, QivarynColors.current.Panel)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(scroll)
                    .padding(horizontal = 26.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                QivarynLogo(modifier = Modifier.size(122.dp))
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Welcome to Qivaryn.",
                    color = QivarynColors.current.Text,
                    fontSize = 27.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Serverless media downloader.",
                    color = QivarynColors.current.Text,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(15.dp))
                Text(
                    text = "All downloads and imported cookies stay only on this device. " +
                        "We do not store your passwords or connect to external servers.",
                    color = QivarynColors.current.Text,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Spacer(Modifier.height(24.dp))
                val policyText = AnnotatedString.Builder(
                    "Cookies are sensitive. Only import them if needed. "
                ).apply {
                    pushStyle(
                        SpanStyle(
                            color = QivarynColors.current.Text,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                    append("(Read Privacy Policy)")
                    pop()
                }.toAnnotatedString()
                ClickableText(
                    text = policyText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = QivarynColors.current.Text,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    ),
                    onClick = { policyOpen = true },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        PrimaryButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
        )
        Spacer(Modifier.height(18.dp))
    }

    if (policyOpen) {
        PrivacyPolicyDialog(onDismiss = { policyOpen = false })
    }
}