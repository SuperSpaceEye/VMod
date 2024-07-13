package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.ScaleMode
import org.lwjgl.glfw.GLFW

interface ScaleCEH: ClientEventsHandler {
    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as ScaleMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }
}