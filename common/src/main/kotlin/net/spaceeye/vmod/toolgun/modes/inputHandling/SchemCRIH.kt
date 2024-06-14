package net.spaceeye.vmod.toolgun.modes.inputHandling

import dev.architectury.event.EventResult
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ClientRawInputsHandler
import net.spaceeye.vmod.toolgun.modes.state.ClientPlayerSchematics
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import org.lwjgl.glfw.GLFW
import kotlin.math.sign

interface SchemCRIH: ClientRawInputsHandler {
    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        this as SchemMode
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            shipInfo = null
            refreshHUD()
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            conn_secondary.sendToServer(this)
        }

        ClientPlayerSchematics.listSchematics()

        return EventResult.interruptFalse()
    }

    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        this as SchemMode
        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }

    override fun handleMouseScrollEvent(amount: Double): EventResult {
        this as SchemMode
        if (shipInfo == null) { return EventResult.pass() }

        rotationAngle.it += scrollAngle * amount.sign

        return EventResult.interruptFalse()
    }
}