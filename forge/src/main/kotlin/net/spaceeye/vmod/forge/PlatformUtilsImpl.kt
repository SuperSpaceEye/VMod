package net.spaceeye.vmod.forge

import net.spaceeye.vmod.config.AbstractConfigBuilder

object PlatformUtilsImpl {
    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = ForgeConfigBuilder()
}