package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.CopyMode
import org.lwjgl.glfw.GLFW

interface CopyCRIHandler: ClientRawInputsHandler {
    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as CopyMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            conn_secondary.sendToServer(this)
        }


        return EventResult.interruptFalse()
    }
}