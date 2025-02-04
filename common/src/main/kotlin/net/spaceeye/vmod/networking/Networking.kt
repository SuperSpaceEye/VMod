package net.spaceeye.vmod.networking

import dev.architectury.networking.NetworkManager
import dev.architectury.networking.NetworkManager.NetworkReceiver
import dev.architectury.networking.NetworkManager.PacketContext
import dev.architectury.networking.NetworkManager.Side
import dev.architectury.utils.Env
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.reflectable.constructor
import java.security.MessageDigest

interface Connection {
    val side: Side
    val id: ResourceLocation
    fun getHandler(): NetworkReceiver
}

interface Serializable {
    fun serialize(): FriendlyByteBuf
    fun deserialize(buf: FriendlyByteBuf)

    fun getBuffer() = FriendlyByteBuf(Unpooled.buffer(64))
}

private val hasher = MessageDigest.getInstance("MD5")
fun Serializable.hash() = hasher.digest(serialize().accessByteBufWithCorrectSize())

val registeredIDs = mutableSetOf<String>()

@Deprecated("For internal use. You should probably not use this", replaceWith = ReplaceWith("regS2C"))
inline infix fun <TT: Serializable> String.idWithConns(constructor: (String) -> S2CConnection<TT>): S2CConnection<TT> {
    val instance = constructor(this)
    if (registeredIDs.contains(instance.id.toString())) {return instance}
    registeredIDs.add(instance.id.toString())
    try { // Why? so that if it's registered on dedicated client/server it won't die
        NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
    } catch(e: NoSuchMethodError) {}
    return instance
}

@Deprecated("For internal use. You should probably not use this", replaceWith = ReplaceWith("regC2S"))
inline infix fun <TT: Serializable> String.idWithConnc(constructor: (String) -> C2SConnection<TT>): C2SConnection<TT> {
    val instance = constructor(this)
    if (registeredIDs.contains(instance.id.toString())) {return instance}
    registeredIDs.add(instance.id.toString())
    try { // Why? so that if it's registered on dedicated client/server it won't die
        NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
    } catch(e: NoSuchMethodError) {}
    return instance
}

inline fun <T: Serializable> registerTR(id: String, registeringSide: Side, constructor: (String) -> TRConnection<T>): TRConnection<T> {
    val instance = constructor(id)
    try {
        // handler should be on the opposite side of the intended invocation side.
        if (registeringSide == instance.invocationSide.opposite()) {
            if (registeredIDs.contains(instance.id.toString())) {return instance}
            registeredIDs.add(instance.id.toString())
            NetworkManager.registerReceiver(instance.invocationSide, instance.id, instance.getHandler())
        }
    } catch (e: NoSuchMethodError) {}
    return instance
}

fun Side.opposite() = when (this) {
    Side.S2C -> Side.C2S
    Side.C2S -> Side.S2C
}

abstract class C2SConnection<T : Serializable>(id: String, connectionName: String): Connection {
    override val side: Side = Side.C2S
    override val id = ResourceLocation(VM.MOD_ID, "c2s_${connectionName}_$id")

    override fun getHandler(): NetworkReceiver = NetworkReceiver(::serverHandler)
    abstract fun serverHandler(buf: FriendlyByteBuf, context: PacketContext)

    fun sendToServer(packet: T) = NetworkManager.sendToServer(id, packet.serialize())
}

abstract class S2CConnection<T : Serializable>(id: String, connectionName: String): Connection {
    override val side: Side = Side.S2C
    override val id = ResourceLocation(VM.MOD_ID, "s2c_${connectionName}_$id")

    override fun getHandler(): NetworkReceiver = NetworkReceiver(::clientHandler)
    abstract fun clientHandler(buf: FriendlyByteBuf, context: PacketContext)

    fun sendToClient(player: ServerPlayer, packet: T) = NetworkManager.sendToPlayer(player, id, packet.serialize())
    fun sendToClients(players: Iterable<ServerPlayer>, packet: T) = NetworkManager.sendToPlayers(players, id, packet.serialize())
}

