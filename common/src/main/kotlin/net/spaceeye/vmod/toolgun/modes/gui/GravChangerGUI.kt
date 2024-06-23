package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.GravChangerMode
import net.spaceeye.vmod.translate.*

interface GravChangerGUI: GUIBuilder {
    override val itemName get() = GRAVITY_CHANGER

    private var gravX get() = (this as GravChangerMode).gravityVector.x
        set(value) {(this as GravChangerMode).gravityVector.x = value}

    private var gravY get() = (this as GravChangerMode).gravityVector.y
        set(value) {(this as GravChangerMode).gravityVector.y = value}

    private var gravZ get() = (this as GravChangerMode).gravityVector.z
        set(value) {(this as GravChangerMode).gravityVector.z = value}

    override fun makeGUISettings(parentWindow: UIContainer) {
        this as GravChangerMode

        makeTextEntry(X_GRAVITY.get(), ::gravX, 2f, 2f, parentWindow)
        makeTextEntry(Y_GRAVITY.get(), ::gravY, 2f, 2f, parentWindow)
        makeTextEntry(Z_GRAVITY.get(), ::gravZ, 2f, 2f, parentWindow)

        makeDropDown(SAMPLING_MODES.get(), parentWindow, 2f, 2f, listOf(
            DItem(INDIVIDUAL.get(), mode == GravChangerMode.Mode.Individual) {mode = GravChangerMode.Mode.Individual},
            DItem(ALL_CONNECTED.get(), mode == GravChangerMode.Mode.AllConnected) {mode = GravChangerMode.Mode.AllConnected},
            DItem(ALL_CONNECTED_AND_TOUCHING.get(), mode == GravChangerMode.Mode.AllConnectedAndTouching) {mode = GravChangerMode.Mode.AllConnectedAndTouching},
        ))
    }
}