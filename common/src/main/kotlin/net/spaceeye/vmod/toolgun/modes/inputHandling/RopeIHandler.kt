package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.RopeMode
import org.lwjgl.glfw.GLFW

interface RopeIHandler: ClientRawInputsHandler {
    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as RopeMode
        if (!primaryFirstRaycast) { return EventResult.pass() }

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as RopeMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            clientHandlePrimary()
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    private fun clientHandlePrimary() {
        this as RopeMode
        primaryFirstRaycast = !primaryFirstRaycast
    }
}