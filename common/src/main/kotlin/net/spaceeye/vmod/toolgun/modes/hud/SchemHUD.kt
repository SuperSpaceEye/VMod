package net.spaceeye.vmod.toolgun.modes.hud

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.SCHEM_HUD_1
import net.spaceeye.vmod.translate.SCHEM_HUD_2
import net.spaceeye.vmod.translate.get

interface SchemHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {}
    override fun makeSubText(makeText: (String) -> Unit, textHolder: UIContainer) {
        this as SchemMode
        when {
            shipInfo == null -> makeText(SCHEM_HUD_1.get())
            shipInfo != null -> {
                makeText(SCHEM_HUD_2.get())
                // i think multiline text incorrectly calculates width, so i need to do this shit for it to look right
                textHolder constrain {
                    x = 0.percent
                    y = 0.percent

                    width = ChildBasedMaxSizeConstraint() - 280.pixels
                    height = ChildBasedSizeConstraint()
                }
            }
        }
    }
}