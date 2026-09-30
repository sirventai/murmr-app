package uoc.edu.psirvent.murmr

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
