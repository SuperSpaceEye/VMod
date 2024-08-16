package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.ConnectionMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementAssistCEH
import net.spaceeye.vmod.toolgun.modes.util.ThreeClicksActivationSteps
import org.lwjgl.glfw.GLFW

interface ConnectionCEH: ClientEventsHandler, PlacementAssistCEH {
    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        this as ConnectionMode
        if (action != GLFW.GLFW_PRESS) {return false}
        if (paStage == ThreeClicksActivationSteps.FIRST_RAYCAST && !primaryFirstRaycast) { return false }

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            refreshHUD()
            return true
        }

        return false
    }

    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as ConnectionMode
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

    override fun onMouseScrollEvent(amount: Double): EventResult {
        return clientHandleMouseEventPA(amount)
    }

    override fun onOpenMode() {
        paOnOpen()
    }

    override fun onCloseMode() {
        paOnClose()
    }

    private fun clientHandlePrimary() {
        this as ConnectionMode
        primaryFirstRaycast = !primaryFirstRaycast
    }
}