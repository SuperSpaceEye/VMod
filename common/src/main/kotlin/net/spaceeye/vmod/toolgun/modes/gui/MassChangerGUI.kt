package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.UIContainer
import net.spaceeye.vmod.guiElements.makeCheckBox
import net.spaceeye.vmod.guiElements.makeText
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.MassChangerMode
import net.spaceeye.vmod.translate.*
import java.awt.Color

interface MassChangerGUI: GUIBuilder, EGUIBuilder {
    override val itemName get() = MASS_CHANGER

    var updateGuiFn: (Boolean) -> Unit

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as MassChangerMode

        makeCheckBox(SYNC_MASS_PER_BLOCK.get(), ::syncMassPerBlock, 2f, 2f, parentWindow)
        makeCheckBox(PERSISTENT.get(), ::persistent, 2f, 2f, parentWindow)

        val massPerBlock = makeTextEntry(MASS_PER_BLOCK.get(), ::massPerBlock, 2f, 2f, parentWindow, ServerLimits.instance.massPerBlock)

        val theWarning = makeText(MASS_CHANGER_IS_DANGEROUS.get(), Color.red, 2f, 2f, parentWindow)
        val totalMass = makeTextEntry(NEW_TOTAL_MASS.get(), ::newMass, 2f, 2f, parentWindow, ServerLimits.instance.massLimit)

        updateGuiFn = {
            if (it) {
                massPerBlock.unhide()

                theWarning.hide()
                totalMass.hide()
            } else {
                massPerBlock.hide()

                theWarning.unhide()
                totalMass.unhide()
            }
        }

        updateGuiFn(syncMassPerBlock)
    }
}