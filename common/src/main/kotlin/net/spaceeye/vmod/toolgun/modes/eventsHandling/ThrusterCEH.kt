package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.ThrusterMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesCEH
import org.lwjgl.glfw.GLFW

interface ThrusterCEH: ClientEventsHandler, PlacementModesCEH {
    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as ThrusterMode
        return EventResult.pass()
    }

    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as ThrusterMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun onOpenMode() {
        pmOnOpen()
    }

    override fun onCloseMode() {
        pmOnClose()
    }
}