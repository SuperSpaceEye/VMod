package net.spaceeye.vsource.networking

import dev.architectury.networking.NetworkManager
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vsource.VS

private val C2S = NetworkManager.Side.C2S
private val S2C = NetworkManager.Side.S2C

object Networking {
    val TEST_PACKET_ID = C2S.withID("test_packet").andHandler(TestHandler)

    infix fun NetworkManager.Side.withID(id: String) = Pair(this, id)
    infix fun Pair<NetworkManager.Side, String>.andHandler(handler: NetworkManager.NetworkReceiver): ResourceLocation {
        val location = ResourceLocation(VS.MOD_ID, this.second)
        NetworkManager.registerReceiver(this.first, location, handler)
        return location
    }
}