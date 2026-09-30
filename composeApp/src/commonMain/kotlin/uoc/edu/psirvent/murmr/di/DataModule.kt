package uoc.edu.psirvent.murmr.di

import com.russhwolf.settings.Settings
import org.koin.dsl.module
import uoc.edu.psirvent.murmr.audio.SystemVolume
import uoc.edu.psirvent.murmr.data.db.DatabaseProvider
import uoc.edu.psirvent.murmr.data.db.DriverFactory
import uoc.edu.psirvent.murmr.data.network.AppSession
import uoc.edu.psirvent.murmr.data.network.ConnectivityMonitor
import uoc.edu.psirvent.murmr.data.network.DeviceTokenStore
import uoc.edu.psirvent.murmr.data.network.OutboxWorker
import uoc.edu.psirvent.murmr.data.network.PresenceTracker
import uoc.edu.psirvent.murmr.data.network.RelayApi
import uoc.edu.psirvent.murmr.data.network.RelayWebSocket
import uoc.edu.psirvent.murmr.data.network.TypingTracker
import uoc.edu.psirvent.murmr.data.repository.BlockedContactRepository
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.data.repository.MessageRepository
import uoc.edu.psirvent.murmr.data.repository.OutboxRepository
import uoc.edu.psirvent.murmr.data.repository.PairingLogRepository
import uoc.edu.psirvent.murmr.data.repository.SessionRepository
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.db.MurmrDatabase

val dataModule = module {
    single<MurmrDatabase> { DatabaseProvider.create(get<DriverFactory>()) }

    single { UserRepository(get()) }
    single { ContactRepository(get()) }
    single { SessionRepository(get()) }
    single { MessageRepository(get()) }
    single { SettingsRepository(get()) }
    single { BlockedContactRepository(get()) }
    single { PairingLogRepository(get()) }

    single { DeviceTokenStore(get<Settings>()) }
    single { SystemVolume() }

    single { PresenceTracker(get(), getOrNull<ConnectivityMonitor>()) }
    single { TypingTracker() }

    single { OutboxRepository(get()) }

    single {
        OutboxWorker(
            outbox = get(),
            messageRepository = get(),
            userRepository = get(),
            relayApi = get(),
            contactRepository = get(),
            relayConfigStore = getOrNull(),
            connectivity = getOrNull<ConnectivityMonitor>(),
        )
    }

    single {
        AppSession(
            userRepository = get(),
            relayApi = get(),
            relayWebSocket = get(),
            tokenStore = get(),
            receiveDecryptMessage = get(),
            outboxWorker = get(),
            relayConfigStore = get(),
        )
    }
}
