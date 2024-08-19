package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.GravChangerMode
import net.spaceeye.vmod.translate.*
import net.spaceeye.vmod.utils.FakeKProperty

interface GravChangerGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = GRAVITY_CHANGER

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as GravChangerMode

        makeTextEntry(X_GRAVITY.get(), FakeKProperty({gravityVector.x}, {gravityVector.x = it}), 2f, 2f, parentWindow)
        makeTextEntry(Y_GRAVITY.get(), FakeKProperty({gravityVector.y}, {gravityVector.y = it}), 2f, 2f, parentWindow)
        makeTextEntry(Z_GRAVITY.get(), FakeKProperty({gravityVector.z}, {gravityVector.z = it}), 2f, 2f, parentWindow)

        makeDropDown(SAMPLING_MODES.get(), parentWindow, 2f, 2f, listOf(
            DItem(INDIVIDUAL.get(), mode == GravChangerMode.Mode.Individual) {mode = GravChangerMode.Mode.Individual},
            DItem(ALL_CONNECTED.get(), mode == GravChangerMode.Mode.AllConnected) {mode = GravChangerMode.Mode.AllConnected},
            DItem(ALL_CONNECTED_AND_TOUCHING.get(), mode == GravChangerMode.Mode.AllConnectedAndTouching) {mode = GravChangerMode.Mode.AllConnectedAndTouching},
        ))
    }
}