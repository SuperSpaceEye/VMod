package net.spaceeye.vmod

import dev.architectury.injectables.annotations.ExpectPlatform
import net.spaceeye.vmod.config.AbstractConfigBuilder

object PlatformUtils {
    @ExpectPlatform
    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = throw AssertionError()
}