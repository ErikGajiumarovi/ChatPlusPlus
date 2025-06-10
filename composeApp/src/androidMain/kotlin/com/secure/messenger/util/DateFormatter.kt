package com.secure.messenger.util

import java.text.SimpleDateFormat
import java.util.*

actual class DateFormatter {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    actual fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    actual fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
}
