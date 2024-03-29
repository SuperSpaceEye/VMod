package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.AxisMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementAssistCRIHandler
import net.spaceeye.vmod.toolgun.modes.util.ThreeClicksActivationSteps
import org.lwjgl.glfw.GLFW

interface AxisCRIHandler: ClientRawInputsHandler, PlacementAssistCRIHandler {
    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as AxisMode
        if (paStage == ThreeClicksActivationSteps.FIRST_RAYCAST && !secondaryFirstRaycast) { return EventResult.pass() }

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as AxisMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            clientHandleMouseClickPA()
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            clientHandleSecondary()
            conn_secondary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun handleMouseScrollEvent(amount: Double): EventResult {
        return clientHandleMouseEventPA(amount)
    }

    private fun clientHandleSecondary() {
        this as AxisMode
        secondaryFirstRaycast = !secondaryFirstRaycast
    }
}