package net.spaceeye.vmod.toolgun.modes.extensions

import dev.architectury.event.EventResult
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.*
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.RaycastFunctions
import org.lwjgl.glfw.GLFW

class BasicConnectionExtension<T: ExtendableToolgunMode>(
    var name: String,
    var allowResetting: Boolean = false,

    var primaryFunction: ((inst: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = null,
    var secondaryFunction: ((inst: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = null,

    var primaryClientCallback: ((inst: T) -> Unit)? = null,
    var secondaryClientCallback: ((inst: T) -> Unit)? = null,
    var resetClientCallback: ((inst: T) -> Unit)? = { mode -> mode.resetState(); mode.refreshHUD() },

    var blockPrimary: (inst: T) -> Boolean = {false},
    var blockSecondary: (inst: T) -> Boolean = {false}
    ): ToolgunModeExtension {
    private var primaryConn: C2SConnection<T>? = null
    private var secondaryConn: C2SConnection<T>? = null
    private var resetConn: C2SConnection<EmptyPacket>? = null

    private lateinit var inst: ExtendableToolgunMode

    override fun onInit(inst: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        this.inst = inst

        primaryConn = primaryFunction?.let { inst.registerConnection(inst, name + "_primary_connection") { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        secondaryConn = secondaryFunction?.let { inst.registerConnection(inst, name + "_secondary_connection") { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        resetConn = if (allowResetting) ServerToolGunState.c2sToolgunWasReset else null
    }

    override fun eOnKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        if (action != GLFW.GLFW_PRESS) {return false}

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode) && resetConn != null) {
            resetClientCallback?.invoke(inst as T)
            resetConn!!.sendToServer(EmptyPacket())
            return true
        }

        return false
    }

    override fun eOnMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (action != GLFW.GLFW_PRESS) {return EventResult.pass()}

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && primaryConn != null) {
            if (!blockPrimary(inst as T)) {
                primaryClientCallback?.invoke(inst as T)
                primaryConn!!.sendToServer(inst as T)
            }
            return EventResult.interruptFalse()
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && secondaryConn != null) {
            if (!blockSecondary(inst as T)) {
                secondaryClientCallback?.invoke(inst as T)
                secondaryConn!!.sendToServer(inst as T)
            }
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }

}