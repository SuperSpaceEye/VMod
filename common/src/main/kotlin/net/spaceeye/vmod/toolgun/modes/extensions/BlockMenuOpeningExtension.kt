package net.spaceeye.vmod.toolgun.modes.extensions

import net.minecraft.network.chat.Component
import net.spaceeye.vmod.gui.additions.ErrorAddition
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModeExtension
import net.spaceeye.vmod.translate.PRESS_R_TO_RESET_STATE
import net.spaceeye.vmod.translate.get
import org.lwjgl.glfw.GLFW

class BlockMenuOpeningExtension<T: ExtendableToolgunMode>(val failMsg: Component? = PRESS_R_TO_RESET_STATE, var predicate: (inst: T) -> Boolean): ToolgunModeExtension {
    lateinit var inst: ExtendableToolgunMode

    override fun onInit(inst: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        this.inst = inst
    }

    override fun eOnKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        if (action != GLFW.GLFW_PRESS) {return false}

        val keyPressed = inst.instance.client.GUI_MENU_OPEN_OR_CLOSE.matches(key, scancode)
        if (!keyPressed) {return false}

        val block = predicate(inst as T)
        if (block && failMsg != null) { ErrorAddition.addHUDError(failMsg.get()) }
        return block
    }
}