package uoc.edu.psirvent.murmr.domain.usecase

import io.ktor.util.encodeBase64
import kotlin.time.Clock
import uoc.edu.psirvent.murmr.crypto.X25519KeyExchange
import uoc.edu.psirvent.murmr.crypto.deviceIdFromPublicKey
import uoc.edu.psirvent.murmr.data.network.DeviceTokenStore
import uoc.edu.psirvent.murmr.data.network.RegisterDeviceRequest
import uoc.edu.psirvent.murmr.data.network.RelayApi
import uoc.edu.psirvent.murmr.data.repository.SettingsRepository
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.AppSettings
import uoc.edu.psirvent.murmr.domain.model.User
import uoc.edu.psirvent.murmr.util.Logger

class RegisterDeviceUseCase(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val relayApi: RelayApi? = null,
    private val tokenStore: DeviceTokenStore? = null,
    private val clock: Clock = Clock.System,
) {
    private val tag = "RegisterDevice"

    suspend operator fun invoke(
        alias: String,
        avatar: ByteArray? = null,
        avatarBig: ByteArray? = null,
    ): User {
        val cleanAlias = alias.trim()
        require(cleanAlias.isNotEmpty()) { "alias vacío" }
        require(cleanAlias.length in 3..45) { "alias de 3 a 45 caracteres" }

        val now = clock.now()
        val keyPair = X25519KeyExchange.generateKeyPair()

        val deviceId = deviceIdFromPublicKey(keyPair.publicKey)
        Logger.i(tag, "Generadas claves locales para alias=$cleanAlias deviceId=$deviceId")

        val user = User(
            deviceId = deviceId,
            alias = cleanAlias,
            publicKey = keyPair.publicKey,
            privateKey = keyPair.privateKey,
            createdAt = now,
            avatar = avatar,
            avatarBig = avatarBig,
        )
        userRepository.upsert(user)
        Logger.i(tag, "Usuario persistido en SQLite local (avatar=${avatar?.size}B, big=${avatarBig?.size}B)")

        val settings = AppSettings.default(deviceId, now).copy(onboardingDone = true)
        settingsRepository.upsert(settings)

        if (relayApi != null && tokenStore != null) {
            try {
                Logger.i(tag, "Registrando device contra relay...")
                val response = relayApi.registerDevice(
                    RegisterDeviceRequest(
                        deviceId = deviceId,
                        publicKeyB64 = keyPair.publicKey.encodeBase64(),
                        alias = cleanAlias,
                    ),
                )
                tokenStore.put(response.token)
                Logger.i(tag, "Device registrado, token guardado (token len=${response.token.length})")
            } catch (t: Throwable) {
                Logger.w(tag, "Registro pendiente - relay no disponible: ${t.message}")
            }
        } else {
            Logger.w(tag, "RelayApi/tokenStore no inyectados; saltando registro.")
        }
        return user
    }
}
