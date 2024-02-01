package net.spaceeye.vssource

import dev.architectury.platform.Platform
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun LOG(s: String) = VSS.logger.warn(s)

object VSS {
    const val MOD_ID = "vs_source"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    @JvmStatic
    fun init() {
        if (!Platform.isModLoaded("valkyrienskies")) {
            LOG("VALKYRIEN SKIES IS NOT INSTALLED. EXITING.")
            return
        }

        VSSItems.register()
    }
}