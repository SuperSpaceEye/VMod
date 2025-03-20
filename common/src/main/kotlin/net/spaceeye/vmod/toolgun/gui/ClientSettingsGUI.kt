package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.dsl.childOf
import net.spaceeye.vmod.toolgun.clientSettings.ClientSettingsTypes

class ClientSettingsGUI(val mainWindow: UIBlock): BaseToolgunGUIWindow(mainWindow) {
    init {
        onGUIOpen()
        makeScrollComponents(ClientSettingsTypes.asList().map { it.get() }) { component ->
            settingsScrollComponent.clearChildren()
            component.makeGUISettings(settingsScrollComponent)
        }
    }

    override fun onGUIOpen() {
        mainWindow.clearChildren()

        scrollComponent childOf mainWindow
        settingsComponent childOf mainWindow
    }
}