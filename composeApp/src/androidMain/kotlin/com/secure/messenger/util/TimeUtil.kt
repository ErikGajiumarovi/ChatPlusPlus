package com.secure.messenger.util

actual object TimeUtil {
    actual fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
