package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.SyncRotation
import org.lwjgl.glfw.GLFW

interface SyncRotationCEH: ClientEventsHandler {
    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as SyncRotation
        if (action != GLFW.GLFW_PRESS) {return EventResult.pass()}

        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            refreshHUD()
            return EventResult.interruptFalse()
        }

        if (primaryFirstRaycast) {return EventResult.interruptFalse()}

        return EventResult.pass()
    }

    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as SyncRotation
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            primaryFirstRaycast = !primaryFirstRaycast
            refreshHUD()
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }
}