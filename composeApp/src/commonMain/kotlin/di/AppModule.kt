package di

import data.FirebaseClient
import data.NewFirebaseClient
import data.repository.AuthRepository
import data.repository.MessagesRepository
import presentation.viewmodel.ChatViewModel
import presentation.viewmodel.LoginViewModel
import presentation.viewmodel.ChatListViewModel
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
        single { NewFirebaseClient() }

        // Repositories
        single { AuthRepository(get()) }
        single { MessagesRepository(get()) }

        // ViewModels
        factory { LoginViewModel(get(), get()) }
        factory { ChatListViewModel(get(), get()) }
        factory { (chatId: String) -> ChatViewModel(chatId, get(), get()) }
    }
}
