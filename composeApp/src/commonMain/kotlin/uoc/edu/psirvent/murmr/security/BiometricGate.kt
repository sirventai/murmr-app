package uoc.edu.psirvent.murmr.security

sealed interface BiometricResult {
    data object Success : BiometricResult
    data object Cancelled : BiometricResult
    data class NotAvailable(val reason: String) : BiometricResult
    data class Error(val message: String) : BiometricResult
}

expect class BiometricGate() {
    suspend fun authenticate(reason: String = "Desbloquea Murmr"): BiometricResult
    fun isAvailable(): Boolean
}
