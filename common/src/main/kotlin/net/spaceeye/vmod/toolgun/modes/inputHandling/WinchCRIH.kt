package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.WinchMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementAssistCRIHandler
import net.spaceeye.vmod.toolgun.modes.util.ThreeClicksActivationSteps
import org.lwjgl.glfw.GLFW

interface WinchCRIH: ClientRawInputsHandler, PlacementAssistCRIHandler {
    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as WinchMode
        if (paStage == ThreeClicksActivationSteps.FIRST_RAYCAST && !primaryFirstRaycast) { return EventResult.pass() }

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            refreshHUD()
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as WinchMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            clientHandlePrimary()
            refreshHUD()
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            clientHandleMouseClickPA()
            refreshHUD()
            conn_secondary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseScrollEvent(amount: Double): EventResult {
        return clientHandleMouseEventPA(amount)
    }

    private fun clientHandlePrimary() {
        this as WinchMode
        primaryFirstRaycast = !primaryFirstRaycast
    }
}