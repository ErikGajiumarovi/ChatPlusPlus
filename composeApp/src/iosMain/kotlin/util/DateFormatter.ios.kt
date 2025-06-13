package util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970

actual class DateFormatter {
    private val timeFormatter: NSDateFormatter = NSDateFormatter().apply {
        dateFormat = "HH:mm"
    }

    private val dateFormatter: NSDateFormatter = NSDateFormatter().apply {
        dateFormat = "dd.MM.yyyy"
    }

    actual fun formatTime(timestamp: Long): String {
        val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
        return timeFormatter.stringFromDate(date)
    }

    actual fun formatDate(timestamp: Long): String {
        val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
        return dateFormatter.stringFromDate(date)
    }
}