package net.spaceeye.vsource.items

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.utils.RaycastFunctions

class TestTool: BaseTool() {
    var blockPos: BlockPos? = null

    override fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {

    }

    override fun resetState() {
        ILOG("RESETTING STATE")
        blockPos = null
    }
}