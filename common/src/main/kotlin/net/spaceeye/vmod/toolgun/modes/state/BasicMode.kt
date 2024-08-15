package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.registerConnection
import net.spaceeye.vmod.utils.RaycastFunctions
import org.lwjgl.glfw.GLFW

abstract class BasicMode<T: Serializable>(
    name: String,
): BaseMode, ClientEventsHandler {
    private var primaryConn: C2SConnection<T>? = null
    private var secondaryConn: C2SConnection<T>? = null
    private var resetConn: C2SConnection<T>? = null

    protected open fun getPrimaryFunction(): ((mode: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = null
    protected open fun getSecondaryFunction(): ((mode: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = null
    protected open fun getResetFunction(): ((mode: T, level: ServerLevel, player: ServerPlayer) -> Unit)? = null

    protected open fun getPrimaryClientCallback(): (() -> Unit)? = null
    protected open fun getSecondaryClientCallback(): (() -> Unit)? = null
    protected open fun getResetClientCallback(): (() -> Unit)? = null

    private val primaryClientCallback: (() -> Unit)? = getPrimaryClientCallback()
    private val secondaryClientCallback: (() -> Unit)? = getSecondaryClientCallback()
    private val resetClientCallback: (() -> Unit)? = getResetClientCallback()

    init {
        primaryConn = getPrimaryFunction()?.let { registerConnection<BasicMode<*>>(this, name + "_primary_connection") { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        secondaryConn = getSecondaryFunction()?.let { registerConnection<BasicMode<*>>(this, name + "_secondary_connection") { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        resetConn = getResetFunction()?.let { registerConnection<BasicMode<*>>(this, name + "_reset_connection") { item, level, player -> it(item as T, level, player) } } as C2SConnection<T>?
    }

    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        if (action != GLFW.GLFW_PRESS) {return EventResult.pass()}

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode) && resetConn != null) {
            resetClientCallback?.invoke()
            resetConn!!.sendToServer(this as T)
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }

    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (action != GLFW.GLFW_PRESS) {return EventResult.pass()}

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && primaryConn != null) {
            primaryClientCallback?.invoke()
            primaryConn!!.sendToServer(this as T)
            return EventResult.interruptFalse()
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && secondaryConn != null) {
            secondaryClientCallback?.invoke()
            secondaryConn!!.sendToServer(this as T)
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }
}