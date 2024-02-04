package net.spaceeye.vsource.items

import dev.architectury.networking.NetworkManager
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.spaceeye.vsource.networking.Networking
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class TestTool: BaseTool() {
    override fun activatePrimaryFunction(level: Level, player: Player, clipResult: BlockHitResult) {
        when (level) {
            is ClientLevel -> {
                val buf = FriendlyByteBuf(Unpooled.buffer())
                NetworkManager.sendToServer(Networking.TEST_PACKET_ID, buf)
            }
            is ServerLevel -> {
            }
        }

    }

    override fun resetState() {}
}