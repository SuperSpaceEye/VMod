package net.spaceeye.vmod.limits

import com.fasterxml.jackson.annotation.JsonIgnore
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.config.ExternalDataUtil
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.getMapper
import kotlin.math.max
import kotlin.math.min

data class DoubleLimit(var minValue: Double = -Double.MAX_VALUE, var maxValue: Double = Double.MAX_VALUE) { fun get(num: Double) = max(minValue, min(maxValue, num)) }
data class FloatLimit (var minValue: Float  = -Float.MAX_VALUE,  var maxValue: Float  = Float.MAX_VALUE ) { fun get(num: Float)  = max(minValue, min(maxValue, num)) }
data class IntLimit   (var minValue: Int    =  Int.MIN_VALUE,    var maxValue: Int    = Int.MAX_VALUE   ) { fun get(num: Int)    = max(minValue, min(maxValue, num)) }

data class StrLimit   (var sizeLimit:Int = Int.MAX_VALUE) {
    fun get(str: String): String {
        if (str.length <= sizeLimit) { return str }
        return str.dropLast(str.length - sizeLimit)
    }
}

class ServerLimitsInstance: AutoSerializable {
    @JsonIgnore private var i = 0

    val maxForce: FloatLimit by get(i++, FloatLimit())
    val restitution: FloatLimit by get(i++, FloatLimit(0f, 1f))
    val stiffness: FloatLimit by get(i++, FloatLimit(0f))
    val damping: FloatLimit by get(i++, FloatLimit())

    val fixedDistance: FloatLimit by get(i++, FloatLimit())
    val extensionDistance: FloatLimit by get(i++, FloatLimit(0.001f))
    val extensionSpeed: FloatLimit by get(i++, FloatLimit(0.001f))
    val distanceFromBlock: DoubleLimit by get(i++, DoubleLimit(0.0001))
    val stripRadius: DoubleLimit by get(i++, DoubleLimit(0.0, 10.0))
    val scale: DoubleLimit by get(i++, DoubleLimit(0.001))
    val precisePlacementAssistSides: IntLimit by get(i++, IntLimit(2, 11))

    val physRopeSegments: IntLimit by get(i++, IntLimit(1, 100))
    val physRopeMassPerSegment: DoubleLimit by get(i++, DoubleLimit(0.01, 10000.0))
    val physRopeRadius: DoubleLimit by get(i++, DoubleLimit(0.01, 10.0))

    val channelLength: StrLimit by get(i++, StrLimit(50))

    val thrusterScale: DoubleLimit by get(i++, DoubleLimit(0.1, 10.0))
}

object ServerLimits {
    init {
        SerializableItem.registerSerializationItem(DoubleLimit::class, {it, buf -> buf.writeDouble(it.minValue); buf.writeDouble(it.maxValue) }) {buf -> DoubleLimit(buf.readDouble(), buf.readDouble())}
        SerializableItem.registerSerializationItem(FloatLimit::class, {it, buf -> buf.writeFloat(it.minValue); buf.writeFloat(it.maxValue) }) {buf -> FloatLimit(buf.readFloat(), buf.readFloat())}
        SerializableItem.registerSerializationItem(IntLimit::class, {it, buf -> buf.writeInt(it.minValue); buf.writeInt(it.maxValue) }) {buf -> IntLimit(buf.readInt(), buf.readInt())}
        SerializableItem.registerSerializationItem(StrLimit::class, {it, buf -> buf.writeInt(it.sizeLimit)}) {buf -> StrLimit(buf.readInt())}
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
            ELOG("Failed to deserialize Server Limits")
            save(_instance)
        }
    }

    fun updateFromServer() { c2sRequestServerLimits.sendToServer(EmptyPacket()) }
    fun tryUpdateToServer() { c2sSendUpdatedServerLimits.sendToServer(instance) }

    private val c2sRequestServerLimits = regC2S<EmptyPacket>("request_server_limits", "server_limits") {pkt, player ->
        s2cSendCurrentServerLimits.sendToClient(player, instance)
    }

    private val s2cSendCurrentServerLimits = regS2C<ServerLimitsInstance>("send_current_server_limits", "server_limits") {
        instance = it
    }
    //TODO
    private val c2sSendUpdatedServerLimits = regC2S<ServerLimitsInstance>("send_updated_server_limits", "server_limits",
        {it.hasPermissions(4)}, { s2cServerLimitsUpdateWasRejected.sendToClient(it, EmptyPacket())}) { pkt, player ->
        instance = pkt
    }

    //TODO
    private val s2cServerLimitsUpdateWasRejected = regS2C<EmptyPacket>("server_limits_update_was_rejected", "server_limits") {
        ClientToolGunState.closeGUI()
        ClientToolGunState.addHUDError("Serve Limits update was rejected")
    }
}