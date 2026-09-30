package uoc.edu.psirvent.murmr.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
    single { DriverFactory(get()) }
    single { KeyStoreManager(get()) }
    single<Settings> {
        val ctx = get<Context>()
        SharedPreferencesSettings(ctx.getSharedPreferences("murmr_prefs", Context.MODE_PRIVATE))
    }
    single {
        RelayConfigStore(
            settings = get(),
            officialUrl = RelayConfig.DEFAULT,
        )
    }
    single { ConnectivityMonitor(get<Context>()) }
    single { get<RelayConfigStore>().current() }
    single<HttpClient> { RelayApi.newClient(OkHttp) }

    single { RelayApi(get(), { get<RelayConfigStore>().current().baseUrl }, get()) }
    single { RelayWebSocket(get(), { get<RelayConfigStore>().current().baseUrl }, get()) }
}
