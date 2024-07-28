package net.spaceeye.vmod.toolgun.modes.eventsHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientEventsHandler
import net.spaceeye.vmod.toolgun.modes.state.SliderMode
import org.lwjgl.glfw.GLFW

interface SliderCEH: ClientEventsHandler {
    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as SliderMode
        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            refreshHUD()
            ServerToolGunState.c2sToolgunWasReset.sendToServer(ServerToolGunState.C2SToolgunWasReset())
            return EventResult.interruptFalse()
        }

        if (primaryTimes != 0 && ClientToolGunState.GUI_MENU_OPEN_OR_CLOSE.matches(key, scancode)) {
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
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