package net.spaceeye.vsource.networking

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vsource.LOG

class TestPacket: Packet {
    constructor()
    constructor(buf: FriendlyByteBuf) {deserialize(buf)}

    override fun serialize(): FriendlyByteBuf {
        return getBuffer()
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        LOG("DESERIALIZED TEST PACKET")
    }
}

class TestC2SConnection(id: String): C2SConnection<TestPacket>(id, "test_connection") {
    override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
        LOG("SERVER RECEIVED TEST PACKET")
        val packet = TestPacket(buf)
        LOG("SENDING RESPONSE TO CLIENT")
        Networking.Server.TEST_S2C_CONNECTION.sendToClient(context.player as ServerPlayer, packet)
    }
}

class TestS2CConnection(id: String): S2CConnection<TestPacket>(id, "test_connection") {
    override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
        LOG("CLIENT RECEIVED TEST PACKET")
        val packet = TestPacket(buf)
    }
}