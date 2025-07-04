package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.gui.COMChangerGUI
import net.spaceeye.vmod.toolgun.modes.hud.COMChangerHUD
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId

//TODO if block where all mass is concentrated is broken, ship is going to disappear
class COMChangerMode: ExtendableToolgunMode(), COMChangerGUI, COMChangerHUD {
    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        val ship = raycastResult.ship as? ServerShip ?: return

        val aabb = ship.shipAABB ?: return
        val pos = raycastResult.blockPosition

        var totalMass = 0.0
        for (x in aabb.minX()-1..aabb.maxX()+1) {
        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
        for (y in aabb.minY()-1..aabb.maxY()+1) {
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) {continue}
            val (mass, type) = BlockStateInfo.get(state) ?: continue
            totalMass += mass

            CustomBlockMassManager.setCustomMass(level, x, y, z, 0.0, type, mass, ship)
        } } }

        CustomBlockMassManager.setCustomMass(level, pos.x, pos.y, pos.z, totalMass)
    }

    fun activateSecondaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) {
        val ship = raycastResult.ship as? ServerShip ?: return

        val aabb = ship.shipAABB ?: return

        for (x in aabb.minX()-1..aabb.maxX()+1) {
        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
        for (y in aabb.minY()-1..aabb.maxY()+1) {
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) {continue}
            val (mass, type) = BlockStateInfo.get(state) ?: continue

            CustomBlockMassManager.setCustomMass(level, x, y, z, mass, type, mass, ship)
            CustomBlockMassManager.removeCustomMass(level.dimensionId, x, y, z)
        } } }
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(COMChangerMode::class) {
                it.addExtension {
                    BasicConnectionExtension<COMChangerMode>("com_changer"
                        ,leftFunction  = {inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr)}
                        ,rightFunction = {inst, level, player, rr -> inst.activateSecondaryFunction(level, player, rr)}
                    )
                }
            }
        }
    }
}