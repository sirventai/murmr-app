package uoc.edu.psirvent.murmr.presentation.screen.home

import kotlinx.coroutines.flow.MutableSharedFlow

enum class HomeTab { Chats, Contacts, Pairing, Settings }

object HomeTabRouter {
    val requestedTab = MutableSharedFlow<HomeTab>(extraBufferCapacity = 1)
}
