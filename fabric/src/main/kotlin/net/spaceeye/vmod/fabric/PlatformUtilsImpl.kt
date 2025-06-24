package net.spaceeye.vmod.fabric

import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.platform.Platform
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.config.AbstractConfigBuilder

object PlatformUtilsImpl {
    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder {
        if (Platform.isModLoaded("forgeconfigapiport")) {
            return FabricConfigBuilder()
        }
        WLOG("\"Forge Config API Port\" is not installed. Using dummy config builder.")
        return FabricDummyConfigBuilder()
    }

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