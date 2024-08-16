package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.StripMode
import org.lwjgl.glfw.GLFW

interface StripCEH: ClientEventsHandler {
    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        this as StripMode
        return false
    }

    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as StripMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }
}