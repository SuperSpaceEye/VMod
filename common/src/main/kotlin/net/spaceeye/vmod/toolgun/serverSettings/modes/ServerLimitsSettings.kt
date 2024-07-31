package net.spaceeye.vmod.toolgun.serverSettings.modes

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.toConstraint
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import java.awt.Color

class ServerLimitsSettings: ServerSettingsGUIBuilder {
    override val itemName: TranslatableComponent get() = TranslatableComponent("Server Limits")
    override val typeName: String get() = "Server Limits"

    override fun makeGUISettings(parentWindow: UIContainer) {
        UIText("TEST TEXT", false) constrain {
            x = CenterConstraint()
            y = CenterConstraint()

            color = Color.BLACK.toConstraint()
        } childOf parentWindow
    }
}