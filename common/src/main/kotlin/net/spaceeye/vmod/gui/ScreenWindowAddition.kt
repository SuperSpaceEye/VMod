package net.spaceeye.vmod.gui

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.toolgun.ToolgunInstance

interface ServersideNetworking {
    fun initConnections(instance: ToolgunInstance)
}

abstract class ScreenWindowAddition() {
    lateinit var instance: ToolgunInstance
    abstract fun init(screenContainer: UIContainer)
    abstract fun onRenderHUD(stack: PoseStack, delta: Float)
}