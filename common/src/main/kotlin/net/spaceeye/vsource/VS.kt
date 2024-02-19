package net.spaceeye.vsource

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.platform.Platform
import net.spaceeye.vsource.items.Toolgun
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.toolgun.ToolgunModes
import net.spaceeye.vsource.utils.ServerLevelHolder
import net.spaceeye.vsource.utils.closeClientObjects
import net.spaceeye.vsource.utils.closeServerObjects
import net.spaceeye.vsource.utils.constraintsSaving.ConstraintManager
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
        ToolgunModes

        VSItems.register()

        makeEvents()
    }

    @JvmStatic
    fun makeEvents() {
        ClientLifecycleEvent.CLIENT_STOPPING.register {
            closeClientObjects()
        }

        LifecycleEvent.SERVER_STOPPING.register {
            closeServerObjects()
        }

        LifecycleEvent.SERVER_STARTED.register {
            server ->
            ServerLevelHolder.server = server
            ServerLevelHolder.serverLevel = server.overworld()
            ConstraintManager.forceNewInstance(server.overworld())
        }

        Toolgun.makeEvents()
    }
}