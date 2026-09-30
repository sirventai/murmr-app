package uoc.edu.psirvent.murmr.di

import org.koin.dsl.module
import uoc.edu.psirvent.murmr.data.network.DeviceTokenStore
import uoc.edu.psirvent.murmr.data.network.RelayApi
import uoc.edu.psirvent.murmr.data.repository.OutboxRepository
import uoc.edu.psirvent.murmr.domain.usecase.BlockContactUseCase
import uoc.edu.psirvent.murmr.domain.usecase.BroadcastExpiryUpdateUseCase
import uoc.edu.psirvent.murmr.domain.usecase.BroadcastProfileUpdateUseCase
import uoc.edu.psirvent.murmr.domain.usecase.DeleteAccountUseCase
import uoc.edu.psirvent.murmr.domain.usecase.DeleteContactSecureWipeUseCase
import uoc.edu.psirvent.murmr.domain.usecase.MarkReadUseCase
import uoc.edu.psirvent.murmr.domain.usecase.ReceiveDecryptMessageUseCase
import uoc.edu.psirvent.murmr.domain.usecase.RegisterDeviceUseCase
import uoc.edu.psirvent.murmr.domain.usecase.RequestPeerAvatarBigUseCase
import uoc.edu.psirvent.murmr.domain.usecase.RotateSessionKeyUseCase
import uoc.edu.psirvent.murmr.domain.usecase.SendEncryptedMessageUseCase
import uoc.edu.psirvent.murmr.domain.usecase.StartAcousticPairingUseCase
import uoc.edu.psirvent.murmr.domain.usecase.UnblockContactUseCase
import uoc.edu.psirvent.murmr.util.ApplicationScope

val domainModule = module {
    single { ApplicationScope() }
    single {
        RegisterDeviceUseCase(
            userRepository = get(),
            settingsRepository = get(),
            relayApi = getOrNull<RelayApi>(),
            tokenStore = getOrNull<DeviceTokenStore>(),
        )
    }
    single { RotateSessionKeyUseCase(get(), get()) }
    single {
        SendEncryptedMessageUseCase(
            userRepository = get(),
            contactRepository = get(),
            sessionRepository = get(),
            messageRepository = get(),
            keyStore = get(),
            rotateSessionKey = get(),
            relayApi = getOrNull<RelayApi>(),
            outbox = getOrNull<OutboxRepository>(),
            settingsRepository = get(),
        )
    }
    single {
        ReceiveDecryptMessageUseCase(
            userRepository = get(),
            contactRepository = get(),
            sessionRepository = get(),
            messageRepository = get(),
            keyStore = get(),
            sendEncrypted = get(),
            settingsRepository = get(),
            typingTracker = getOrNull(),
            presenceTracker = getOrNull(),
            rotateSessionKey = get(),
        )
    }
    single { MarkReadUseCase(get(), get(), get(), get()) }
    single { BroadcastProfileUpdateUseCase(get(), get(), get()) }
    single { BroadcastExpiryUpdateUseCase(get(), get(), get(), get()) }
    single { RequestPeerAvatarBigUseCase(get()) }
    single { StartAcousticPairingUseCase(get(), get(), get(), get(), get(), get(), broadcastProfileUpdate = get(), relayConfigStore = get(), broadcastExpiryUpdate = getOrNull()) }
    single { DeleteContactSecureWipeUseCase(get(), get(), get(), get(), get(), getOrNull()) }
    single { BlockContactUseCase(get(), get(), get(), sendEncrypted = getOrNull()) }
    single { UnblockContactUseCase(get(), get(), get()) }
    single { DeleteAccountUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), sendEncrypted = getOrNull()) }
}
