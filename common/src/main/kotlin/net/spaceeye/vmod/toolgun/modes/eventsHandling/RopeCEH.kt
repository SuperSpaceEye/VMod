package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.RopeMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesCEH
import org.lwjgl.glfw.GLFW

interface RopeCEH: ClientEventsHandler, PlacementModesCEH {
    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        this as RopeMode
        if (!primaryFirstRaycast) { return false }

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            refreshHUD()
            return true
        }

        return false
    }

    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as RopeMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            clientHandlePrimary()
            refreshHUD()
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    private fun clientHandlePrimary() {
        this as RopeMode
        primaryFirstRaycast = !primaryFirstRaycast
    }

    override fun onOpenMode() {
        pmOnOpen()
    }

    override fun onCloseMode() {
        pmOnClose()
    }
}