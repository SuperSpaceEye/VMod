package net.spaceeye.vsource.networking

import dev.architectury.networking.NetworkManager
import dev.architectury.networking.NetworkManager.NetworkReceiver
import dev.architectury.networking.NetworkManager.PacketContext
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vsource.VS

object Networking {
    object Client {
        val TEST_C2S_CONNECTION = "test" idWithConn ::TestC2SConnection
    }
    object Server {
        val TEST_S2C_CONNECTION = "test" idWithConn ::TestS2CConnection
    }

    private infix fun <T: Packet> String.idWithConn(constructor: (String) -> C2SConnection<T>): C2SConnection<T> {
        val instance = constructor(this)
        NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        return instance
    }

    private infix fun <T: Packet> String.idWithConn(constructor: (String) -> S2CConnection<T>): S2CConnection<T> {
        val instance = constructor(this)
        NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        return instance
    }
}

interface PacketConn {
    val side: NetworkManager.Side
    val id: ResourceLocation
    fun getHandler(): NetworkReceiver
}

interface Packet {
    fun serialize(): FriendlyByteBuf
    fun deserialize(buf: FriendlyByteBuf)

    fun getBuffer() = FriendlyByteBuf(Unpooled.buffer())
}

abstract class C2SConnection<T : Packet>(id: String, connectionName: String): PacketConn {
    override val side: NetworkManager.Side = NetworkManager.Side.C2S
    override val id = ResourceLocation(VS.MOD_ID, "c2s$connectionName$id")

    override fun getHandler(): NetworkReceiver = NetworkReceiver(::serverHandler)
    abstract fun serverHandler(buf: FriendlyByteBuf, context: PacketContext)

    fun sendToServer(packet: T) = NetworkManager.sendToServer(id, packet.serialize())
}

abstract class S2CConnection<T : Packet>(id: String, connectionName: String): PacketConn {
    override val side: NetworkManager.Side = NetworkManager.Side.S2C
    override val id = ResourceLocation(VS.MOD_ID, "s2c$connectionName$id")

    override fun getHandler(): NetworkReceiver = NetworkReceiver(::clientHandler)
    abstract fun clientHandler(buf: FriendlyByteBuf, context: PacketContext)

    fun sendToClient(player: ServerPlayer, packet: T) = NetworkManager.sendToPlayer(player, id, packet.serialize())
    fun sendToClients(players: Iterable<ServerPlayer>, packet: T) = NetworkManager.sendToPlayers(players, id, packet.serialize())
}