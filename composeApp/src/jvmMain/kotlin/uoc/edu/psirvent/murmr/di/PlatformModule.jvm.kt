package uoc.edu.psirvent.murmr.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.util.prefs.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
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
    single<Settings> {
        val profile = System.getenv("MURMR_PROFILE")?.takeIf { it.isNotBlank() }
        val node = if (profile != null) "uoc/edu/psirvent/murmr/$profile" else "uoc/edu/psirvent/murmr"
        PreferencesSettings(Preferences.userRoot().node(node))
    }
    single {
        RelayConfigStore(
            settings = get(),
            officialUrl = System.getenv("MURMR_RELAY_URL") ?: RelayConfig.DEFAULT,
        )
    }
    single { ConnectivityMonitor() }
    single { get<RelayConfigStore>().current() }
    single<HttpClient> { RelayApi.newClient(CIO) }
    single { RelayApi(get(), { get<RelayConfigStore>().current().baseUrl }, get()) }
    single { RelayWebSocket(get(), { get<RelayConfigStore>().current().baseUrl }, get()) }
}
