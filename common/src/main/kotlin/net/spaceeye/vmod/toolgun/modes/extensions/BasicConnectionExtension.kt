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

//TODO rename it to smth else bc it's not basic at all
class BasicConnectionExtension<T: ExtendableToolgunMode>(
    var name: String,
    var allowResetting: Boolean = false,

    var leftFunction:   ((inst: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = null,
    var rightFunction:  ((inst: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = null,
    var middleFunction: ((inst: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = null,

    var leftClientCallback:   ((inst: T) -> Unit)? = null,
    var rightClientCallback:  ((inst: T) -> Unit)? = null,
    var middleClientCallback: ((inst: T) -> Unit)? = null,
    var resetClientCallback: ((inst: T) -> Unit)? = { mode -> mode.resetState(); mode.refreshHUD() },

    var blockLeft:   (inst: T) -> Boolean = {false},
    var blockRight:  (inst: T) -> Boolean = {false},
    var blockMiddle: (inst: T) -> Boolean = {false}
    ): ToolgunModeExtension {
    private var leftConn:   C2SConnection<T>? = null
    private var rightConn:  C2SConnection<T>? = null
    private var middleConn: C2SConnection<T>? = null
    private var resetConn: C2SConnection<EmptyPacket>? = null

    private lateinit var inst: ExtendableToolgunMode

    override fun onInit(inst: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        this.inst = inst

        leftConn   = leftFunction  ?.let { inst.registerConnection(inst, name + "_left_connection")   { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        rightConn  = rightFunction ?.let { inst.registerConnection(inst, name + "_right_connection")  { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        middleConn = middleFunction?.let { inst.registerConnection(inst, name + "_middle_connection") { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        resetConn = if (allowResetting) inst.instance.server.c2sToolgunWasReset else null
    }

    override fun eOnKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        if (action != GLFW.GLFW_PRESS) {return false}

        if (inst.instance.client.TOOLGUN_RESET_KEY.matches(key, scancode) && resetConn != null) {
            resetClientCallback?.invoke(inst as T)
            resetConn!!.sendToServer(EmptyPacket())
            return true
        }

        return false
    }

    private fun trySendButtonPress(inst: T, block: (T) -> Boolean, callback: ((T) -> Unit)?, conn: C2SConnection<T>?): EventResult {
        return if ((conn != null || callback != null) && !block(inst)) {
            callback?.invoke(inst)
            conn?.sendToServer(inst)

            EventResult.interruptFalse()
        } else {
            EventResult.pass()
        }
    }

    override fun eOnMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (action != GLFW.GLFW_PRESS) {return EventResult.pass()}
        val inst = inst as T

        return when (button) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT   -> trySendButtonPress(inst, blockLeft,   leftClientCallback,   leftConn)
            GLFW.GLFW_MOUSE_BUTTON_RIGHT  -> trySendButtonPress(inst, blockRight,  rightClientCallback,  rightConn)
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> trySendButtonPress(inst, blockMiddle, middleClientCallback, middleConn)
            else -> EventResult.pass()
        }
    }
}