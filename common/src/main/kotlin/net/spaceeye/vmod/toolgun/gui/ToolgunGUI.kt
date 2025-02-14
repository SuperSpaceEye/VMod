package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.components.UIBlock
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode

class ToolgunGUI(mainWindow: UIBlock): BaseToolgunGUIWindow(mainWindow) {
    init {
        makeScrollComponents(ClientToolGunState.modes) { component ->
            settingsScrollComponent.clearChildren()
            component.makeGUISettings(settingsScrollComponent)
            ClientToolGunState.refreshHUD()
            ClientToolGunState.currentMode = component as BaseMode
        }
    }

    override fun onGUIOpen() {
        ServerLimits.updateFromServer()
    }
}