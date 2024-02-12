package net.spaceeye.vsource

import dev.architectury.platform.Platform
import net.spaceeye.vsource.networking.SynchronisedRenderingData
import net.spaceeye.vsource.utils.LevelEvents
import net.spaceeye.vsource.utils.closeClientObjects
import net.spaceeye.vsource.utils.closeServerObjects
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun LOG(s: String) = VS.logger.warn(s)

object VS {
    const val MOD_ID = "vsource"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    @JvmStatic
    fun init() {
        if (!Platform.isModLoaded("valkyrienskies")) {
            LOG("VALKYRIEN SKIES IS NOT INSTALLED. EXITING.")
            return
        }
        SynchronisedRenderingData

        VSItems.register()

        makeClosingEvents()
    }

    @JvmStatic
    fun makeClosingEvents() {
        LevelEvents.clientDisconnectEvent.on {
            _, _ ->
            closeClientObjects()
        }

        LevelEvents.serverStopEvent.on {
            _, _ ->
            closeServerObjects()
        }
    }
}