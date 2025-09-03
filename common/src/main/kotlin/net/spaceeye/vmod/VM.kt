package net.spaceeye.vmod

import com.mojang.blaze3d.systems.RenderSystem
import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import kotlinx.coroutines.Runnable
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.compat.patchouli.PageGIF
import net.spaceeye.vmod.compat.schem.SchemCompatObj
import net.spaceeye.vmod.config.ConfigDelegateRegister
import net.spaceeye.vmod.vEntityManaging.VEntityManager
import net.spaceeye.vmod.vEntityManaging.VEntityTypes
import net.spaceeye.vmod.vEntityManaging.VEExtensionTypes
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.events.SessionEvents
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.gui.SimpleMessagerNetworking
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.network.MessageTypes
import net.spaceeye.vmod.reflectable.ByteSerializableItem
import net.spaceeye.vmod.reflectable.TagSerializableItem
import net.spaceeye.vmod.physgun.ClientPhysgunState
import net.spaceeye.vmod.physgun.PhysgunItem
import net.spaceeye.vmod.physgun.ServerPhysgunState
import net.spaceeye.vmod.rendering.RenderingTypes
import net.spaceeye.vmod.rendering.initRenderingData
import net.spaceeye.vmod.schematic.SchematicActionsQueue
import net.spaceeye.vmod.shipAttachments.VMAttachments
import net.spaceeye.vmod.toolgun.*
import net.spaceeye.vmod.toolgun.clientSettings.ClientSettingsTypes
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsTypes
import net.spaceeye.vmod.utils.ServerObjectsHolder
import net.spaceeye.vmod.utils.closeClientObjects
import net.spaceeye.vmod.utils.closeServerObjects
import net.spaceeye.vmod.vsStuff.VSGravityManager
import net.spaceeye.vmod.vsStuff.VSMasslessShipProcessor
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.opengl.GL20
import org.valkyrienskies.mod.common.shipObjectWorld
import vazkii.patchouli.client.book.ClientBookRegistry

fun ILOG(s: String) = VM.logger.info(s)
fun WLOG(s: String) = VM.logger.warn(s)
fun DLOG(s: String) = VM.logger.debug(s)
fun ELOG(s: String) = VM.logger.error(s)

const val MOD_ID = "the_vmod"

var GLMaxTextureSize: Int = -1
    get() {
    if (field != -1) return field
        val arr = IntArray(1)
        GL20.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, arr)
        field = arr[0]
        return field
    }

object VM {
    const val MOD_ID = "the_vmod"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    @JvmStatic
    fun init() {
        if (Platform.isModLoaded("patchouli")) {
            EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
                ClientBookRegistry.INSTANCE.pageTypes.put(ResourceLocation(net.spaceeye.vmod.MOD_ID, "gif_page"), PageGIF::class.java)
            } }
        }

        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            RenderSystem.recordRenderCall { GLMaxTextureSize }
        } }

        VMAttachments.register()
        ConfigDelegateRegister.initConfig()
        initRenderingData()
        initRegistries()

        SimpleMessagerNetworking
        ServerLimits
        ServerPhysgunState
        SchemCompatObj
        VSMasslessShipProcessor
        VMToolgun
        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            ScreenWindow
            ClientPhysgunState
            ClientSettingsTypes
        } }

        VMBlocks.register()
        VMBlockEntities.register()
        VMItems.register()
        VMEntities.register()

        CommandRegistrationEvent.EVENT.register { it, _, _-> VMCommands.registerServerCommands(it) }

        makeEvents()
    }

    var serverStopping = false

    @JvmStatic
    fun initRegistries() {
        VEntityTypes
        VEExtensionTypes
        MessageTypes
        RenderingTypes
        ToolgunModes
        ServerSettingsTypes
        ByteSerializableItem
        TagSerializableItem
    }

    @JvmStatic
    fun makeEvents() {
        PersistentEvents
        SessionEvents
        SchematicActionsQueue

        LifecycleEvent.SERVER_LEVEL_SAVE.register {
            if (it != ServerObjectsHolder.overworldServerLevel) {return@register}
            VEntityManager.setDirty()
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
            ServerObjectsHolder.server = server
            ServerObjectsHolder.overworldServerLevel = server.overworld()
            ServerObjectsHolder.shipObjectWorld = server.shipObjectWorld
            VEntityManager.initNewInstance()

            VSGravityManager
            PlayerAccessManager.afterInit()
        }

        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register {
            if (it != Minecraft.getInstance().player || it == null) {return@register}
            closeClientObjects()
        }
        }}

        PhysgunItem.makeEvents()
    }
}