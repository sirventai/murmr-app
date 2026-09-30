package uoc.edu.psirvent.murmr.audio

class PreEmphasisFilter(private val alpha: Float = 0.7f) {

    fun applyInPlace(samples: ShortArray) {
        if (samples.isEmpty()) return
        var prev = 0f
        for (i in samples.indices) {
            val x = samples[i].toFloat()
            val y = x - alpha * prev
            prev = x
            samples[i] = when {
                y > Short.MAX_VALUE -> Short.MAX_VALUE
                y < Short.MIN_VALUE -> Short.MIN_VALUE
                else -> y.toInt().toShort()
            }
        }
    }
}
