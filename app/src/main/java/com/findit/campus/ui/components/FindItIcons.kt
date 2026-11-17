package com.findit.campus.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Item symbols that share the size and stroke of the Material outlined icons. */
object FindItIcons {
    val Bottle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bottle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10f, 3f)
                horizontalLineTo(14f)
                verticalLineTo(6f)
                curveTo(14f, 7.6f, 17f, 9f, 17f, 11f)
                verticalLineTo(19f)
                curveTo(17f, 20.1f, 16.1f, 21f, 15f, 21f)
                horizontalLineTo(9f)
                curveTo(7.9f, 21f, 7f, 20.1f, 7f, 19f)
                verticalLineTo(11f)
                curveTo(7f, 9f, 10f, 7.6f, 10f, 6f)
                close()
                moveTo(10f, 6f)
                horizontalLineTo(14f)
                moveTo(7f, 12f)
                horizontalLineTo(17f)
                moveTo(7f, 17f)
                horizontalLineTo(17f)
            }
        }.build()
    }
}
