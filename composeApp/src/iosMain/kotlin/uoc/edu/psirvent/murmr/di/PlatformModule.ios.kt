package uoc.edu.psirvent.murmr.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults
import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.data.db.DriverFactory
import uoc.edu.psirvent.murmr.data.network.ConnectivityMonitor
import uoc.edu.psirvent.murmr.data.network.RelayApi
import uoc.edu.psirvent.murmr.data.network.RelayConfig
import uoc.edu.psirvent.murmr.data.network.RelayConfigStore
import uoc.edu.psirvent.murmr.data.network.RelayWebSocket

actual fun platformModule(): Module = module {
    single { DriverFactory() }
    single { KeyStoreManager() }
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single {
        RelayConfigStore(
            settings = get(),
            officialUrl = RelayConfig.DEFAULT,
        )
    }
    single { ConnectivityMonitor() }
    single { get<RelayConfigStore>().current() }
    single<HttpClient> { RelayApi.newClient(Darwin) }
    single { RelayApi(get(), { get<RelayConfigStore>().current().baseUrl }, get()) }
    single { RelayWebSocket(get(), { get<RelayConfigStore>().current().baseUrl }, get()) }
}
