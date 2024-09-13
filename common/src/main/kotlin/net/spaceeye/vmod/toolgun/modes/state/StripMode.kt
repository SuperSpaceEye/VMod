package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.extensions.NonStrippable
import net.spaceeye.vmod.constraintsManaging.util.ExtendableMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.gui.StripGUI
import net.spaceeye.vmod.toolgun.modes.hud.StripHUD
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.mod.common.getShipManagingPos
import kotlin.math.ceil
import kotlin.math.max

class StripMode: ExtendableToolgunMode(), StripGUI, StripHUD {
    enum class StripModes {
        StripAll,
        StripInRadius
    }

    var radius: Double by get(0, 1.0, {ServerLimits.instance.stripRadius.get(it)})
    var mode: StripModes by get(1, StripModes.StripAll)

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        when (mode) {
            StripModes.StripAll -> stripAll(level as ServerLevel, raycastResult)
            StripModes.StripInRadius -> stripInRadius(level as ServerLevel, raycastResult)
        }
    }

    private fun stripAll(level: ServerLevel, raycastResult: RaycastFunctions.RaycastResult) {
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return

        level.getAllManagedConstraintIdsOfShipId(ship.id).forEach {
            val mc = level.getManagedConstraint(it)
            if (mc is ExtendableMConstraint && mc.getExtensionsOfType<NonStrippable>().isNotEmpty()) { return@forEach }
            level.removeManagedConstraint(it)
        }
    }

    private fun stripInRadius(level: ServerLevel, raycastResult: RaycastFunctions.RaycastResult) {
        val instance = ConstraintManager.getInstance()

        val b = raycastResult.blockPosition
        val r = max(ceil(radius).toInt(), 1)

        for (x in b.x-r .. b.x+r) {
        for (y in b.y-r .. b.y+r) {
        for (z in b.z-r .. b.z+r) {
            val list = instance.tryGetIdsOfPosition(BlockPos(x, y, z)) ?: continue
            val temp = mutableListOf<ManagedConstraintId>()
            temp.addAll(list)
            temp.forEach {const ->
                val mc = level.getManagedConstraint(const)
                //TODO make "Strippable" instead of NonStrippable
                if (mc is ExtendableMConstraint && mc.getExtensionsOfType<NonStrippable>().isNotEmpty()) { return@forEach }
                mc!!.getAttachmentPoints().forEach {
                    if ((it - raycastResult.globalHitPos!!).dist() <= radius) {
                        level.removeManagedConstraint(const)
                    }
                }
            }
        } } }
    }


    companion object {
        init {
            ToolgunModes.registerWrapper(StripMode::class) {
                it.addExtension<StripMode> {
                    BasicConnectionExtension<StripMode>("strip_mode"
                        ,primaryFunction = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}