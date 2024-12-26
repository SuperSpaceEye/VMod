package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.types.DisabledCollisionMConstraint
import net.spaceeye.vmod.toolgun.modes.gui.DisableCollisionsGUI
import net.spaceeye.vmod.toolgun.modes.hud.DisableCollisionHUD
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.mod.common.getShipManagingPos

class DisableCollisionsMode: ExtendableToolgunMode(), DisableCollisionHUD, DisableCollisionsGUI {
    var primaryFirstRaycast: Boolean by get(0, false)

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(PositionModes.NORMAL, 3, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeManagedConstraint(DisabledCollisionMConstraint(shipId1, shipId2).addExtension(Strippable())){it.addFor(player)}
        resetState()
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return}
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return
        level.getAllDisabledCollisionsOfId(ship.id)?.forEach { (id, num) -> for (i in 0 until num) { level.enableCollisionBetween(ship.id, id) } }
    }

    override fun eResetState() {
        previousResult = null
        primaryFirstRaycast = false
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(DisableCollisionsMode::class) {
                it.addExtension<DisableCollisionsMode> {
                    BasicConnectionExtension<DisableCollisionsMode>("disable_connections_mode"
                        ,allowResetting = true
                        ,primaryFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,secondaryFunction     = { inst, level, player, rr -> inst.activateSecondaryFunction(level, player, rr)}
                        ,primaryClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                        ,blockSecondary = {inst -> inst.primaryFirstRaycast}
                    )
                }.addExtension<DisableCollisionsMode> {
                    BlockMenuOpeningExtension<DisableCollisionsMode> { inst -> inst.primaryFirstRaycast }
                }
            }
        }
    }
}