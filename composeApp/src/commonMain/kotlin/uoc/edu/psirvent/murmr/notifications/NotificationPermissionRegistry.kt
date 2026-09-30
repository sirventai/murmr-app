package uoc.edu.psirvent.murmr.notifications

object NotificationPermissionRegistry {

    var requester: (() -> Unit)? = null

    var hasAsked: () -> Boolean = { false }
    var markAsked: () -> Unit = {}

    fun requestIfNeeded() {
        if (hasAsked()) return
        val r = requester ?: return
        markAsked()
        r.invoke()
    }
}
