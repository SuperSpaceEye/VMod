package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.ClientPlayerSchematics
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import org.lwjgl.glfw.GLFW
import kotlin.math.sign

interface SchemCEH: ClientEventsHandler {
    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as SchemMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            shipInfo = null
            refreshHUD()
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            conn_secondary.sendToServer(this)
        }

        ClientPlayerSchematics.listSchematics()

        return EventResult.interruptFalse()
    }

    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        this as SchemMode
        if (action != GLFW.GLFW_PRESS) { return false }
        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            return true
        }

        return false
    }

    override fun onMouseScrollEvent(amount: Double): EventResult {
        this as SchemMode
        if (shipInfo == null) { return EventResult.pass() }

        rotationAngle.it += scrollAngle * amount.sign

        return EventResult.interruptFalse()
    }
}