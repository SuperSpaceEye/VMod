package net.spaceeye.vmod

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.spaceeye.vmod.config.AbstractConfigBuilder

object PlatformUtils {
    @ExpectPlatform
    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = throw AssertionError()

    @ExpectPlatform
    @JvmStatic
    fun initScreen(screen: Screen) { throw AssertionError() }

    @ExpectPlatform
    @JvmStatic
    fun renderScreen(screen: Screen, stack: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) { throw AssertionError() }
}