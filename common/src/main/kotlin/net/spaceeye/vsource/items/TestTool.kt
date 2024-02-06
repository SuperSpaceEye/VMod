package net.spaceeye.vsource.items

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.spaceeye.vsource.rendering.RenderEvents
import net.spaceeye.vsource.utils.CARenderType

class TestTool: BaseTool() {
    override fun activatePrimaryFunction(level: Level, player: Player, clipResult: BlockHitResult) {
        when (level) {
            is ClientLevel -> {
                RenderEvents.WORLD.register {
                    poseStack, buffer ->
                    val ivertexBuilder = buffer.getBuffer(CARenderType.WIRE)

                }
            }
            is ServerLevel -> {
            }
        }

    }

    override fun resetState() {}
}