package uoc.edu.psirvent.murmr.domain.model

sealed interface PairingState {
    data object Idle : PairingState
    data object Discovering : PairingState
    data class PeerFound(val peerAlias: String) : PairingState
    data class ExchangingKeys(
        val peerAlias: String,
        val profileName: String = "",
        val phase: KeyExchangePhase? = null,
        val phaseDetail: String? = null,
    ) : PairingState

    data class Verified(
        val contact: Contact,
        val session: Session,
        val sas: Sas,
    ) : PairingState

    data class Failed(val reason: PairingFailure) : PairingState
}

enum class KeyExchangePhase {
    WAITING_HELLO_ACK,
    SENDING_HELLO_ACK,
    WAITING_KEY_A,
    WAITING_KEY_B,
    SENDING_KEY_B,
    DERIVING,
}

enum class PairingFailure {
    AUDIO_TIMEOUT,
    DECODE_ERROR,
    SAS_MISMATCH,
    PERMISSION_DENIED,
    ALREADY_PAIRED,
    UNKNOWN,
    NO_PEER_FOUND,
    CHANNEL_TOO_NOISY,
}
