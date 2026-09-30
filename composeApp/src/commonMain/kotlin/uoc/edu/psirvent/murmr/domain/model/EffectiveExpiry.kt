package uoc.edu.psirvent.murmr.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

data class EffectiveExpiry(
    val duration: Duration?,
    val origin: Origin,
    val expiry: SessionExpiry,
    val customSeconds: Long,
) {
    enum class Origin { LOCAL, PEER, BOTH }

    companion object {
        fun resolveLocal(contact: Contact, settings: AppSettings): Pair<SessionExpiry, Long> {
            val override = contact.localSessionExpiry
            return if (override != null) {
                override to contact.localCustomExpirySeconds
            } else {
                settings.sessionExpiry to settings.customExpirySeconds
            }
        }

        fun computeForContact(contact: Contact, settings: AppSettings): EffectiveExpiry {
            val (localExpiry, localSecs) = resolveLocal(contact, settings)
            return compute(
                localExpiry = localExpiry,
                localCustomSeconds = localSecs,
                peerExpiry = contact.peerSessionExpiry,
                peerCustomSeconds = contact.peerCustomExpirySeconds,
            )
        }

        fun compute(
            localExpiry: SessionExpiry,
            localCustomSeconds: Long,
            peerExpiry: SessionExpiry,
            peerCustomSeconds: Long,
        ): EffectiveExpiry {
            val localDur = toDuration(localExpiry, localCustomSeconds)
            val peerDur = toDuration(peerExpiry, peerCustomSeconds)

            if (localDur == null && peerDur == null) {
                return EffectiveExpiry(null, Origin.BOTH, SessionExpiry.NEVER, 0L)
            }
            if (localDur == null && peerDur != null) {
                return EffectiveExpiry(peerDur, Origin.PEER, peerExpiry, peerCustomSeconds)
            }
            if (peerDur == null && localDur != null) {
                return EffectiveExpiry(localDur, Origin.LOCAL, localExpiry, localCustomSeconds)
            }
            return when {
                localDur!! < peerDur!! -> EffectiveExpiry(localDur, Origin.LOCAL, localExpiry, localCustomSeconds)
                peerDur < localDur -> EffectiveExpiry(peerDur, Origin.PEER, peerExpiry, peerCustomSeconds)
                else -> EffectiveExpiry(localDur, Origin.BOTH, localExpiry, localCustomSeconds)
            }
        }

        private fun toDuration(expiry: SessionExpiry, customSeconds: Long): Duration? = when (expiry) {
            SessionExpiry.NEVER -> null
            SessionExpiry.ONE_DAY -> 1.days
            SessionExpiry.SEVEN_DAYS -> 7.days
            SessionExpiry.THIRTY_DAYS -> 30.days
            SessionExpiry.CUSTOM -> customSeconds.coerceAtLeast(1L).seconds
        }
    }
}
