package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.components.UIBlock
import net.spaceeye.vmod.gui.additions.HUDAddition
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode

class ToolgunGUI(mainWindow: UIBlock, client: ClientToolGunState): BaseToolgunGUIWindow(mainWindow) {
    init {
        makeScrollComponents(client.modes) { component ->
            settingsScrollComponent.clearChildren()
            component.makeGUISettings(settingsScrollComponent)
            HUDAddition.refreshHUD()
            client.currentMode = component as BaseMode
        }
    }

    override fun onGUIOpen() {
        ServerLimits.updateFromServer()
    }
}