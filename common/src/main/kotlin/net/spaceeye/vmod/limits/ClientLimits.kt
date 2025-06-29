package net.spaceeye.vmod.limits

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.config.ExternalDataUtil
import net.spaceeye.vmod.utils.getMapper
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject

class ClientLimitsInstance: ReflectableObject {
    @JsonIgnore private var i = 0

    var lineRendererWidth    : DoubleLimit by get(i++, DoubleLimit(0.0, 5.0))
    var ropeRendererWidth    : DoubleLimit by get(i++, DoubleLimit(0.0, 5.0))
    var blockRendererScale   : FloatLimit  by get(i++, FloatLimit(0.001f, 10f))
    var ropeRendererSegments : IntLimit    by get(i++, IntLimit(1, 128))
    var physRopeSides        : IntLimit    by get(i++, IntLimit(2, 10))

    var tubeRopeRendererSegments : IntLimit by get(i++, IntLimit(1, 128))
    var tubeRopeRendererSides    : IntLimit by get(i++, IntLimit(1, 32))
    var tubeRopeRendererWidth    : DoubleLimit by get(i++, DoubleLimit(0.0, 5.0))

    var lightingMode: BoolLimit by get(i++, BoolLimit())
}

@JsonIgnoreProperties(ignoreUnknown = true)
object ClientLimits {
    private var _instance = ClientLimitsInstance()

    var wasLoaded = false
    var instance: ClientLimitsInstance
        get() {
            if (!wasLoaded) {load(); wasLoaded = true}
            return _instance
        }
        set(value) {
            save(value)
            _instance = value
        }

    private fun save(value: ClientLimitsInstance) {
        val mapper = getMapper()
        ExternalDataUtil.writeObject("ClientLimits.json", mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value))
    }

    private fun load() {
        val bytes = ExternalDataUtil.readObject("ClientLimits.json") ?: run {
            save(_instance)
            return
        }
        try {
            val mapper = getMapper()
            _instance = mapper.readValue(bytes, ClientLimitsInstance::class.java)
        } catch (e: Exception) {
            ELOG("Failed to deserialize Client Limits.\n${e.stackTraceToString()}")
            _instance = ClientLimitsInstance()
            save(_instance)
        }
    }
}