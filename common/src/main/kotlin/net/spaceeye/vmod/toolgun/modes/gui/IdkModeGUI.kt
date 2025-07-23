package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.IdkMode
import net.spaceeye.vmod.toolgun.modes.state.StripMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.*
import net.spaceeye.vmod.translate.RADIUS
import net.spaceeye.vmod.translate.STRIP_ALL
import net.spaceeye.vmod.translate.STRIP_IN_RADIUS
import net.spaceeye.vmod.translate.STRIP_MODES
import net.spaceeye.vmod.translate.get

interface IdkModeGUI: GUIBuilder, EGUIBuilder, SimpleHUD {
    override val itemName get() = makeFake("IDK")
    override fun makeSubText(makeText: (String) -> Unit) {
        makeText("IDK Mode")
    }

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as IdkMode
        val offset = 2.0f
        val limits = ServerLimits.instance

//        makeTextEntry(RADIUS.get(), ::radius, offset, offset, parentWindow)
        makeTextEntry(MAX_FORCE.get(), ::maxForce, offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(STIFFNESS.get(), ::stiffness, offset, offset, parentWindow, limits.stiffness)
        makeTextEntry(DAMPING.get(), ::damping, offset, offset, parentWindow, limits.damping)
    }
}