package uoc.edu.psirvent.murmr.util

sealed interface ValidationResult {
    data object Ok : ValidationResult
    data class Invalid(val message: String) : ValidationResult

    fun errorOrNull(): String? = (this as? Invalid)?.message
    val isValid: Boolean get() = this is Ok
}

object Validators {

    private val ALIAS_REGEX = Regex("^[\\p{L}0-9 _-]+$")
    private val URL_REGEX = Regex("^(http|https)://[A-Za-z0-9.\\-]+(:[0-9]{1,5})?(/.*)?$")

    fun alias(input: String): ValidationResult {

        val sanitized = TextSanitizer.sanitize(input, allowNewlines = false)
        if (sanitized != input) {
            return ValidationResult.Invalid("Caracteres invisibles no permitidos")
        }
        val t = sanitized.trim()
        return when {
            t.isEmpty() -> ValidationResult.Invalid("Introduce un alias")
            t.length < 3 -> ValidationResult.Invalid("Mínimo 3 caracteres")
            t.length > 45 -> ValidationResult.Invalid("Máximo 45 caracteres")
            !ALIAS_REGEX.matches(t) -> ValidationResult.Invalid("Sin caracteres especiales")
            else -> ValidationResult.Ok
        }
    }

    fun relayUrl(input: String): ValidationResult {
        val t = input.trim().trimEnd('/')
        return when {
            t.isEmpty() -> ValidationResult.Invalid("URL vacía")
            t.length > 200 -> ValidationResult.Invalid("URL demasiado larga")
            !URL_REGEX.matches(t) -> ValidationResult.Invalid("Formato esperado: http(s)://host[:puerto]")
            else -> ValidationResult.Ok
        }
    }

    fun chatMessage(input: String): ValidationResult {
        val t = input.trim()
        return when {
            t.isEmpty() -> ValidationResult.Invalid("Mensaje vacío")
            t.length > 4_000 -> ValidationResult.Invalid("Máximo 4 000 caracteres")
            else -> ValidationResult.Ok
        }
    }

    fun autoLockSeconds(input: String): ValidationResult {
        val n = input.trim().toIntOrNull()
        return when {
            n == null -> ValidationResult.Invalid("Introduce un número entero de segundos")
            n < 1 -> ValidationResult.Invalid("Mínimo 1 segundo")
            n > 24 * 3_600 -> ValidationResult.Invalid("Máximo 24 horas (86 400 s)")
            else -> ValidationResult.Ok
        }
    }

    fun avatarBytes(bytes: ByteArray): ValidationResult {
        if (bytes.isEmpty()) return ValidationResult.Ok
        if (bytes.size > 10 * 1024 * 1024) {
            return ValidationResult.Invalid("Imagen demasiado grande (máx 10 MB)")
        }
        val type = detectImageMagic(bytes)
        return when (type) {
            ImageType.JPEG, ImageType.PNG, ImageType.WEBP -> ValidationResult.Ok
            ImageType.UNKNOWN -> ValidationResult.Invalid("Formato no reconocido (usa JPEG, PNG o WebP)")
        }
    }

    enum class ImageType { JPEG, PNG, WEBP, UNKNOWN }

    fun detectImageMagic(bytes: ByteArray): ImageType {
        if (bytes.size < 12) return ImageType.UNKNOWN
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) return ImageType.JPEG
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) return ImageType.PNG
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) return ImageType.WEBP
        return ImageType.UNKNOWN
    }
}
