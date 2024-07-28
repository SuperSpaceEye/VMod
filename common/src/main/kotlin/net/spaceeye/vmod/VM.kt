package net.spaceeye.vmod

import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.compat.schem.SchemCompatObj
import net.spaceeye.vmod.config.ConfigDelegateRegister
import net.spaceeye.vmod.constraintsManaging.ConstraintManager
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.gui.SimpleMessagerNetworking
import net.spaceeye.vmod.rendering.initRenderingData
import net.spaceeye.vmod.schematic.SchematicActionsQueue
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.sendHUDErrorToOperators
import net.spaceeye.vmod.utils.ServerLevelHolder
import net.spaceeye.vmod.utils.closeClientObjects
import net.spaceeye.vmod.utils.closeServerObjects
import net.spaceeye.vmod.vsStuff.VSGravityManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun ILOG(s: String) = VM.logger.info(s)
fun WLOG(s: String) = VM.logger.warn(s)
fun DLOG(s: String) = VM.logger.debug(s)
fun ELOG(s: String) = VM.logger.error(s)

// B is for broadcast
fun BWLOG(main: String, toBroadcast: String) {
    WLOG(main)
    sendHUDErrorToOperators(toBroadcast)
}

object VM {
    const val MOD_ID = "valkyrien_mod"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    @JvmStatic
    fun init() {
        ConfigDelegateRegister.initConfig()

        initRenderingData()
        VSGravityManager
        SimpleMessagerNetworking
        ToolgunModes
        ServerToolGunState
        ShipSchematic
        SchemCompatObj
        EnvExecutor.runInEnv(Env.CLIENT) { Runnable { ClientToolGunState } }

        VMBlocks.register()
        VMBlockEntities.register()
        VMItems.register()
        VMEntities.register()

        makeEvents()
    }

    var serverStopping = false

    @JvmStatic
    fun makeEvents() {
        RandomEvents
        SchematicActionsQueue

        LifecycleEvent.SERVER_LEVEL_SAVE.register {
            if (it != ServerLevelHolder.overworldServerLevel) {return@register}
            ConstraintManager.setDirty()
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

        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register {
            if (it != Minecraft.getInstance().player || it == null) {return@register}
            closeClientObjects()
        }
        }}

        ToolgunItem.makeEvents()
    }
}