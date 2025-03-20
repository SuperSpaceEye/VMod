package net.spaceeye.vmod.gui

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.components.UIContainer

interface ScreenWindowAddition {
    fun init(screenContainer: UIContainer)
    fun onRenderHUD(stack: PoseStack, delta: Float)
}