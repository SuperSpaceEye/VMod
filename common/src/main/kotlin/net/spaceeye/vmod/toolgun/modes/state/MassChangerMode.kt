package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.gui.MassChangerGUI
import net.spaceeye.vmod.toolgun.modes.hud.MassChangerHUD
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.shipAttachments.WeightSynchronizer
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.apigame.world.chunks.BlockType
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld

class MassChangerMode: ExtendableToolgunMode(), MassChangerGUI, MassChangerHUD {
    @JsonIgnore private var i = 0

    var newMass: Double by get(i++, 1000.0) { ServerLimits.instance.massLimit.get(it) }
    var persistent: Boolean by get(i++, true)

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        val ship = raycastResult.ship as? ServerShip ?: return

        if (persistent) {
            val sship = level.shipObjectWorld.loadedShips.getById(ship.id)!!

            val atch = WeightSynchronizer()
            atch.syncWithConnectedStructure = false
            atch.lastDimensionId = level.dimensionId
            atch.dimensionId = level.dimensionId
            atch.targetWeight = newMass
            atch.updateWeights = true
            atch.shipId = sship.id
            atch.level = level

            sship.setAttachment(atch.javaClass, atch)

            return
        } else {
            val sship = level.shipObjectWorld.loadedShips.getById(ship.id)!!
            sship.removeAttachment<WeightSynchronizer>()
        }

        val aabb = ship.shipAABB!!

        var defaultTotalMass = 0.0
        val blocks = mutableListOf<Tuple3<Double, BlockPos, BlockType>>()

        for (x in aabb.minX()-1..aabb.maxX()+1) {
        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
        for (y in aabb.minY()-1..aabb.maxY()+1) {
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) {continue}
            val (mass, type) = BlockStateInfo.get(state) ?: continue
            defaultTotalMass += mass
            blocks.add(Tuple.of(mass, bpos, type))
        } } }

        blocks.forEach { (defaultMass, pos, type) ->
            val mass = defaultMass / defaultTotalMass * newMass
            CustomBlockMassManager.setCustomMass(level, pos.x, pos.y, pos.z, mass, type, defaultMass, ship)
        }
    }

    fun activateSecondaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return}
        val ship = raycastResult.ship as? ServerShip ?: return

        val sship = level.shipObjectWorld.loadedShips.getById(ship.id)
        if (sship?.getAttachment<WeightSynchronizer>() != null) {
            sship.removeAttachment<WeightSynchronizer>()
        }

        val aabb = ship.shipAABB!!

        var defaultTotalMass = 0.0
        val blocks = mutableListOf<Tuple3<Double, BlockPos, BlockType>>()

        for (x in aabb.minX()-1..aabb.maxX()+1) {
        for (z in aabb.minZ()-1..aabb.maxZ()+1) {
        for (y in aabb.minY()-1..aabb.maxY()+1) {
            val bpos = BlockPos(x, y, z)
            val state = level.getBlockState(bpos)
            if (state.isAir) {continue}
            val (mass, type) = BlockStateInfo.get(state) ?: continue
            defaultTotalMass += mass
            blocks.add(Tuple.of(mass, bpos, type))
        } } }

        for ((defaultMass, pos, type) in blocks) {
            val oldMass = CustomBlockMassManager.getCustomMass(ship.chunkClaimDimension, pos.x, pos.y, pos.z) ?: continue
            CustomBlockMassManager.setCustomMass(level, pos.x, pos.y, pos.z, defaultMass, type, oldMass, ship)
        }
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(MassChangerMode::class) {
                it.addExtension<MassChangerMode> {
                    BasicConnectionExtension<MassChangerMode>("ship_mass_changer"
                        ,leftFunction  = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,rightFunction = { inst, level, player, rr -> inst.activateSecondaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}