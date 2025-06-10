package com.secure.messenger.util

/**
 * Кросс-платформенная утилита для получения текущего времени в миллисекундах
 */
expect object TimeUtil {
    /**
     * Возвращает текущее время в миллисекундах с начала эпохи (1 января 1970)
     */
    fun currentTimeMillis(): Long
}
