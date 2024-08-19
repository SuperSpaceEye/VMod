package net.spaceeye.vmod.toolgun.modes.extensions

import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModeExtension

class BlockMenuOpeningExtension<T: ExtendableToolgunMode>(val predicate: (inst: T) -> Boolean): ToolgunModeExtension {
    lateinit var inst: ExtendableToolgunMode

    override fun onInit(inst: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        this.inst = inst
    }

    override fun eOnKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        return predicate(inst as T) && ClientToolGunState.GUI_MENU_OPEN_OR_CLOSE.matches(key, scancode)
    }
}