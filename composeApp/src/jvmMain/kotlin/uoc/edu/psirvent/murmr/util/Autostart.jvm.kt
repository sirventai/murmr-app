package uoc.edu.psirvent.murmr.util

actual object Autostart {
    actual val alias: String? = System.getenv("MURMR_ALIAS")?.takeIf { it.isNotBlank() }
}
