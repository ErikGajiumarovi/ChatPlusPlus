package com.secure.messenger.di

import com.secure.messenger.data.FirebaseClient
import com.secure.messenger.data.repository.AuthRepository
import com.secure.messenger.data.repository.MessagesRepository
import com.secure.messenger.presentation.viewmodel.ChatViewModel
import com.secure.messenger.presentation.viewmodel.LoginViewModel
import com.secure.messenger.presentation.viewmodel.ChatListViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.core.module.Module
import org.koin.dsl.module

object AppModule {
    // Эта функция больше не будет использоваться для запуска Koin
    // но можно оставить для явной инициализации Firebase
    fun initialize() {
        try {
            Firebase.initialize()
        } catch (e: Exception) {
            println("Error initializing Firebase: ${e.message}")
        }
    }

    // Делаем метод публичным для доступа из KoinApplication
    fun appModule(): Module = module {
        // Firebase client
        single { FirebaseClient() }

        // Repositories
        single { AuthRepository(get()) }
        single { MessagesRepository(get()) }

        // ViewModels
        factory { LoginViewModel(get()) }
        factory { ChatListViewModel(get(), get()) }
        factory { (chatId: String) -> ChatViewModel(chatId, get(), get()) }
    }
}
