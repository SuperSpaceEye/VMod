package net.spaceeye.vmod.toolgun.modes.extensions

import dev.architectury.event.EventResult
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.*
import net.spaceeye.vmod.utils.RaycastFunctions
import org.lwjgl.glfw.GLFW

//TODO rename
abstract class BasicConnectionExtension<T: ExtendableToolgunMode>(): ToolgunModeExtension {
    override val typeName: String get() = "BasicConnectionExtension"

    var name = ""

    constructor(name: String): this() {this.name = name}

    private var primaryConn: C2SConnection<T>? = null
    private var secondaryConn: C2SConnection<T>? = null
    private var resetConn: C2SConnection<T>? = null

    open fun getPrimaryFunction(): ((mode: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = null
    open fun getSecondaryFunction(): ((mode: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = null
    open fun getResetFunction(): ((mode: T, level: ServerLevel, player: ServerPlayer) -> Unit)? = null

    open fun getPrimaryClientCallback(): ((mode: T) -> Unit)? = null
    open fun getSecondaryClientCallback(): ((mode: T) -> Unit)? = null
    open fun getResetClientCallback(): ((mode: T) -> Unit)? = null

    @JvmField var primaryFunction: ((mode: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = getPrimaryFunction()
    @JvmField var secondaryFunction: ((mode: T, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? = getSecondaryFunction()
    @JvmField var resetFunction: ((mode: T, level: ServerLevel, player: ServerPlayer) -> Unit)? = getResetFunction()

    @JvmField var primaryClientCallback: ((mode: T) -> Unit)? = getPrimaryClientCallback()
    @JvmField var secondaryClientCallback: ((mode: T) -> Unit)? = getSecondaryClientCallback()
    @JvmField var resetClientCallback: ((mode: T) -> Unit)? = getResetClientCallback()

    private lateinit var mode: ExtendableToolgunMode

    final override fun onInit(mode: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        this.mode = mode

        primaryConn = primaryFunction?.let { mode.registerConnection(mode, name + "_primary_connection") { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        secondaryConn = secondaryFunction?.let { mode.registerConnection(mode, name + "_secondary_connection") { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        resetConn = resetFunction?.let { mode.registerConnection(mode, name + "_reset_connection") { item, level, player -> it(item as T, level, player) } } as C2SConnection<T>?
    }

    final override fun eOnKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        if (action != GLFW.GLFW_PRESS) {return false}

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode) && resetConn != null) {
            resetClientCallback?.invoke(mode as T)
            resetConn!!.sendToServer(mode as T)
            return true
        }

        return false
    }

    final override fun eOnMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (action != GLFW.GLFW_PRESS) {return EventResult.pass()}

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && primaryConn != null) {
            primaryClientCallback?.invoke(mode as T)
            primaryConn!!.sendToServer(mode as T)
            return EventResult.interruptFalse()
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && secondaryConn != null) {
            secondaryClientCallback?.invoke(mode as T)
            secondaryConn!!.sendToServer(mode as T)
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }

}