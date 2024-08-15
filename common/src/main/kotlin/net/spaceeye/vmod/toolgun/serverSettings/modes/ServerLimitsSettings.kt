package net.spaceeye.vmod.toolgun.serverSettings.modes

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.IntLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.limits.StrLimit
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.utils.FakeKProperty
import java.awt.Color
import java.util.*

class ServerLimitsSettings: ServerSettingsGUIBuilder {
    override val itemName: TranslatableComponent get() = TranslatableComponent("Server Limits")
    override val typeName: String get() = "Server Limits"

    override fun makeGUISettings(parentWindow: UIContainer) {
        Button(Color(180, 180, 180), "Apply new Server Limits") {
            ServerLimits.tryUpdateToServer()
        } constrain {
            x = CenterConstraint()
            y = SiblingConstraint() + 2f.pixels

            width = 98.percent
        } childOf parentWindow

        for (item in ServerLimits.instance.getSerializableItems()) {
            val separated = item.cachedName.split(Regex("(?=[A-Z])")).toMutableList()
            separated[0] = separated[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val name = separated.reduce { acc, s -> acc + " " + s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}

            when(val limit = item.it) {
                is DoubleLimit -> {
                    makeTextEntry("Min \"$name\"", FakeKProperty({limit.minValue}) {limit.minValue = it}, 2f, 2f, parentWindow)
                    makeTextEntry("Max \"$name\"", FakeKProperty({limit.maxValue}) {limit.maxValue = it}, 2f, 2f, parentWindow)
                }
                is IntLimit -> {
                    makeTextEntry("Min \"$name\"", FakeKProperty({limit.minValue}) {limit.minValue = it}, 2f, 2f, parentWindow)
                    makeTextEntry("Max \"$name\"", FakeKProperty({limit.maxValue}) {limit.maxValue = it}, 2f, 2f, parentWindow)
                }
                is StrLimit -> {
                    makeTextEntry("Max \"$name\"", FakeKProperty({limit.sizeLimit}) {limit.sizeLimit = it}, 2f, 2f, parentWindow)
                }
                else -> throw AssertionError("Unhandled type")
            }

            UIText("", false) constrain {
                y = SiblingConstraint()
            } childOf parentWindow
        }
    }
}