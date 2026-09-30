package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

object MurmrIcons {

    val ArrowBack: ImageVector by lazy {
        materialPath24("ArrowBack") {
            moveTo(20f, 11f); horizontalLineTo(7.83f); lineTo(13.42f, 5.41f)
            lineTo(12f, 4f); lineTo(4f, 12f); lineTo(12f, 20f); lineTo(13.41f, 18.59f)
            lineTo(7.83f, 13f); horizontalLineTo(20f); close()
        }
    }

    val Backspace: ImageVector by lazy {
        materialPath24("Backspace") {
            moveTo(22f, 3f); horizontalLineTo(7f)
            curveTo(6.31f, 3f, 5.77f, 3.35f, 5.41f, 3.88f)
            lineTo(0f, 12f); lineTo(5.41f, 20.11f)
            curveTo(5.77f, 20.64f, 6.31f, 21f, 7f, 21f)
            horizontalLineTo(22f)
            curveTo(23.1f, 21f, 24f, 20.1f, 24f, 19f)
            verticalLineTo(5f)
            curveTo(24f, 3.9f, 23.1f, 3f, 22f, 3f); close()
            moveTo(19f, 15.59f); lineTo(17.59f, 17f); lineTo(14f, 13.41f)
            lineTo(10.41f, 17f); lineTo(9f, 15.59f); lineTo(12.59f, 12f)
            lineTo(9f, 8.41f); lineTo(10.41f, 7f); lineTo(14f, 10.59f)
            lineTo(17.59f, 7f); lineTo(19f, 8.41f); lineTo(15.41f, 12f); lineTo(19f, 15.59f); close()
        }
    }

    val Close: ImageVector by lazy {
        materialPath24("Close") {
            moveTo(19f, 6.41f); lineTo(17.59f, 5f); lineTo(12f, 10.59f); lineTo(6.41f, 5f)
            lineTo(5f, 6.41f); lineTo(10.59f, 12f); lineTo(5f, 17.59f); lineTo(6.41f, 19f)
            lineTo(12f, 13.41f); lineTo(17.59f, 19f); lineTo(19f, 17.59f); lineTo(13.41f, 12f); close()
        }
    }

    val Check: ImageVector by lazy {
        materialPath24("Check") {
            moveTo(9f, 16.17f); lineTo(4.83f, 12f); lineTo(3.41f, 13.41f); lineTo(9f, 19f)
            lineTo(21f, 7f); lineTo(19.59f, 5.59f); close()
        }
    }

    val Edit: ImageVector by lazy {
        materialPath24("Edit") {
            moveTo(3f, 17.25f); verticalLineTo(21f); horizontalLineTo(6.75f)
            lineTo(17.81f, 9.94f); lineTo(14.06f, 6.19f); close()
            moveTo(20.71f, 7.04f)
            curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
            lineTo(18.37f, 3.29f)
            curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
            lineTo(15.13f, 5.12f); lineTo(18.88f, 8.87f); close()
        }
    }

    val Delete: ImageVector by lazy {
        materialPath24("Delete") {
            moveTo(6f, 19f); curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
            horizontalLineTo(16f); curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
            verticalLineTo(7f); horizontalLineTo(6f); close()
            moveTo(19f, 4f); horizontalLineTo(15.5f); lineTo(14.5f, 3f)
            horizontalLineTo(9.5f); lineTo(8.5f, 4f); horizontalLineTo(5f)
            verticalLineTo(6f); horizontalLineTo(19f); close()
        }
    }

