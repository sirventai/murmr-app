package uoc.edu.psirvent.murmr

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import uoc.edu.psirvent.murmr.data.network.AppSession
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.di.initKoin
import uoc.edu.psirvent.murmr.domain.model.AutoLockTimeout
import uoc.edu.psirvent.murmr.security.AppLockManager
import uoc.edu.psirvent.murmr.util.CrashReporter
import uoc.edu.psirvent.murmr.util.Logger

private val koinReady: Boolean by lazy {
    CrashReporter.install()
    initKoin()
    true
}

@Suppress("unused")
fun setLoggingEnabled(enabled: Boolean) {
    Logger.enabled = enabled
}

@Suppress("FunctionName")
fun MainViewController() = ComposeUIViewController {
    koinReady
    App()
}

private val iosLifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

fun notifyAppForegrounded() {
    val tag = "AppLockBridge"
    iosLifecycleScope.launch {
        runCatching {
            val koin = KoinPlatform.getKoin()
            val lock = koin.get<AppLockManager>()
            val settingsRepo = koin.get<SettingsRepository>()
            val settings = settingsRepo.getFirst() ?: run {
                Logger.i(tag, "foregrounded: sin settings persistidos, salto bloqueo")
                return@runCatching
            }
            val seconds = when (settings.autoLockTimeout) {
                AutoLockTimeout.NEVER -> -1
                AutoLockTimeout.FIVE_SECONDS -> 5
                AutoLockTimeout.SIXTY_SECONDS -> 60
                AutoLockTimeout.FIVE_MINUTES -> 5 * 60
                AutoLockTimeout.CUSTOM -> settings.customAutoLockSeconds
            }
            Logger.i(tag, "foregrounded: biometric=${settings.biometricLock} autoLock=${settings.autoLockTimeout} ($seconds s)")
            koin.get<AppSession>().resumeNetwork()
            if (seconds < 0) return@runCatching
            lock.notifyForegrounded(
                biometricEnabled = settings.biometricLock,
                autoLockSeconds = seconds,
            )
        }.onFailure { Logger.w(tag, "foregrounded failed: ${it.message}") }
    }
}

fun notifyAppBackgrounded() {
    val tag = "AppLockBridge"
    runCatching {
        val koin = KoinPlatform.getKoin()
        koin.get<AppLockManager>().notifyBackgrounded()
        koin.get<AppSession>().pauseNetwork()
        Logger.i(tag, "backgrounded + pauseNetwork")
    }.onFailure { Logger.w(tag, "backgrounded failed: ${it.message}") }
}

