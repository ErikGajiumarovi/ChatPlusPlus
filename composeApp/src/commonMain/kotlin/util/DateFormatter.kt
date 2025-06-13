package util

import kotlinx.datetime.Instant

expect class DateFormatter() {
    fun formatTime(timestamp: Long): String
    fun formatDate(timestamp: Long): String
}