@Deprecated("For internal use. You should probably not use this", replaceWith = ReplaceWith("regC2S"))
inline fun <reified T: Serializable>makeC2S(name: String, connName: String,
                                            crossinline doProcess: (ServerPlayer) -> Boolean,
                                            crossinline rejectCallback: (ServerPlayer) -> Unit = {},
                                            crossinline fn: (pkt: T, player: ServerPlayer) -> Unit ) =
    object : C2SConnection<T>(name, connName) {
        override fun serverHandler(buf: FriendlyByteBuf, context: PacketContext) {
            val player = context.player as ServerPlayer
            if (!doProcess(player)) {return rejectCallback(player)}

            val pkt = T::class.constructor(buf)
            pkt.deserialize(buf)
            fn(pkt, context.player as ServerPlayer)
        }
    }

@Deprecated("For internal use. You should probably not use this", replaceWith = ReplaceWith("regC2S"))
inline fun <reified T: Serializable>makeC2S(name: String, connName: String, crossinline fn: (pkt: T, player: ServerPlayer) -> Unit ) =
    object : C2SConnection<T>(name, connName) {
        override fun serverHandler(buf: FriendlyByteBuf, context: PacketContext) {
            val pkt = T::class.constructor(buf)
            pkt.deserialize(buf)
            fn(pkt, context.player as ServerPlayer)
        }
    }

@Deprecated("For internal use. You should probably not use this", replaceWith = ReplaceWith("regS2C"))
inline fun <reified T: Serializable>makeS2C(name: String, connName: String, crossinline fn: (pkt: T) -> Unit ) =
    object : S2CConnection<T>(name, connName) {
        override fun clientHandler(buf: FriendlyByteBuf, context: PacketContext) {
            val pkt = T::class.constructor(buf)
            pkt.deserialize(buf)
            fn(pkt)
        }
    }

inline fun <reified T: Serializable>regC2S(name: String, connName: String, crossinline doProcess: (ServerPlayer) -> Boolean, crossinline rejectCallback: (ServerPlayer) -> Unit = {}, crossinline fn: (pkt: T, player: ServerPlayer) -> Unit) = name idWithConnc { makeC2S(it, connName, doProcess, rejectCallback, fn)}
inline fun <reified T: Serializable>regC2S(name: String, connName: String, crossinline fn: (pkt: T, player: ServerPlayer) -> Unit) = name idWithConnc { makeC2S(it, connName, fn)}
inline fun <reified T: Serializable>regS2C(name: String, connName: String, crossinline fn: (pkt: T) -> Unit) = name idWithConns { makeS2C(it, connName, fn)}

abstract class TRConnection<T : Serializable>(id: String, connectionName: String, val invocationSide: Side): Connection {
    override val side: Side = invocationSide
    override val id: ResourceLocation = ResourceLocation(VM.MOD_ID, "tr_${connectionName}_$id")

    override fun getHandler(): NetworkReceiver = NetworkReceiver(::handlerFn)
    abstract fun handlerFn(buf: FriendlyByteBuf, context: PacketContext)

    fun transmitData(packet: T, context: PacketContext? = null) = when (invocationSide) {
        Side.S2C -> NetworkManager.sendToPlayer(context!!.player as ServerPlayer, id, packet.serialize())
        Side.C2S -> NetworkManager.sendToServer(id, packet.serialize())
    }
}

class FakePacketContext(val _player: ServerPlayer? = null): PacketContext {
    override fun getPlayer(): Player {
        if (_player != null) {return _player}
        throw AssertionError("Shouldn't be invoked")
    }
    override fun queue(runnable: Runnable?) { throw AssertionError("Shouldn't be invoked") }
    override fun getEnvironment(): Env { throw AssertionError("Shouldn't be invoked") }
}