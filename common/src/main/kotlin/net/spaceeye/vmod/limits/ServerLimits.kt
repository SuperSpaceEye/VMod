package net.spaceeye.vmod.limits

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.config.ExternalDataUtil
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.utils.EmptyPacket
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
    val compliance: DoubleLimit by get(0, DoubleLimit(1e-300, 1.0))
    val maxForce: FloatLimit by get(1, FloatLimit(1.0f))
    val fixedDistance: DoubleLimit by get(2, DoubleLimit())
    val extensionDistance: DoubleLimit by get(3, DoubleLimit(0.001))
    val extensionSpeed: DoubleLimit by get(4, DoubleLimit(0.001))
    val distanceFromBlock: DoubleLimit by get(5, DoubleLimit(0.0001))
    val stripRadius: DoubleLimit by get(6, DoubleLimit(0.0, 10.0))
    val scale: DoubleLimit by get(7, DoubleLimit(0.001))
    val precisePlacementAssistSides: IntLimit by get(8, IntLimit(2, 11))

    val physRopeSegments: IntLimit by get(9, IntLimit(1, 100))
    val physRopeMassPerSegment: DoubleLimit by get(10, DoubleLimit(0.01, 10000.0))
    val physRopeRadius: DoubleLimit by get(11, DoubleLimit(0.01, 10.0))

    val channelLength: StrLimit by get(12, StrLimit(50))

    val thrusterScale: DoubleLimit by get(13, DoubleLimit(0.1, 10.0))
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
        //TODO this is stupid and needs rework
        val arr = value.serialize().accessByteBufWithCorrectSize()
        ExternalDataUtil.writeObject("ServerLimits", arr)
    }

    private fun load() {
        val bytes = ExternalDataUtil.readObject("ServerLimits") ?: run {
            save(_instance)
            return
        }
        val buf = FriendlyByteBuf(Unpooled.wrappedBuffer(bytes))
        try {
            val temp = ServerLimitsInstance()
            temp.deserialize(buf)
            _instance = temp
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