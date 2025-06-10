package net.spaceeye.vmod.limits

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dev.architectury.event.events.common.LifecycleEvent
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.config.ExternalDataUtil
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.reflectable.ByteSerializableItem
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.reflectable.deserialize
import net.spaceeye.vmod.reflectable.serialize
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.translate.SERVER_LIMITS_UPDATE_WAS_REJECTED
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.getMapper
import kotlin.math.max
import kotlin.math.min

data class DoubleLimit(var minValue: Double = -Double.MAX_VALUE, var maxValue: Double = Double.MAX_VALUE) { fun get(num: Double) = max(minValue, min(maxValue, num)) }
data class FloatLimit (var minValue: Float  = -Float.MAX_VALUE,  var maxValue: Float  = Float.MAX_VALUE ) { fun get(num: Float)  = max(minValue, min(maxValue, num)) }
data class IntLimit   (var minValue: Int    =  Int.MIN_VALUE,    var maxValue: Int    = Int.MAX_VALUE   ) { fun get(num: Int)    = max(minValue, min(maxValue, num)) }

data class BoolLimit  (var mode: Force = Force.NOTHING) {
    enum class Force {
        TRUE,
        FALSE,
        NOTHING
    }

    fun get(state: Boolean) = when(mode) {
        Force.TRUE -> true
        Force.FALSE -> false
        Force.NOTHING -> state
    }
}

data class StrLimit   (var sizeLimit:Int = Int.MAX_VALUE) {
    fun get(str: String): String {
        if (str.length <= sizeLimit) { return str }
        return str.dropLast(str.length - sizeLimit)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
open class ServerLimitsInstance: ReflectableObject {
    @JsonIgnore private var i = 0

    val maxForce : FloatLimit by get(i++, FloatLimit())
    val stiffness: FloatLimit by get(i++, FloatLimit())
    val damping  : FloatLimit by get(i++, FloatLimit())

    val precisePlacementAssistSides: IntLimit by get(i++, IntLimit(2, 11))
    val extensionDistance: FloatLimit by get(i++, FloatLimit(0.001f))
    val distanceFromBlock: DoubleLimit by get(i++, DoubleLimit(0.0))
    val extensionSpeed: FloatLimit by get(i++, FloatLimit(0.001f))
    val fixedDistance: FloatLimit by get(i++, FloatLimit())
    val thrusterForce: DoubleLimit by get(i++, DoubleLimit(1.0, 1e100))
    var massPerBlock: DoubleLimit by get(i++, DoubleLimit(1.0))
    val stripRadius: DoubleLimit by get(i++, DoubleLimit(0.0, 10.0))
    val maxDistance: DoubleLimit by get(i++, DoubleLimit(0.0, 100.0))
    val gearRatio: FloatLimit by get(i++, FloatLimit(0.001f))
    var massLimit: DoubleLimit by get(i++, DoubleLimit(Double.MIN_VALUE))
    val scale: DoubleLimit by get(i++, DoubleLimit(0.001))

    val physRopeSegments: IntLimit by get(i++, IntLimit(1, 100))
    val totalMassOfPhysRope: DoubleLimit by get(i++, DoubleLimit(0.01, Double.MAX_VALUE))
    val physRopeRadius: DoubleLimit by get(i++, DoubleLimit(0.01, 10.0))
    val physRopeAngleLimit: DoubleLimit by get(i++, DoubleLimit(0.0, 180.0))
    val physRopeSides: IntLimit by get(i++, IntLimit(2, 10))

    val channelLength: StrLimit by get(i++, StrLimit(50))

    val thrusterScale: DoubleLimit by get(i++, DoubleLimit(0.001, 10.0))
    val sensorScale: DoubleLimit by get(i++, DoubleLimit(0.001, 10.0))

    fun toPacket() = ServerLimitsPacket(this)
}

//why? jackson fails to get FriendlyByteBuf class in release for some reason and crashes, so i need to do this
class ServerLimitsPacket(): Serializable {
    constructor(instance: ServerLimitsInstance): this() {
        this.instance = instance
    }
    var instance: ServerLimitsInstance = ServerLimitsInstance()
    override fun serialize(): FriendlyByteBuf = instance.serialize()
    override fun deserialize(buf: FriendlyByteBuf) = instance.deserialize(buf)
}

object ServerLimits {
    init {
        ByteSerializableItem.registerSerializationItem(DoubleLimit::class, { it, buf -> buf.writeDouble(it.minValue); buf.writeDouble(it.maxValue) }) { buf -> DoubleLimit(buf.readDouble(), buf.readDouble())}
        ByteSerializableItem.registerSerializationItem(FloatLimit::class, { it, buf -> buf.writeFloat(it.minValue); buf.writeFloat(it.maxValue) }) { buf -> FloatLimit(buf.readFloat(), buf.readFloat())}
        ByteSerializableItem.registerSerializationItem(IntLimit::class, { it, buf -> buf.writeInt(it.minValue); buf.writeInt(it.maxValue) }) { buf -> IntLimit(buf.readInt(), buf.readInt())}
        ByteSerializableItem.registerSerializationItem(StrLimit::class, { it, buf -> buf.writeInt(it.sizeLimit)}) { buf -> StrLimit(buf.readInt())}
        ByteSerializableItem.registerSerializationItem(BoolLimit::class, { it, buf -> buf.writeEnum(it.mode) }) { buf -> BoolLimit(buf.readEnum(BoolLimit.Force::class.java)) }

        LifecycleEvent.SERVER_STOPPED.register {
            wasLoaded = false
        }
    }
    private var _instance = ServerLimitsInstance()
    var wasLoaded = false
    var instance: ServerLimitsInstance
        get() {
            if (!wasLoaded) {load(); wasLoaded = true}
            return _instance
        }
        set(value) {
            save(value)
            _instance = value
        }

    private fun save(value: ServerLimitsInstance) {
        val mapper = getMapper()
        ExternalDataUtil.writeObject("ServerLimits.json", mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value))
    }

    private fun load() {
        val bytes = ExternalDataUtil.readObject("ServerLimits.json") ?: run {
            save(_instance)
            return
        }
        try {
            val mapper = getMapper()
            _instance = mapper.readValue(bytes, ServerLimitsInstance::class.java)
        } catch (e: Exception) {
            ELOG("Failed to deserialize Server Limits.\n${e.stackTraceToString()}")
            _instance = ServerLimitsInstance()
            save(_instance)
        }
    }

    fun updateFromServer() { c2sRequestServerLimits.sendToServer(EmptyPacket()) }
    fun tryUpdateToServer() { c2sSendUpdatedServerLimits.sendToServer(instance.toPacket()) }

    private val c2sRequestServerLimits = regC2S<EmptyPacket>("request_server_limits", "server_limits") {pkt, player ->
        s2cSendCurrentServerLimits.sendToClient(player, instance.toPacket())
    }

    private val s2cSendCurrentServerLimits = regS2C<ServerLimitsPacket>("send_current_server_limits", "server_limits") {
        instance = it.instance
    }

    private val c2sSendUpdatedServerLimits = regC2S<ServerLimitsPacket>("send_updated_server_limits", "server_limits",
        {it.hasPermissions(4)}, { ServerToolGunState.sendErrorTo(it, SERVER_LIMITS_UPDATE_WAS_REJECTED) }) { pkt, player ->
        instance = pkt.instance
    }
}