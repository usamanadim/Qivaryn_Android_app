package com.qivaryn.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Central design tokens for Qivaryn. Every screen reads the active palette
 * through [QivarynColors] so light/dark mode stays consistent and maintainable.
 */
@Immutable
data class QivarynPalette(
    val Teal: Color,
    val TealDark: Color,
    val TealInk: Color,
    val TealButton: Color,
    val OnTeal: Color,
    val OnMint: Color,
    val Mint: Color,
    val MintSoft: Color,
    val MintStrong: Color,
    val Cream: Color,
    val Background: Color,
    val BackgroundTop: Color,
    val Header: Color,
    val Surface: Color,
    val SurfaceAlt: Color,
    val Panel: Color,
    val PanelDeep: Color,
    val Segment: Color,
    val Text: Color,
    val TextSecondary: Color,
    val TextMuted: Color,
    val Outline: Color,
    val OutlineStrong: Color,
    val ComingSoon: Color,
    val ComingSoonPill: Color,
    val ComingSoonText: Color,
    val BrandGray: Color,
    val LogoGray: Color,
    val Success: Color,
    val SuccessBg: Color,
    val Danger: Color,
    val DangerBg: Color,
    val Amber: Color,
    val AmberBg: Color,
    val Scrim: Color,
    val White: Color,
    val ThumbTints: List<Pair<Color, Color>>
)

val LightQivarynColors = QivarynPalette(
    Teal = Color(0xFF005C64),
    TealDark = Color(0xFF00474D),
    TealInk = Color(0xFF083A3D),
    TealButton = Color(0xFF005C64),
    OnTeal = Color(0xFFFFFFFF),
    OnMint = Color(0xFF005C64),
    Mint = Color(0xFFCFE1DA),
    MintSoft = Color(0xFFDDEBE5),
    MintStrong = Color(0xFFC0D5CC),
    Cream = Color(0xFFF8F5EE),
    Background = Color(0xFFF7F4ED),
    BackgroundTop = Color(0xFFFBF8F2),
    Header = Color(0xFFF0ECE4),
    Surface = Color(0xFFFCFAF5),
    SurfaceAlt = Color(0xFFF9F6F0),
    Panel = Color(0xFFEDE9E1),
    PanelDeep = Color(0xFFE7E2D9),
    Segment = Color(0xFFEAE6DE),
    Text = Color(0xFF1D1C18),
    TextSecondary = Color(0xFF64625B),
    TextMuted = Color(0xFF8A877E),
    Outline = Color(0xFFD7D2C8),
    OutlineStrong = Color(0xFFBBB6AB),
    ComingSoon = Color(0xFFB7BBB5),
    ComingSoonPill = Color(0xFF8E918B),
    ComingSoonText = Color(0xFF585B56),
    BrandGray = Color(0xFF8C8D87),
    LogoGray = Color(0xFFA9A398),
    Success = Color(0xFF0A5B4C),
    SuccessBg = Color(0xFFCBE4DB),
    Danger = Color(0xFF9A342C),
    DangerBg = Color(0xFFF8D2CD),
    Amber = Color(0xFF8A5A12),
    AmberBg = Color(0xFFF1E2C6),
    Scrim = Color(0xB326241F),
    White = Color(0xFFFFFFFF),
    ThumbTints = listOf(
        Color(0xFF6F8F84) to Color(0xFFC9D6C3),
        Color(0xFF8B6F5A) to Color(0xFFE4CDB4),
        Color(0xFF687E92) to Color(0xFFC8D9E2),
        Color(0xFF7B7462) to Color(0xFFD8D0BC)
    )
)

val DarkQivarynColors = QivarynPalette(
    Teal = Color(0xFF4FC7C4),
    TealDark = Color(0xFF2FA9A6),
    TealInk = Color(0xFFB8F1ED),
    TealButton = Color(0xFF0B6F6B),
    OnTeal = Color(0xFFFFFFFF),
    OnMint = Color(0xFFC9F4EF),
    Mint = Color(0xFF3A5C56),
    MintSoft = Color(0xFF2B4742),
    MintStrong = Color(0xFF4A6B64),
    Cream = Color(0xFFF8F5EE),
    Background = Color(0xFF121311),
    BackgroundTop = Color(0xFF171815),
    Header = Color(0xFF1B1C18),
    Surface = Color(0xFF1E1F1B),
    SurfaceAlt = Color(0xFF242521),
    Panel = Color(0xFF2A2B26),
    PanelDeep = Color(0xFF33342E),
    Segment = Color(0xFF2D2E28),
    Text = Color(0xFFF1EEE7),
    TextSecondary = Color(0xFFB7B1A5),
    TextMuted = Color(0xFF8F897E),
    Outline = Color(0xFF3A3B35),
    OutlineStrong = Color(0xFF4C4D46),
    ComingSoon = Color(0xFF45463F),
    ComingSoonPill = Color(0xFF62645C),
    ComingSoonText = Color(0xFFB7B8AF),
    BrandGray = Color(0xFF9A9B93),
    LogoGray = Color(0xFFB0A99D),
    Success = Color(0xFF7FE0C4),
    SuccessBg = Color(0xFF1C3B32),
    Danger = Color(0xFFFFB4A8),
    DangerBg = Color(0xFF3D211E),
    Amber = Color(0xFFF1C27D),
    AmberBg = Color(0xFF3E301B),
    Scrim = Color(0xCC000000),
    White = Color(0xFFFFFFFF),
    ThumbTints = listOf(
        Color(0xFF3E5D54) to Color(0xFF182A26),
        Color(0xFF5D4A3C) to Color(0xFF2C2018),
        Color(0xFF405A6C) to Color(0xFF172630),
        Color(0xFF55503F) to Color(0xFF29261B)
    )
)

val LocalQivarynColors = staticCompositionLocalOf { LightQivarynColors }

object QivarynColors {
    val current: QivarynPalette
        @Composable get() = LocalQivarynColors.current
}