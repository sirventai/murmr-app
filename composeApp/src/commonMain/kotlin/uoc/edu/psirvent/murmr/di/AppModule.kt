package uoc.edu.psirvent.murmr.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import uoc.edu.psirvent.murmr.audio.GgwaveAcousticTransport
import uoc.edu.psirvent.murmr.audio.ggwave.GgwaveProtocol
import uoc.edu.psirvent.murmr.domain.usecase.RegisterDeviceUseCase
import uoc.edu.psirvent.murmr.presentation.components.RelayDialogBus
import uoc.edu.psirvent.murmr.presentation.components.SnackbarController
import uoc.edu.psirvent.murmr.presentation.viewmodel.ChatExpiryHandler
import uoc.edu.psirvent.murmr.presentation.viewmodel.ChatViewModel
import uoc.edu.psirvent.murmr.presentation.viewmodel.ChatsListViewModel
import uoc.edu.psirvent.murmr.presentation.viewmodel.ContactDetailViewModel
import uoc.edu.psirvent.murmr.presentation.viewmodel.ContactsViewModel
import uoc.edu.psirvent.murmr.presentation.viewmodel.OnboardingViewModel
import uoc.edu.psirvent.murmr.presentation.viewmodel.PairingViewModel
import uoc.edu.psirvent.murmr.presentation.viewmodel.SettingsViewModel
import uoc.edu.psirvent.murmr.presentation.viewmodel.SplashViewModel
import uoc.edu.psirvent.murmr.security.AppLockManager
import uoc.edu.psirvent.murmr.security.BiometricGate

val appModule = module {
    single { SnackbarController() }
    single { RelayDialogBus() }
    single { AppLockManager() }
    single { BiometricGate() }
    single { ChatExpiryHandler(get(), get(), getOrNull()) }

    single { ChatsListViewModel(get(), get(), get(), getOrNull()) }
    single { ContactsViewModel(get(), get(), getOrNull()) }
    single { SettingsViewModel(get(), get(), get(), get(), get(), get(), getOrNull(), getOrNull(), broadcastExpiryUpdate = getOrNull()) }
    factory {
        PairingViewModel(
            userRepository = get(),
            keyStore = get(),
            confirmUseCase = get(),

            transportFactory = {
                GgwaveAcousticTransport(
                    protocol = GgwaveProtocol.ULTRASOUND_FASTEST,
                    volume = 100,
                    payloadRepeats = 1,
                    applyPreEmphasis = true,
                )
            },
            systemVolume = get(),
        )
    }
    factory { SplashViewModel(get()) }

    factory { (contactId: String) ->
        ChatViewModel(
            contactId, get(), get(),
            getOrNull(), getOrNull(), getOrNull(),
            settingsRepository = get(),
            expiryHandler = getOrNull(),
            sessionRepository = get(),
            typingTracker = getOrNull(),
            sendEncrypted = getOrNull(),
            broadcastProfileUpdate = getOrNull(),
        )
    }
    factory { (contactId: String) ->
        ContactDetailViewModel(
            contactId, get(), get(),
            settingsRepository = get(),
            broadcastProfileUpdate = getOrNull(),
            rotateSessionKey = getOrNull(),
        )
    }

    factory {
        OnboardingViewModel(registerDevice = wrapRegister(get<RegisterDeviceUseCase>()))
    }
}

private fun wrapRegister(useCase: RegisterDeviceUseCase): suspend (String, ByteArray?, ByteArray?) -> Unit =
    { alias, avatar, avatarBig -> useCase(alias, avatar, avatarBig) }