    val Lock: ImageVector by lazy {
        materialPath24("Lock") {
            moveTo(18f, 8f); horizontalLineTo(17f); verticalLineTo(6f)
            curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f); reflectiveCurveTo(7f, 3.24f, 7f, 6f)
            verticalLineTo(8f); horizontalLineTo(6f); curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
            verticalLineTo(20f); curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f); curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(10f); curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f); close()
            moveTo(9f, 6f); curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
            reflectiveCurveTo(15f, 4.34f, 15f, 6f); verticalLineTo(8f); horizontalLineTo(9f); close()
        }
    }

    val LockOpen: ImageVector by lazy {
        materialPath24("LockOpen") {
            moveTo(18f, 8f); horizontalLineTo(17f); verticalLineTo(6f)
            curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f); reflectiveCurveTo(7f, 3.24f, 7f, 6f)
            verticalLineTo(8f); horizontalLineTo(8.9f); verticalLineTo(6f)
            curveTo(8.9f, 4.29f, 10.29f, 2.9f, 12f, 2.9f); reflectiveCurveTo(15.1f, 4.29f, 15.1f, 6f)
            verticalLineTo(8f); horizontalLineTo(6f); curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
            verticalLineTo(20f); curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f); curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(10f); curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f); close()
        }
    }

    val Notifications: ImageVector by lazy {
        materialPath24("Notifications") {
            moveTo(12f, 22f); curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f); horizontalLineTo(10f)
            curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f); close()
            moveTo(18f, 16f); verticalLineTo(11f); curveTo(18f, 7.93f, 16.36f, 5.36f, 13.5f, 4.68f)
            verticalLineTo(4f); curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
            reflectiveCurveTo(10.5f, 3.17f, 10.5f, 4f); verticalLineTo(4.68f)
            curveTo(7.63f, 5.36f, 6f, 7.92f, 6f, 11f); verticalLineTo(16f); lineTo(4f, 18f)
            verticalLineTo(19f); horizontalLineTo(20f); verticalLineTo(18f); close()
        }
    }

    val Person: ImageVector by lazy {
        materialPath24("Person") {
            moveTo(12f, 12f); curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
            reflectiveCurveTo(14.21f, 4f, 12f, 4f); reflectiveCurveTo(8f, 5.79f, 8f, 8f)
            reflectiveCurveTo(9.79f, 12f, 12f, 12f); close()
            moveTo(12f, 14f); curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
            verticalLineTo(20f); horizontalLineTo(20f); verticalLineTo(18f)
            curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f); close()
        }
    }

    val Settings: ImageVector by lazy {
        materialPath24("Settings") {
            moveTo(19.43f, 12.98f)
            curveTo(19.47f, 12.66f, 19.5f, 12.34f, 19.5f, 12f)
            reflectiveCurveTo(19.47f, 11.34f, 19.43f, 11.02f)
            lineTo(21.54f, 9.37f)
            curveTo(21.73f, 9.22f, 21.78f, 8.95f, 21.66f, 8.73f)
            lineTo(19.66f, 5.27f)
            curveTo(19.54f, 5.05f, 19.27f, 4.97f, 19.05f, 5.05f)
            lineTo(16.56f, 6.05f)
            curveTo(16.04f, 5.65f, 15.48f, 5.32f, 14.87f, 5.07f)
            lineTo(14.49f, 2.42f)
            curveTo(14.46f, 2.18f, 14.25f, 2f, 14f, 2f)
            horizontalLineTo(10f)
            curveTo(9.75f, 2f, 9.54f, 2.18f, 9.51f, 2.42f)
            lineTo(9.13f, 5.07f)
            curveTo(8.52f, 5.32f, 7.96f, 5.66f, 7.44f, 6.05f)
            lineTo(4.95f, 5.05f)
            curveTo(4.72f, 4.96f, 4.46f, 5.05f, 4.34f, 5.27f)
            lineTo(2.34f, 8.73f)
            curveTo(2.21f, 8.95f, 2.27f, 9.22f, 2.46f, 9.37f)
            lineTo(4.57f, 11.02f)
            curveTo(4.53f, 11.34f, 4.5f, 11.67f, 4.5f, 12f)
            reflectiveCurveTo(4.53f, 12.66f, 4.57f, 12.98f)
            lineTo(2.46f, 14.63f)
            curveTo(2.27f, 14.78f, 2.22f, 15.05f, 2.34f, 15.27f)
            lineTo(4.34f, 18.73f)
            curveTo(4.46f, 18.95f, 4.73f, 19.03f, 4.95f, 18.95f)
            lineTo(7.44f, 17.95f)
            curveTo(7.96f, 18.35f, 8.52f, 18.68f, 9.13f, 18.93f)
            lineTo(9.51f, 21.58f)
            curveTo(9.54f, 21.82f, 9.75f, 22f, 10f, 22f)
            horizontalLineTo(14f)
            curveTo(14.25f, 22f, 14.46f, 21.82f, 14.49f, 21.58f)
            lineTo(14.87f, 18.93f)
            curveTo(15.48f, 18.68f, 16.04f, 18.34f, 16.56f, 17.95f)
            lineTo(19.05f, 18.95f)
            curveTo(19.28f, 19.04f, 19.54f, 18.95f, 19.66f, 18.73f)
            lineTo(21.66f, 15.27f)
            curveTo(21.78f, 15.05f, 21.73f, 14.78f, 21.54f, 14.63f)
            close()
            moveTo(12f, 15.5f)
            curveTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
            reflectiveCurveTo(10.07f, 8.5f, 12f, 8.5f)
            reflectiveCurveTo(15.5f, 10.07f, 15.5f, 12f)
            reflectiveCurveTo(13.93f, 15.5f, 12f, 15.5f)
            close()
        }
    }

    val MoreVert: ImageVector by lazy {
        materialPath24("MoreVert") {
            moveTo(12f, 8f); curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f)
            reflectiveCurveTo(13.1f, 4f, 12f, 4f); reflectiveCurveTo(10f, 4.9f, 10f, 6f)
            reflectiveCurveTo(10.9f, 8f, 12f, 8f); close()
            moveTo(12f, 10f); curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
            reflectiveCurveTo(10.9f, 14f, 12f, 14f); reflectiveCurveTo(14f, 13.1f, 14f, 12f)
            reflectiveCurveTo(13.1f, 10f, 12f, 10f); close()
            moveTo(12f, 16f); curveTo(10.9f, 16f, 10f, 16.9f, 10f, 18f)
            reflectiveCurveTo(10.9f, 20f, 12f, 20f); reflectiveCurveTo(14f, 19.1f, 14f, 18f)
            reflectiveCurveTo(13.1f, 16f, 12f, 16f); close()
        }
    }

    val ChatBubbleOutline: ImageVector by lazy {
        materialPath24("ChatBubbleOutline") {
            moveTo(20f, 2f); horizontalLineTo(4f)
            curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f); verticalLineTo(22f)
            lineTo(6f, 18f); horizontalLineTo(20f)
            curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f); verticalLineTo(4f)
            curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f); close()
            moveTo(20f, 16f); horizontalLineTo(5.17f); lineTo(4f, 17.17f)
            verticalLineTo(4f); horizontalLineTo(20f); verticalLineTo(16f); close()
        }
    }

    val GraphicEq: ImageVector by lazy {
        materialPath24("GraphicEq") {
            moveTo(7f, 18f); horizontalLineTo(10f); verticalLineTo(6f); horizontalLineTo(7f); close()
            moveTo(11f, 22f); horizontalLineTo(14f); verticalLineTo(2f); horizontalLineTo(11f); close()
            moveTo(3f, 14f); horizontalLineTo(6f); verticalLineTo(10f); horizontalLineTo(3f); close()
            moveTo(15f, 18f); horizontalLineTo(18f); verticalLineTo(6f); horizontalLineTo(15f); close()
            moveTo(19f, 14f); horizontalLineTo(22f); verticalLineTo(10f); horizontalLineTo(19f); close()
        }
    }

    val PersonOutline: ImageVector by lazy {
        materialPath24("PersonOutline") {
            moveTo(12f, 5.9f); curveTo(13.16f, 5.9f, 14.1f, 6.84f, 14.1f, 8f)
            reflectiveCurveTo(13.16f, 10.1f, 12f, 10.1f); reflectiveCurveTo(9.9f, 9.16f, 9.9f, 8f)
            reflectiveCurveTo(10.84f, 5.9f, 12f, 5.9f); close()
            moveTo(12f, 4f); curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
            reflectiveCurveTo(9.79f, 12f, 12f, 12f); reflectiveCurveTo(16f, 10.21f, 16f, 8f)
            reflectiveCurveTo(14.21f, 4f, 12f, 4f); close()
            moveTo(12f, 14.9f); curveTo(14.97f, 14.9f, 18.1f, 16.36f, 18.1f, 17f)
            verticalLineTo(18.1f); horizontalLineTo(5.9f); verticalLineTo(17f)
            curveTo(5.9f, 16.36f, 9.03f, 14.9f, 12f, 14.9f); close()
            moveTo(12f, 13f); curveTo(9.33f, 13f, 4f, 14.34f, 4f, 17f)
            verticalLineTo(20f); horizontalLineTo(20f); verticalLineTo(17f)
            curveTo(20f, 14.34f, 14.67f, 13f, 12f, 13f); close()
        }
    }

    val ChevronRight: ImageVector by lazy {
        materialPath24("ChevronRight") {
            moveTo(10f, 6f); lineTo(8.59f, 7.41f); lineTo(13.17f, 12f)
            lineTo(8.59f, 16.59f); lineTo(10f, 18f); lineTo(16f, 12f); close()
        }
    }

    val Schedule: ImageVector by lazy {
        materialPath24("Schedule") {
            moveTo(11.99f, 2f); curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveTo(6.47f, 22f, 11.99f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f); reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
            close()
            moveTo(12f, 20f); curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
            reflectiveCurveTo(7.58f, 4f, 12f, 4f); reflectiveCurveTo(20f, 7.58f, 20f, 12f)
            reflectiveCurveTo(16.42f, 20f, 12f, 20f); close()
            moveTo(12.5f, 7f); horizontalLineTo(11f); verticalLineTo(13f)
            lineTo(16.25f, 16.15f); lineTo(17f, 14.92f); lineTo(12.5f, 12.25f); close()
        }
    }

    val Fingerprint: ImageVector by lazy {
        materialPath24("Fingerprint") {

            moveTo(12f, 2f); curveTo(8.69f, 2f, 6f, 4.69f, 6f, 8f)
            verticalLineTo(13f); curveTo(6f, 16.31f, 8.69f, 19f, 12f, 19f)
            reflectiveCurveTo(18f, 16.31f, 18f, 13f); verticalLineTo(8f)
            curveTo(18f, 4.69f, 15.31f, 2f, 12f, 2f); close()
            moveTo(16f, 13f); curveTo(16f, 15.21f, 14.21f, 17f, 12f, 17f)
            reflectiveCurveTo(8f, 15.21f, 8f, 13f); verticalLineTo(8f)
            curveTo(8f, 5.79f, 9.79f, 4f, 12f, 4f); reflectiveCurveTo(16f, 5.79f, 16f, 8f); close()
            moveTo(11f, 19f); horizontalLineTo(13f); verticalLineTo(22f)
            horizontalLineTo(11f); close()
        }
    }

    val Send: ImageVector by lazy {
        materialPath24("Send") {
            moveTo(2.01f, 21f); lineTo(23f, 12f); lineTo(2.01f, 3f)
            lineTo(2f, 10f); lineTo(17f, 12f); lineTo(2f, 14f); close()
        }
    }

    val Block: ImageVector by lazy {
        materialPath24("Block") {
            moveTo(12f, 2f); curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveTo(6.48f, 22f, 12f, 22f); reflectiveCurveTo(22f, 17.52f, 22f, 12f)
            reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
            moveTo(4f, 12f); curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
            curveTo(13.85f, 4f, 15.55f, 4.63f, 16.9f, 5.69f); lineTo(5.69f, 16.9f)
            curveTo(4.63f, 15.55f, 4f, 13.85f, 4f, 12f); close()
            moveTo(12f, 20f); curveTo(10.15f, 20f, 8.45f, 19.37f, 7.1f, 18.31f)
            lineTo(18.31f, 7.1f); curveTo(19.37f, 8.45f, 20f, 10.15f, 20f, 12f)
            curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f); close()
        }
    }

    val InfoOutline: ImageVector by lazy {
        materialPath24("InfoOutline") {
            moveTo(11f, 7f); horizontalLineTo(13f); verticalLineTo(9f); horizontalLineTo(11f); close()
            moveTo(11f, 11f); horizontalLineTo(13f); verticalLineTo(17f); horizontalLineTo(11f); close()
            moveTo(12f, 2f); curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveTo(6.48f, 22f, 12f, 22f); reflectiveCurveTo(22f, 17.52f, 22f, 12f)
            reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
            moveTo(12f, 20f); curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
            reflectiveCurveTo(7.59f, 4f, 12f, 4f); reflectiveCurveTo(20f, 7.59f, 20f, 12f)
            reflectiveCurveTo(16.41f, 20f, 12f, 20f); close()
        }
    }

    val Refresh: ImageVector by lazy {
        materialPath24("Refresh") {
            moveTo(17.65f, 6.35f)
            curveTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
            curveTo(7.58f, 4f, 4.01f, 7.58f, 4.01f, 12f)
            reflectiveCurveTo(7.58f, 20f, 12f, 20f)
            curveTo(15.73f, 20f, 18.84f, 17.45f, 19.73f, 14f)
            horizontalLineTo(17.65f)
            curveTo(16.83f, 16.33f, 14.61f, 18f, 12f, 18f)
            curveTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
            reflectiveCurveTo(8.69f, 6f, 12f, 6f)
            curveTo(13.66f, 6f, 15.14f, 6.69f, 16.22f, 7.78f)
            lineTo(13f, 11f); horizontalLineTo(20f); verticalLineTo(4f); close()
        }
    }

    val Search: ImageVector by lazy {
        materialPath24("Search") {
            moveTo(15.5f, 14f); horizontalLineTo(14.71f); lineTo(14.43f, 13.73f)
            curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
            curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f); reflectiveCurveTo(3f, 5.91f, 3f, 9.5f)
            reflectiveCurveTo(5.91f, 16f, 9.5f, 16f); curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
            lineTo(14f, 14.71f); verticalLineTo(15.5f); lineTo(19f, 20.49f); lineTo(20.49f, 19f); close()
            moveTo(9.5f, 14f); curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
            reflectiveCurveTo(7.01f, 5f, 9.5f, 5f); reflectiveCurveTo(14f, 7.01f, 14f, 9.5f)
            reflectiveCurveTo(11.99f, 14f, 9.5f, 14f); close()
        }
    }

    val Mood: ImageVector by lazy {
        materialPath24("Mood") {
            moveTo(11.99f, 2f); curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveTo(6.47f, 22f, 11.99f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f); reflectiveCurveTo(17.52f, 2f, 11.99f, 2f); close()
            moveTo(12f, 20f); curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
            reflectiveCurveTo(7.58f, 4f, 12f, 4f); reflectiveCurveTo(20f, 7.58f, 20f, 12f)
            reflectiveCurveTo(16.42f, 20f, 12f, 20f); close()
            moveTo(12f, 17.5f); curveTo(14.33f, 17.5f, 16.31f, 16.04f, 17.11f, 14f)
            horizontalLineTo(6.89f); curveTo(7.69f, 16.04f, 9.67f, 17.5f, 12f, 17.5f); close()
            moveTo(8.5f, 11f); curveTo(9.33f, 11f, 10f, 10.33f, 10f, 9.5f)
            reflectiveCurveTo(9.33f, 8f, 8.5f, 8f); reflectiveCurveTo(7f, 8.67f, 7f, 9.5f)
            reflectiveCurveTo(7.67f, 11f, 8.5f, 11f); close()
            moveTo(15.5f, 11f); curveTo(16.33f, 11f, 17f, 10.33f, 17f, 9.5f)
            reflectiveCurveTo(16.33f, 8f, 15.5f, 8f); reflectiveCurveTo(14f, 8.67f, 14f, 9.5f)
            reflectiveCurveTo(14.67f, 11f, 15.5f, 11f); close()
        }
    }
}

private fun materialPath24(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = "Murmr.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathBuilder().apply(block).nodes,
        pathFillType = PathFillType.NonZero,
        name = name,
        fill = SolidColor(Color.Black),
        fillAlpha = 1f,
        stroke = null,
        strokeAlpha = 1f,
        strokeLineWidth = 0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 4f,
    ).build()
