package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.HydraulicsMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementAssistCEH
import net.spaceeye.vmod.toolgun.modes.util.ThreeClicksActivationSteps
import org.lwjgl.glfw.GLFW

interface HydraulicsCEH: ClientEventsHandler, PlacementAssistCEH {
    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as HydraulicsMode
        if (paStage == ThreeClicksActivationSteps.FIRST_RAYCAST && !primaryFirstRaycast) { return EventResult.pass() }

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            refreshHUD()
        }

        return EventResult.interruptFalse()
    }

    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as HydraulicsMode
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

    private fun clientHandlePrimary() {
        this as HydraulicsMode
        primaryFirstRaycast = !primaryFirstRaycast
    }

    override fun onOpenMode() {
        paOnOpen()
    }

    override fun onCloseMode() {
        paOnClose()
    }
}