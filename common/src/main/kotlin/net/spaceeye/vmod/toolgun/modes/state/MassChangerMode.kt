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
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.apigame.world.chunks.BlockType
import org.valkyrienskies.mod.common.BlockStateInfo

class MassChangerMode: ExtendableToolgunMode(), MassChangerGUI, MassChangerHUD {
    @JsonIgnore private var i = 0

    var newMass: Double by get(i++, 1000.0) { ServerLimits.instance.massLimit.get(it) }

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        val ship = raycastResult.ship as? ServerShip ?: return

        val aabb = ship.shipAABB!!

        var defaultTotalMass = 0.0
        val blocks = mutableListOf<Tuple3<Double, BlockPos, BlockType>>()

        for (x in aabb.minX()..aabb.maxX()) {
        for (z in aabb.minZ()..aabb.maxZ()) {
        for (y in aabb.minY()..aabb.maxY()) {
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

        val aabb = ship.shipAABB!!

        var defaultTotalMass = 0.0
        val blocks = mutableListOf<Tuple3<Double, BlockPos, BlockType>>()

        for (x in aabb.minX()..aabb.maxX()) {
        for (z in aabb.minZ()..aabb.maxZ()) {
        for (y in aabb.minY()..aabb.maxY()) {
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