package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.HydraulicsMode
import org.lwjgl.glfw.GLFW

interface HydraulicsCRIHandler: ClientRawInputsHandler {
    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as HydraulicsMode
        if (!primaryFirstRaycast) { return EventResult.pass() }

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as HydraulicsMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            clientHandlPrimary()
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    private fun clientHandlPrimary() {
        this as HydraulicsMode
        primaryFirstRaycast = !primaryFirstRaycast
    }
}