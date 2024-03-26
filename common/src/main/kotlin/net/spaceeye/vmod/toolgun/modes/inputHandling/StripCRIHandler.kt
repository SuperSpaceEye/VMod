package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.StripMode
import org.lwjgl.glfw.GLFW

interface StripCRIHandler: ClientRawInputsHandler {
    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as StripMode
        return EventResult.pass()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as StripMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }
}