package net.spaceeye.vmod.toolgun.modes.state

import gg.essential.elementa.components.UIContainer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Blocks
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.translate.makeFake
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.BlockStateInfo

class TestMode: ExtendableToolgunMode() {
    override val itemName = makeFake("Test Mode")

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        super.eMakeGUISettings(parentWindow)
    }

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        val ship = raycastResult.ship as? ServerShip ?: return

        val aabb = ship.shipAABB ?: return

        val (airMass, airType) = BlockStateInfo.get(Blocks.AIR.defaultBlockState()) ?: return

//        val bpos = raycastResult.blockPosition
//        val state = level.getBlockState(bpos)
//        val (mass, type) = BlockStateInfo.get(state) ?: return
//        level.shipObjectWorld.onSetBlock(bpos.x, bpos.y, bpos.z, level.dimensionId, type, airType, mass, mass)

//        for (x in aabb.minX()-1..aabb.maxX()+1) {
//        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
//        for (y in aabb.minY()-1..aabb.maxY()+1) {
//            val bpos = BlockPos(x, y, z)
//            val state = level.getBlockState(bpos)
//            if (state.isAir) {continue}
//            val (mass, type) = BlockStateInfo.get(state) ?: continue
//
//            level.setBlockAndUpdate(bpos, Blocks.AIR.defaultBlockState())
//            level.shipObjectWorld.onSetBlock(bpos.x, bpos.y, bpos.z, level.dimensionId, airType, type, airMass, mass)
//        } } }

        var totalMass = 0.0
        for (x in aabb.minX()-1..aabb.maxX()+1) {
        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
        for (y in aabb.minY()-1..aabb.maxY()+1) {
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) {continue}
            val (mass, type) = BlockStateInfo.get(state) ?: continue
            totalMass += mass
            CustomBlockMassManager.setCustomMass(level, x, y, z, 0.0)
        } } }

        val bpos = raycastResult.blockPosition
//        level.setBlockAndUpdate(bpos, Blocks.AIR.defaultBlockState())
//        val (_, type) = BlockStateInfo.get(Blocks.TNT.defaultBlockState()) ?: return
//        level.shipObjectWorld.onSetBlock(bpos.x, bpos.y, bpos.z, level.dimensionId, airType, type, airMass, totalMass)
        CustomBlockMassManager.setCustomMass(level, bpos.x, bpos.y, bpos.z, totalMass)

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