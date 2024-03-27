package net.spaceeye.vmod.fabric

import dev.architectury.platform.Platform
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
}