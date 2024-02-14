package net.spaceeye.vsource

import dev.architectury.platform.Platform
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.events.LevelEvents
import net.spaceeye.vsource.utils.closeClientObjects
import net.spaceeye.vsource.utils.closeServerObjects
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun ILOG(s: String) = VS.logger.info(s)
fun WLOG(s: String) = VS.logger.warn(s)
fun DLOG(s: String) = VS.logger.debug(s)

object VS {
    const val MOD_ID = "vsource"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    @JvmStatic
    fun init() {
        if (!Platform.isModLoaded("valkyrienskies")) {
            WLOG("VALKYRIEN SKIES IS NOT INSTALLED. NOT INITIALIZING THE MOD.")
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