package com.erik.chatpp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import App
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import util.AESCrypto

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        val key = "dota2"
        val shifrator = AESCrypto(key)

        val plantext = "Hello, World!"
        val encrypted = shifrator.encrypt(plantext)
        val decrypted = shifrator.decrypt(encrypted)
        println("Original: $plantext")
        println("Encrypted: $encrypted")
        println("Decrypted: $decrypted")


        AESCrypto
        setContent {
            App()
        }
    }
}