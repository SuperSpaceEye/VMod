package net.spaceeye.vmod.forge

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.spaceeye.vmod.config.AbstractConfigBuilder

object PlatformUtilsImpl {
    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = ForgeConfigBuilder()

    @JvmStatic
    fun initScreen(screen: Screen) {
        val minecraft = Minecraft.getInstance()
        screen.init(minecraft, minecraft.window.guiScaledWidth, minecraft.window.guiScaledHeight)
    }

    @JvmStatic
    fun renderScreen(screen: Screen, stack: PoseStack, mouseX: Int, mouseY: Int, delta: Float) {
        screen.render(stack, mouseX, mouseY, delta)
    }
}