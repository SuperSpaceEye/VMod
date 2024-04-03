package net.spaceeye.vmod

import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.platform.Platform
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.config.ConfigDelegateRegister
import net.spaceeye.vmod.constraintsManaging.ConstraintManager
import net.spaceeye.vmod.gui.SimpleMessagerNetworking
import net.spaceeye.vmod.rendering.SynchronisedRenderingData
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.utils.ServerLevelHolder
import net.spaceeye.vmod.utils.closeClientObjects
import net.spaceeye.vmod.utils.closeServerObjects
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun ILOG(s: String) = VM.logger.info(s)
fun WLOG(s: String) = VM.logger.warn(s)
fun DLOG(s: String) = VM.logger.debug(s)
fun ELOG(s: String) = VM.logger.error(s)

object VM {
    const val MOD_ID = "valkyrien_mod"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    @JvmStatic
    fun init() {
        if (!Platform.isModLoaded("valkyrienskies")) {
            WLOG("VALKYRIEN SKIES IS NOT INSTALLED. NOT INITIALIZING THE MOD.")
            return
        }
        ConfigDelegateRegister.initConfig()

        SynchronisedRenderingData
        SimpleMessagerNetworking
        ToolgunModes
        ServerToolGunState
        ClientToolGunState

        VMBlocks.register()
        VMBlockEntities.register()
        VMItems.register()

        makeEvents()
    }

    var serverStopping = false

    @JvmStatic
    fun makeEvents() {
        LifecycleEvent.SERVER_LEVEL_SAVE.register {
            if (it != ServerLevelHolder.overworldServerLevel) {return@register}
            ConstraintManager.setDirty()
        }

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register {
            if (it != Minecraft.getInstance().player || it == null) {return@register}
            closeClientObjects()
        }

        LifecycleEvent.SERVER_STOPPING.register {
            serverStopping = true
        }

        LifecycleEvent.SERVER_STOPPED.register {
            closeServerObjects()
        }

        LifecycleEvent.SERVER_STARTED.register {
            server ->
            serverStopping = false
            ServerLevelHolder.server = server
            ServerLevelHolder.overworldServerLevel = server.overworld()
            ConstraintManager.initNewInstance()
        }

        ToolgunItem.makeEvents()
    }
}