package net.spaceeye.vsource.items

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.VS
import net.spaceeye.vsource.gui.ExampleGui
import net.spaceeye.vsource.gui.ToolGunGUI
import net.spaceeye.vsource.utils.RaycastFunctions

class Toolgun: BaseTool() {
    var blockPos: BlockPos? = null

    override fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (level !is ClientLevel) {return}

        val gui = ToolGunGUI()

        Minecraft.getInstance().setScreen(gui)

//        VS.gui
    }

    override fun resetState() {
        ILOG("RESETTING STATE")
        blockPos = null
    }
}