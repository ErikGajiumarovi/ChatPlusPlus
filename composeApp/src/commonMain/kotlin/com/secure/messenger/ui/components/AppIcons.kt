package com.secure.messenger.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Кросс-платформенная обертка для иконок
 */
expect object AppIcons {
    val Add: ImageVector
    val ArrowBack: ImageVector
    val ExitToApp: ImageVector
    val Send: ImageVector
}
