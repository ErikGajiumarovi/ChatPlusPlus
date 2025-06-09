package com.erik.chatpp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform