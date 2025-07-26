package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.ToolgunInstance
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsTypes
import net.spaceeye.vmod.translate.YOU_DONT_HAVE_ACCESS_TO_THIS
import net.spaceeye.vmod.translate.get
import java.awt.Color

class ServerSettingsGUI(val mainWindow: UIBlock, val instance: ToolgunInstance): BaseToolgunGUIWindow(mainWindow) {
    init {
        onGUIOpen()
        makeScrollComponents(ServerSettingsTypes.asList().map { it.get() }) { component ->
            settingsScrollComponent.clearChildren()
            component.makeGUISettings(settingsScrollComponent)
        }
    }

    override fun onGUIOpen() {
        mainWindow.clearChildren()

        instance.server.checkIfIHaveAccess(ServerToolGunState.AccessTo.ServerSettings) {
            if (!it) {
                UIText(YOU_DONT_HAVE_ACCESS_TO_THIS.get(), false) constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()

                    color = Color.BLACK.toConstraint()
                } childOf mainWindow
                return@checkIfIHaveAccess
            }

            scrollComponent childOf mainWindow
            settingsComponent childOf mainWindow
        }
    }
}