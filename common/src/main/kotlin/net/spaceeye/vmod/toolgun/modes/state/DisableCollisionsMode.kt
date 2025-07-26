package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.types.constraints.DisabledCollisionConstraint
import net.spaceeye.vmod.toolgun.modes.gui.DisableCollisionsGUI
import net.spaceeye.vmod.toolgun.modes.hud.DisableCollisionHUD
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.mod.common.getShipManagingPos

class DisableCollisionsMode: ExtendableToolgunMode(), DisableCollisionHUD, DisableCollisionsGUI {
    @JsonIgnore private var i = 0

    var primaryFirstRaycast: Boolean by get(i++, false)

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(PositionModes.NORMAL, 3, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeVEntity(DisabledCollisionConstraint(shipId1, shipId2).addExtension(Strippable())){it.addForVMod(player)}
        resetState()
    }

    fun activateSecondaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) {
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
                it.addExtension {
                    BasicConnectionExtension<DisableCollisionsMode>("disable_connections_mode"
                        ,allowResetting = true
                        ,leftFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,rightFunction      = { inst, level, player, rr -> inst.activateSecondaryFunction(level, player, rr)}
                        ,leftClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                        ,blockRight = { inst -> inst.primaryFirstRaycast}
                    )
                }.addExtension {
                    BlockMenuOpeningExtension<DisableCollisionsMode> { inst -> inst.primaryFirstRaycast }
                }
            }
        }
    }
}