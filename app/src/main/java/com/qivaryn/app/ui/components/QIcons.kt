package com.qivaryn.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Small set of plan-style icons used across the app. Material's bundled core
 * set does not include download, key, cookie, reel, or link glyphs, so these
 * are defined locally with the same 24x24 grid as Material icons.
 */
object QIcon {

    private fun build(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(block).build()

    private fun ImageVector.Builder.stroke(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )

    private fun ImageVector.Builder.fill(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        path(fill = SolidColor(Color.Black), pathBuilder = block)

    val Download: ImageVector = build("Download") {
        fill {
            moveTo(19f, 9f)
            horizontalLineTo(15f)
            verticalLineTo(3f)
            horizontalLineTo(9f)
            verticalLineTo(9f)
            horizontalLineTo(5f)
            lineTo(12f, 16f)
            close()
            moveTo(5f, 18f)
            horizontalLineTo(19f)
            verticalLineTo(20f)
            horizontalLineTo(5f)
            close()
        }
    }

    val DownloadOutline: ImageVector = build("DownloadOutline") {
        stroke {
            moveTo(12f, 3.5f)
            verticalLineTo(15f)
            moveTo(7.2f, 10.2f)
            lineTo(12f, 15f)
            lineTo(16.8f, 10.2f)
            moveTo(4.5f, 15.5f)
            verticalLineTo(18f)
            quadTo(4.5f, 19.5f, 6f, 19.5f)
            horizontalLineTo(18f)
            quadTo(19.5f, 19.5f, 19.5f, 18f)
            verticalLineTo(15.5f)
        }
    }

    val Key: ImageVector = build("Key") {
        fill {
            moveTo(12.65f, 10f)
            curveTo(11.83f, 7.67f, 9.61f, 6f, 7f, 6f)
            curveTo(3.69f, 6f, 1f, 8.69f, 1f, 12f)
            curveTo(1f, 15.31f, 3.69f, 18f, 7f, 18f)
            curveTo(9.61f, 18f, 11.83f, 16.33f, 12.65f, 14f)
            horizontalLineTo(17f)
            verticalLineTo(18f)
            horizontalLineTo(21f)
            verticalLineTo(14f)
            horizontalLineTo(23f)
            verticalLineTo(10f)
            close()
            moveTo(7f, 14f)
            curveTo(5.9f, 14f, 5f, 13.1f, 5f, 12f)
            curveTo(5f, 10.9f, 5.9f, 10f, 7f, 10f)
            curveTo(8.1f, 10f, 9f, 10.9f, 9f, 12f)
            curveTo(9f, 13.1f, 8.1f, 14f, 7f, 14f)
            close()
        }
    }

    val Upload: ImageVector = build("Upload") {
        fill {
            moveTo(9f, 16f)
            horizontalLineTo(15f)
            verticalLineTo(10f)
            horizontalLineTo(19f)
            lineTo(12f, 3f)
            lineTo(5f, 10f)
            horizontalLineTo(9f)
            close()
            moveTo(5f, 18f)
            horizontalLineTo(19f)
            verticalLineTo(20f)
            horizontalLineTo(5f)
            close()
        }
    }

    val Photo: ImageVector = build("Photo") {
        stroke {
            moveTo(4f, 5.5f)
            quadTo(4f, 4f, 5.5f, 4f)
            horizontalLineTo(18.5f)
            quadTo(20f, 4f, 20f, 5.5f)
            verticalLineTo(18.5f)
            quadTo(20f, 20f, 18.5f, 20f)
            horizontalLineTo(5.5f)
            quadTo(4f, 20f, 4f, 18.5f)
            close()
            moveTo(5f, 16.5f)
            lineTo(9.2f, 11.8f)
            lineTo(12.4f, 15.1f)
            lineTo(14.7f, 12.5f)
            lineTo(19f, 17.2f)
            moveTo(15.7f, 8.3f)
            moveTo(15.7f, 8.3f)
        }
        fill {
            moveToRelative(15.3f, 6.6f)
            arcTo(1.6f, 1.6f, 0f, true, true, 18.5f, 6.6f)
            arcTo(1.6f, 1.6f, 0f, true, true, 15.3f, 6.6f)
            close()
        }
    }

    val Grid: ImageVector = build("Grid") {
        fill {
            moveTo(3f, 3f)
            horizontalLineTo(10.5f)
            verticalLineTo(10.5f)
            horizontalLineTo(3f)
            close()
            moveTo(13.5f, 3f)
            horizontalLineTo(21f)
            verticalLineTo(10.5f)
            horizontalLineTo(13.5f)
            close()
            moveTo(3f, 13.5f)
            horizontalLineTo(10.5f)
            verticalLineTo(21f)
            horizontalLineTo(3f)
            close()
            moveTo(13.5f, 13.5f)
            horizontalLineTo(21f)
            verticalLineTo(21f)
            horizontalLineTo(13.5f)
            close()
        }
    }

    val Reel: ImageVector = build("Reel") {
        fill {
            moveTo(18f, 4f)
            lineTo(20f, 8f)
            horizontalLineTo(17f)
            lineTo(15f, 4f)
            horizontalLineTo(13f)
            lineTo(15f, 8f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            horizontalLineTo(8f)
            lineTo(10f, 8f)
            horizontalLineTo(7f)
            lineTo(5f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
            verticalLineTo(18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(4f)
            horizontalLineTo(18f)
            close()
        }
    }

    val Story: ImageVector = build("Story") {
        stroke {
            moveTo(12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
        }
        fill {
            moveTo(10f, 8.5f)
            lineTo(16f, 12f)
            lineTo(10f, 15.5f)
            close()
        }
    }

    val Highlights: ImageVector = build("Highlights") {
        stroke {
            moveTo(12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            close()
        }
        fill {
            moveTo(9f, 7f)
            horizontalLineTo(15f)
            verticalLineTo(17.2f)
            lineTo(12f, 15.3f)
            lineTo(9f, 17.2f)
            close()
        }
    }

    val Profile: ImageVector = build("Profile") {
        fill {
            moveTo(12f, 12f)
            curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
            curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
            curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
            curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
            close()
            moveTo(12f, 14f)
            curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
            verticalLineTo(20f)
            horizontalLineTo(20f)
            verticalLineTo(18f)
            curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f)
            close()
        }
    }

    val Globe: ImageVector = build("Globe") {
        stroke {
            moveTo(12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            close()
            moveTo(3.6f, 9f)
            horizontalLineTo(20.4f)
            moveTo(3.6f, 15f)
            horizontalLineTo(20.4f)
            moveTo(12f, 3f)
            curveTo(9.5f, 5.5f, 8.5f, 8.6f, 8.5f, 12f)
            curveTo(8.5f, 15.4f, 9.5f, 18.5f, 12f, 21f)
            moveTo(12f, 3f)
            curveTo(14.5f, 5.5f, 15.5f, 8.6f, 15.5f, 12f)
            curveTo(15.5f, 15.4f, 14.5f, 18.5f, 12f, 21f)
        }
    }

    val Link: ImageVector = build("Link") {
        fill {
            moveTo(3.9f, 12f)
            curveTo(3.9f, 10.29f, 5.29f, 8.9f, 7f, 8.9f)
            horizontalLineTo(11f)
            verticalLineTo(7f)
            horizontalLineTo(7f)
            curveTo(4.24f, 7f, 2f, 9.24f, 2f, 12f)
            curveTo(2f, 14.76f, 4.24f, 17f, 7f, 17f)
            horizontalLineTo(11f)
            verticalLineTo(15.1f)
            horizontalLineTo(7f)
            curveTo(5.29f, 15.1f, 3.9f, 13.71f, 3.9f, 12f)
            close()
            moveTo(8f, 13f)
            horizontalLineTo(16f)
            verticalLineTo(11f)
            horizontalLineTo(8f)
            close()
            moveTo(17f, 7f)
            horizontalLineTo(13f)
            verticalLineTo(8.9f)
            horizontalLineTo(17f)
            curveTo(18.71f, 8.9f, 20.1f, 10.29f, 20.1f, 12f)
            curveTo(20.1f, 13.71f, 18.71f, 15.1f, 17f, 15.1f)
            horizontalLineTo(13f)
            verticalLineTo(17f)
            horizontalLineTo(17f)
            curveTo(19.76f, 17f, 22f, 14.76f, 22f, 12f)
            curveTo(22f, 9.24f, 19.76f, 7f, 17f, 7f)
            close()
        }
    }

    val OpenInNew: ImageVector = build("OpenInNew") {
        fill {
            moveTo(19f, 19f)
            horizontalLineTo(5f)
            verticalLineTo(5f)
            horizontalLineTo(12f)
            verticalLineTo(3f)
            horizontalLineTo(5f)
            curveTo(3.89f, 3f, 3f, 3.9f, 3f, 5f)
            verticalLineTo(19f)
            curveTo(3f, 20.1f, 3.89f, 21f, 5f, 21f)
            horizontalLineTo(19f)
            curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
            verticalLineTo(12f)
            horizontalLineTo(19f)
            close()
            moveTo(14f, 3f)
            verticalLineTo(5f)
            horizontalLineTo(17.59f)
            lineTo(7.76f, 14.83f)
            lineTo(9.17f, 16.24f)
            lineTo(19f, 6.41f)
            verticalLineTo(10f)
            horizontalLineTo(21f)
            verticalLineTo(3f)
            close()
        }
    }

    val MusicNote: ImageVector = build("MusicNote") {
        fill {
            moveTo(12f, 3f)
            verticalLineTo(13.55f)
            curveTo(11.41f, 13.21f, 10.73f, 13f, 10f, 13f)
            curveTo(7.79f, 13f, 6f, 14.79f, 6f, 17f)
            curveTo(6f, 19.21f, 7.79f, 21f, 10f, 21f)
            curveTo(12.21f, 21f, 14f, 19.21f, 14f, 17f)
            verticalLineTo(7f)
            horizontalLineTo(18f)
            verticalLineTo(3f)
            close()
        }
    }

    val Cookie: ImageVector = build("Cookie") {
        stroke {
            moveTo(12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            close()
        }
        fill {
            moveTo(8.2f, 8.2f)
            curveTo(9.0f, 8.2f, 9.6f, 8.8f, 9.6f, 9.6f)
            curveTo(9.6f, 10.4f, 9.0f, 11f, 8.2f, 11f)
            curveTo(7.4f, 11f, 6.8f, 10.4f, 6.8f, 9.6f)
            curveTo(6.8f, 8.8f, 7.4f, 8.2f, 8.2f, 8.2f)
            close()
            moveTo(15.8f, 5.8f)
            curveTo(16.6f, 5.8f, 17.2f, 6.4f, 17.2f, 7.2f)
            curveTo(17.2f, 8f, 16.6f, 8.6f, 15.8f, 8.6f)
            curveTo(15f, 8.6f, 14.4f, 8f, 14.4f, 7.2f)
            curveTo(14.4f, 6.4f, 15f, 5.8f, 15.8f, 5.8f)
            close()
            moveTo(8.6f, 14.2f)
            curveTo(9.4f, 14.2f, 10f, 14.8f, 10f, 15.6f)
            curveTo(10f, 16.4f, 9.4f, 17f, 8.6f, 17f)
            curveTo(7.8f, 17f, 7.2f, 16.4f, 7.2f, 15.6f)
            curveTo(7.2f, 14.8f, 7.8f, 14.2f, 8.6f, 14.2f)
            close()
            moveTo(15.4f, 12.6f)
            curveTo(16.2f, 12.6f, 16.8f, 13.2f, 16.8f, 14f)
            curveTo(16.8f, 14.8f, 16.2f, 15.4f, 15.4f, 15.4f)
            curveTo(14.6f, 15.4f, 14f, 14.8f, 14f, 14f)
            curveTo(14f, 13.2f, 14.6f, 12.6f, 15.4f, 12.6f)
            close()
        }
    }

    val Sync: ImageVector = build("Sync") {
        fill {
            moveTo(17.65f, 6.35f)
            curveTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
            curveTo(7.58f, 4f, 4.01f, 7.58f, 4.01f, 12f)
            curveTo(4.01f, 16.42f, 7.58f, 20f, 12f, 20f)
            curveTo(15.73f, 20f, 18.84f, 17.45f, 19.73f, 14f)
            horizontalLineTo(17.65f)
            curveTo(16.83f, 16.33f, 14.61f, 18f, 12f, 18f)
            curveTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
            curveTo(6f, 8.69f, 8.69f, 6f, 12f, 6f)
            curveTo(13.66f, 6f, 15.14f, 6.69f, 16.22f, 7.78f)
            lineTo(13f, 11f)
            horizontalLineTo(20f)
            verticalLineTo(4f)
            close()
        }
    }

    val History: ImageVector = build("History") {
        fill {
            moveTo(13f, 3f)
            curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
            horizontalLineTo(1f)
            lineTo(4.89f, 15.89f)
            lineTo(4.96f, 16.03f)
            lineTo(9f, 12f)
            horizontalLineTo(6f)
            curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f)
            curveTo(16.87f, 5f, 20f, 8.13f, 20f, 12f)
            curveTo(20f, 15.87f, 16.87f, 19f, 13f, 19f)
            curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
            lineTo(6.64f, 18.36f)
            curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
            curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f)
            curveTo(22f, 7.03f, 17.97f, 3f, 13f, 3f)
            close()
            moveTo(12f, 8f)
            verticalLineTo(13f)
            lineTo(16.28f, 15.54f)
            lineTo(17f, 14.33f)
            lineTo(13.5f, 12.25f)
            verticalLineTo(8f)
            close()
        }
    }

    val Moon: ImageVector = build("Moon") {
        fill {
            moveTo(12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 11.54f, 20.96f, 11.08f, 20.9f, 10.64f)
            curveTo(19.92f, 12.01f, 18.32f, 12.9f, 16.5f, 12.9f)
            curveTo(13.47f, 12.9f, 11f, 10.43f, 11f, 7.4f)
            curveTo(11f, 5.58f, 11.89f, 3.98f, 13.26f, 3f)
            curveTo(12.84f, 2.98f, 12.42f, 3f, 12f, 3f)
            close()
        }
    }

    val Shield: ImageVector = build("Shield") {
        fill {
            moveTo(12f, 1f)
            lineTo(3f, 5f)
            verticalLineTo(11f)
            curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
            curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
            verticalLineTo(5f)
            close()
        }
    }

    val Check: ImageVector = build("Check") {
        stroke {
            moveTo(4.5f, 12.5f)
            lineTo(9.5f, 17.5f)
            lineTo(19.5f, 7f)
        }
    }

    val File: ImageVector = build("File") {
        stroke {
            moveTo(6f, 3.5f)
            horizontalLineTo(14f)
            lineTo(19f, 8.5f)
            verticalLineTo(20.5f)
            horizontalLineTo(6f)
            close()
            moveTo(14f, 3.5f)
            verticalLineTo(8.5f)
            horizontalLineTo(19f)
            moveTo(9f, 12.5f)
            horizontalLineTo(16f)
            moveTo(9f, 16f)
            horizontalLineTo(14f)
        }
    }

    val ChevronRight: ImageVector = build("ChevronRight") {
        stroke {
            moveTo(9f, 5f)
            lineTo(16f, 12f)
            lineTo(9f, 19f)
        }
    }

    val CloseSmall: ImageVector = build("CloseSmall") {
        stroke {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }
}