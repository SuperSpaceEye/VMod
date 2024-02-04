package net.spaceeye.vsource.networking

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.LOG

object TestHandler: NetworkManager.NetworkReceiver {
    override fun receive(buf: FriendlyByteBuf?, context: NetworkManager.PacketContext?) {
        LOG("HALLO")
        context!!.player

    }
}