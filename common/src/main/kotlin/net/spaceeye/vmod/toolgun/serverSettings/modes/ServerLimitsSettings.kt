package net.spaceeye.vmod.toolgun.serverSettings.modes

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.*
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.translate.APPLY_NEW_SERVER_LIMITS
import net.spaceeye.vmod.translate.SERVER_LIMITS
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.FakeKProperty
import net.spaceeye.vmod.utils.separateTypeName
import java.awt.Color
import java.util.*

class ServerLimitsSettings: ServerSettingsGUIBuilder {
    override val itemName = SERVER_LIMITS

    override fun makeGUISettings(parentWindow: UIContainer) {
        Button(Color(180, 180, 180), APPLY_NEW_SERVER_LIMITS.get()) {
            ServerLimits.tryUpdateToServer()
        } constrain {
            x = CenterConstraint()
            y = SiblingConstraint() + 2f.pixels

            width = 98.percent
        } childOf parentWindow

        for (item in ServerLimits.instance.getAllReflectableItems()) {
            val name = separateTypeName(item.cachedName)
            when(val limit = item.it) {
                is DoubleLimit -> {
                    makeTextEntry("Min \"$name\"", FakeKProperty({limit.minValue}) {limit.minValue = it}, 2f, 2f, parentWindow)
                    makeTextEntry("Max \"$name\"", FakeKProperty({limit.maxValue}) {limit.maxValue = it}, 2f, 2f, parentWindow)
                }
                is FloatLimit -> {
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
                is BoolLimit -> {
                    makeDropDown("Force", parentWindow, 2f, 2f, listOf(
                        DItem("True",    limit.mode == BoolLimit.Force.TRUE   ) {limit.mode = BoolLimit.Force.TRUE},
                        DItem("False",   limit.mode == BoolLimit.Force.FALSE  ) {limit.mode = BoolLimit.Force.FALSE},
                        DItem("Nothing", limit.mode == BoolLimit.Force.NOTHING) {limit.mode = BoolLimit.Force.NOTHING},
                    ))
                }
                else -> throw AssertionError("Unhandled type")
            }

            UIText("", false) constrain {
                y = SiblingConstraint()
            } childOf parentWindow
        }
    }
}