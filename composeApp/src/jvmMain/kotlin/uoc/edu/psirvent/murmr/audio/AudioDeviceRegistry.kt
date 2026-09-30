package uoc.edu.psirvent.murmr.audio

import java.io.File
import java.util.Properties
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AudioDevice(val name: String, val description: String) {
    val displayName: String
        get() = if (description.isBlank() || description == name) name else "$name - $description"
}

object AudioDeviceRegistry {

    private const val INPUT_KEY = "input"
    private const val OUTPUT_KEY = "output"

    private val format = AudioFormat(48_000f, 16, 1, true, false)
    private val outputInfo = DataLine.Info(SourceDataLine::class.java, format)
    private val inputInfo = DataLine.Info(TargetDataLine::class.java, format)

    private val configFile: File by lazy {
        val home = System.getProperty("user.home") ?: "."
        val dir = File(home, ".murmr").apply { mkdirs() }
        File(dir, "audio.properties")
    }

    private val _inputName = MutableStateFlow(loadKey(INPUT_KEY))
    val inputName: StateFlow<String?> = _inputName

    private val _outputName = MutableStateFlow(loadKey(OUTPUT_KEY))
    val outputName: StateFlow<String?> = _outputName

    fun availableInputs(): List<AudioDevice> = AudioSystem.getMixerInfo()
        .filter { mixerSupportsLine(it, inputInfo) }
        .map { AudioDevice(it.name, it.description) }
        .distinctBy { it.name }

    fun availableOutputs(): List<AudioDevice> = AudioSystem.getMixerInfo()
        .filter { mixerSupportsLine(it, outputInfo) }
        .map { AudioDevice(it.name, it.description) }
        .distinctBy { it.name }

    fun selectInput(name: String?) {
        _inputName.value = name?.takeIf { it.isNotBlank() }
        save()
    }

    fun selectOutput(name: String?) {
        _outputName.value = name?.takeIf { it.isNotBlank() }
        save()
    }

    fun resolveInputMixer(): Mixer? = resolveMixer(_inputName.value, inputInfo)

    fun resolveOutputMixer(): Mixer? = resolveMixer(_outputName.value, outputInfo)

    private fun resolveMixer(name: String?, lineInfo: DataLine.Info): Mixer? {
        if (name.isNullOrBlank()) return null
        val info = AudioSystem.getMixerInfo().firstOrNull { it.name == name } ?: return null
        if (!mixerSupportsLine(info, lineInfo)) return null
        return runCatching { AudioSystem.getMixer(info) }.getOrNull()
    }

    private fun mixerSupportsLine(info: Mixer.Info, lineInfo: DataLine.Info): Boolean = try {
        AudioSystem.getMixer(info).isLineSupported(lineInfo)
    } catch (_: Exception) {
        false
    }

    private fun loadKey(key: String): String? {
        if (!configFile.exists()) return null
        return runCatching {
            val props = Properties()
            configFile.inputStream().use { props.load(it) }
            props.getProperty(key)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun save() {
        runCatching {
            val props = Properties()
            _inputName.value?.let { props.setProperty(INPUT_KEY, it) }
            _outputName.value?.let { props.setProperty(OUTPUT_KEY, it) }
            configFile.outputStream().use { props.store(it, "Murmr audio device preferences") }
        }
    }
}
