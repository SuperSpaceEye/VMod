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

    open fun getPrimaryClientCallback(): (() -> Unit)? = null
    open fun getSecondaryClientCallback(): (() -> Unit)? = null
    open fun getResetClientCallback(): (() -> Unit)? = null

    private val primaryClientCallback: (() -> Unit)? = getPrimaryClientCallback()
    private val secondaryClientCallback: (() -> Unit)? = getSecondaryClientCallback()
    private val resetClientCallback: (() -> Unit)? = getResetClientCallback()

    private lateinit var mode: ExtendableToolgunMode

    override fun onInit(mode: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        this.mode = mode

        primaryConn = getPrimaryFunction()?.let { mode.registerConnection(mode, name + "_primary_connection") { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        secondaryConn = getSecondaryFunction()?.let { mode.registerConnection(mode, name + "_secondary_connection") { item, level, player, rr -> it(item as T, level, player, rr) } } as C2SConnection<T>?
        resetConn = getResetFunction()?.let { mode.registerConnection(mode, name + "_reset_connection") { item, level, player -> it(item as T, level, player) } } as C2SConnection<T>?
    }

    override fun eOnKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        if (action != GLFW.GLFW_PRESS) {return false}

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode) && resetConn != null) {
            resetClientCallback?.invoke()
            resetConn!!.sendToServer(mode as T)
            return true
        }

        return false
    }

    override fun eOnMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (action != GLFW.GLFW_PRESS) {return EventResult.pass()}

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && primaryConn != null) {
            primaryClientCallback?.invoke()
            primaryConn!!.sendToServer(mode as T)
            return EventResult.interruptFalse()
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && secondaryConn != null) {
            secondaryClientCallback?.invoke()
            secondaryConn!!.sendToServer(mode as T)
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }

}