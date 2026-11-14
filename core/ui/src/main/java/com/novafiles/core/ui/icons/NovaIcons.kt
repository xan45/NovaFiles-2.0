package com.novafiles.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object NovaIcons {

    val Home: ImageVector by lazy {
        ImageVector.Builder("NovaHome", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 10.5f)
                lineTo(12f, 3f)
                lineTo(21f, 10.5f)
                lineTo(21f, 20f)
                quadTo(21f, 21f, 20f, 21f)
                lineTo(15f, 21f)
                lineTo(15f, 14f)
                lineTo(9f, 14f)
                lineTo(9f, 21f)
                lineTo(4f, 21f)
                quadTo(3f, 21f, 3f, 20f)
                close()
            }
        }.build()
    }

    val Folder: ImageVector by lazy {
        ImageVector.Builder("NovaFolder", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 6f)
                quadTo(3f, 4f, 5f, 4f)
                lineTo(9.5f, 4f)
                lineTo(12f, 7f)
                lineTo(19f, 7f)
                quadTo(21f, 7f, 21f, 9f)
                lineTo(21f, 18f)
                quadTo(21f, 20f, 19f, 20f)
                lineTo(5f, 20f)
                quadTo(3f, 20f, 3f, 18f)
                close()
            }
        }.build()
    }

    val DualPane: ImageVector by lazy {
        ImageVector.Builder("NovaDualPane", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 5f)
                lineTo(10f, 5f)
                lineTo(10f, 19f)
                lineTo(3f, 19f)
                close()
                moveTo(14f, 5f)
                lineTo(21f, 5f)
                lineTo(21f, 19f)
                lineTo(14f, 19f)
                close()
            }
        }.build()
    }

    val SettingsSliders: ImageVector by lazy {
        ImageVector.Builder("NovaSettings", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
                moveTo(4f, 17f)
                lineTo(20f, 17f)
                moveTo(8f, 5f)
                lineTo(8f, 9f)
                moveTo(16f, 10f)
                lineTo(16f, 14f)
                moveTo(11f, 15f)
                lineTo(11f, 19f)
            }
        }.build()
    }

    val Photos: ImageVector by lazy {
        ImageVector.Builder("NovaPhotos", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 20f)
                lineTo(4f, 20f)
                close()
                moveTo(4f, 16f)
                lineTo(9f, 11f)
                lineTo(14f, 15f)
                lineTo(17f, 12f)
                lineTo(20f, 15f)
                moveTo(8.5f, 8.5f)
                lineTo(8.6f, 8.5f)
            }
        }.build()
    }

    val Videos: ImageVector by lazy {
        ImageVector.Builder("NovaVideos", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 5f)
                lineTo(20f, 5f)
                lineTo(20f, 19f)
                lineTo(4f, 19f)
                close()
                moveTo(10f, 9f)
                lineTo(15.5f, 12f)
                lineTo(10f, 15f)
                close()
            }
        }.build()
    }

    val Documents: ImageVector by lazy {
        ImageVector.Builder("NovaDocuments", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 3f)
                lineTo(14f, 3f)
                lineTo(19f, 8f)
                lineTo(19f, 21f)
                lineTo(6f, 21f)
                close()
                moveTo(14f, 3f)
                lineTo(14f, 8f)
                lineTo(19f, 8f)
                moveTo(9f, 13f)
                lineTo(15f, 13f)
                moveTo(9f, 17f)
                lineTo(13f, 17f)
            }
        }.build()
    }

    val AudioWave: ImageVector by lazy {
        ImageVector.Builder("NovaAudioWave", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 10f)
                lineTo(4f, 14f)
                moveTo(8f, 7f)
                lineTo(8f, 17f)
                moveTo(12f, 4f)
                lineTo(12f, 20f)
                moveTo(16f, 7f)
                lineTo(16f, 17f)
                moveTo(20f, 10f)
                lineTo(20f, 14f)
            }
        }.build()
    }

    val ArchiveZip: ImageVector by lazy {
        ImageVector.Builder("NovaArchiveZip", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 20f)
                lineTo(4f, 20f)
                close()
                moveTo(12f, 4f)
                lineTo(12f, 14f)
                moveTo(10f, 7f)
                lineTo(14f, 7f)
                moveTo(10f, 10f)
                lineTo(14f, 10f)
                moveTo(10f, 14f)
                lineTo(14f, 14f)
                lineTo(14f, 17f)
                lineTo(10f, 17f)
                close()
            }
        }.build()
    }

    val ApkCube: ImageVector by lazy {
        ImageVector.Builder("NovaApkCube", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                lineTo(20.5f, 7.2f)
                lineTo(20.5f, 16.8f)
                lineTo(12f, 21.5f)
                lineTo(3.5f, 16.8f)
                lineTo(3.5f, 7.2f)
                close()
                moveTo(3.5f, 7.2f)
                lineTo(12f, 12f)
                lineTo(20.5f, 7.2f)
                moveTo(12f, 12f)
                lineTo(12f, 21.5f)
            }
        }.build()
    }
}
