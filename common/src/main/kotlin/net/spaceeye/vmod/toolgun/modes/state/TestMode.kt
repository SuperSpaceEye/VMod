package net.spaceeye.vmod.toolgun.modes.state

import gg.essential.elementa.components.UIContainer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.translate.makeFake
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import org.valkyrienskies.core.api.ships.ServerShip

class TestMode: ExtendableToolgunMode() {
    override val itemName = makeFake("Test Mode")

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        super.eMakeGUISettings(parentWindow)
    }

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        val ship = raycastResult.ship as? ServerShip ?: return

    }
    companion object {
        init {
            ToolgunModes.registerWrapper(TestMode::class) {
                it.addExtension<TestMode> {
                    BasicConnectionExtension<TestMode>("test_mode"
                        ,leftFunction = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}