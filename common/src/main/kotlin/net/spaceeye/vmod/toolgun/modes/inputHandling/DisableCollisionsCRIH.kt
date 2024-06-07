package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.DisableCollisionsMode
import org.lwjgl.glfw.GLFW

interface DisableCollisionsCRIH: ClientRawInputsHandler {
    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as DisableCollisionsMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            primaryFirstRaycast = !primaryFirstRaycast
            refreshHUD()
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            conn_secondary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as DisableCollisionsMode
        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            refreshHUD()
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }
}