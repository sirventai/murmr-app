package uoc.edu.psirvent.murmr.data.network

data class RelayConfig(val baseUrl: String) {
    companion object {
        const val DEFAULT = "http://murmr.cc:8345"
    }
}
