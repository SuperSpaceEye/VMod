package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.SliderMode
import net.spaceeye.vmod.utils.EmptyPacket
import org.lwjgl.glfw.GLFW

interface SliderCEH: ClientEventsHandler {
    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        this as SliderMode
        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            refreshHUD()
            ServerToolGunState.c2sToolgunWasReset.sendToServer(EmptyPacket())
            return true
        }

        if (primaryTimes != 0 && ClientToolGunState.GUI_MENU_OPEN_OR_CLOSE.matches(key, scancode)) {
            return true
        }

        return false
    }

    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as SliderMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            primaryTimes++
            refreshHUD()
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }
}