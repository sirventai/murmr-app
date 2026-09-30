package uoc.edu.psirvent.murmr

import android.Manifest
import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import uoc.edu.psirvent.murmr.audio.MicPermissionRegistry
import uoc.edu.psirvent.murmr.data.network.AppSession
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.notifications.NotificationPermissionRegistry
import uoc.edu.psirvent.murmr.di.appModule
import uoc.edu.psirvent.murmr.di.dataModule
import uoc.edu.psirvent.murmr.di.domainModule
import uoc.edu.psirvent.murmr.di.platformModule
import uoc.edu.psirvent.murmr.domain.model.AutoLockTimeout
import uoc.edu.psirvent.murmr.security.AppLockManager
import uoc.edu.psirvent.murmr.security.BiometricActivityRegistry
import uoc.edu.psirvent.murmr.util.CrashReporter
import uoc.edu.psirvent.murmr.util.Logger
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val tag = "MainActivity"

    private val recordAudioLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Logger.i(tag, "RECORD_AUDIO grant=$granted")
        MicPermissionRegistry.notifyGrantStatus(granted)
    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Logger.i(tag, "POST_NOTIFICATIONS grant=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        Logger.enabled = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        setTheme(R.style.Theme_Murmr)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        MicPermissionRegistry.activity = this
        MicPermissionRegistry.modernLauncher = {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        BiometricActivityRegistry.activity = this

        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        Logger.i(tag, "RECORD_AUDIO already granted at startup=$micGranted")

        val notifPrefs = getSharedPreferences("murmr_runtime_perms", MODE_PRIVATE)
        NotificationPermissionRegistry.hasAsked = { notifPrefs.getBoolean("notif_asked", false) }
        NotificationPermissionRegistry.markAsked = {
            notifPrefs.edit().putBoolean("notif_asked", true).apply()
        }
        NotificationPermissionRegistry.requester = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notifGranted = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (!notifGranted) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        setContent { App() }
    }

    override fun onPause() {
        super.onPause()
        runCatching {
            val koin = KoinPlatform.getKoin()
            koin.get<AppLockManager>().notifyBackgrounded()
            koin.get<AppSession>().pauseNetwork()
            Logger.i(tag, "onPause: notifyBackgrounded() + pauseNetwork()")
        }.onFailure { Logger.w(tag, "onPause failed: ${it.message}") }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            runCatching {
                val koin = KoinPlatform.getKoin()
                val lock = koin.get<AppLockManager>()
                val settingsRepo = koin.get<SettingsRepository>()
                val settings = settingsRepo.getFirst() ?: run {
                    Logger.i(tag, "onResume: sin settings persistidos, salto bloqueo")
                    return@runCatching
                }
                val seconds = when (settings.autoLockTimeout) {
                    AutoLockTimeout.NEVER -> -1
                    AutoLockTimeout.FIVE_SECONDS -> 5
                    AutoLockTimeout.SIXTY_SECONDS -> 60
                    AutoLockTimeout.FIVE_MINUTES -> 5 * 60
                    AutoLockTimeout.CUSTOM -> settings.customAutoLockSeconds
                }
                Logger.i(tag, "onResume: biometric=${settings.biometricLock} autoLock=${settings.autoLockTimeout} ($seconds s)")
                koin.get<AppSession>().resumeNetwork()
                if (seconds < 0) return@runCatching
                lock.notifyForegrounded(
                    biometricEnabled = settings.biometricLock,
                    autoLockSeconds = seconds,
                )
            }.onFailure { Logger.w(tag, "onResume failed: ${it.message}") }
        }
    }

    override fun onDestroy() {
        if (MicPermissionRegistry.activity === this) {
            MicPermissionRegistry.activity = null
            MicPermissionRegistry.modernLauncher = null
        }
        if (BiometricActivityRegistry.activity === this) {
            BiometricActivityRegistry.activity = null
        }
        super.onDestroy()
    }
}

class MurmrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.bind(this)
        CrashReporter.install()
        startKoin {
            androidContext(this@MurmrApplication)
            modules(
                appModule,
                dataModule,
                domainModule,
                platformModule(),
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
