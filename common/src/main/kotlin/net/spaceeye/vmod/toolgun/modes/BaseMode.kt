package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIContainer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.gui.additions.HUDAddition
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.toolgun.ToolgunInstance
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.toolgun.modes.util.serverTryActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import java.util.function.Supplier

interface GUIBuilder {
    val itemName: Component
    fun makeGUISettings(parentWindow: UIContainer)
}

interface HUDBuilder {
    fun makeHUD(screen: UIContainer) {}
}

interface ClientEventsHandler {
    fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean { return false }
    fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult { return EventResult.pass() }
    fun onMouseScrollEvent(amount: Double): EventResult { return EventResult.pass() }

    fun onOpenMode() {}
    fun onCloseMode() {}
}

interface MSerializable: AutoSerializable {
    fun serverSideVerifyLimits() {
        getAllReflectableItems().forEach { it.setValue(null, null, (it.metadata["verification"] as? (Any) -> Any)?.invoke(it.it!!) ?: it.it!!) }
    }
}

interface BaseMode : MSerializable, GUIBuilder, HUDBuilder, ClientEventsHandler {
    var instance: ToolgunInstance

    fun resetState() {}

    fun init(type: BaseNetworking.EnvType) {}

    fun refreshHUD() { HUDAddition.refreshHUD() }
}

fun <T: BaseMode> BaseMode.registerConnection(mode: T, name: String, toExecute: (item: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit) =
    name idWithConnc {
        val instance = this.instance
        object : C2SConnection<T>() {
            override val id = ResourceLocation(mode.instance.modId, "c2s_toolgun_command_$it")
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) =
                serverRaycastAndActivate<T>(context.player, buf, mode::class.java, instance.modeTypes.typeToSupplier(mode::class).let{Supplier{it.get().also{it.instance = instance}}}, toExecute)
        }
    }

fun <T: BaseMode> BaseMode.registerConnection(mode: T, name: String, toExecute: (item: T, level: ServerLevel, player: ServerPlayer) -> Unit) =
    name idWithConnc {
        val instance = this.instance
        object : C2SConnection<T>() {
            override val id = ResourceLocation(mode.instance.modId, "c2s_toolgun_command_$it")
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) =
                serverTryActivate<T>(context.player, buf, mode::class.java, instance.modeTypes.typeToSupplier(mode::class).let{Supplier{it.get().also{it.instance = instance}}}, toExecute)
        }
    }

abstract class BaseNetworking <T: BaseMode> {
    var clientObj: T? = null
    var serverObj: T? = null

    enum class EnvType { Client, Server }

    fun init(obj: T, type: EnvType) {
        when (type) {
            EnvType.Client -> clientObj = obj
            EnvType.Server -> serverObj = obj
        }
    }
}