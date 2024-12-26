package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.SyncRotationMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.gui.SyncRotationGUI
import net.spaceeye.vmod.toolgun.modes.hud.SyncRotationHUD
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import org.joml.Quaterniond

class SyncRotation: ExtendableToolgunMode(), SyncRotationHUD, SyncRotationGUI {
    var compliance: Double by get(0, 1e-20, {ServerLimits.instance.compliance.get(it)})
    var maxForce: Double by get(1, 1e20, {ServerLimits.instance.maxForce.get(it)})
    var primaryFirstRaycast: Boolean by get(2, false)


    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(PositionModes.NORMAL, 1, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeManagedConstraint(
            SyncRotationMConstraint(shipId1, shipId2,
                ship1?.transform?.shipToWorldRotation ?: Quaterniond(),
                ship2?.transform?.shipToWorldRotation ?: Quaterniond(), compliance, maxForce
            ).addExtension(Strippable())
        ) { it.addFor(player) }

        resetState()
    }

    override fun eResetState() {
        primaryFirstRaycast = false
        previousResult = null
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(SyncRotation::class) {
                it.addExtension<SyncRotation> {
                    BasicConnectionExtension<SyncRotation>("sync_rotation_mode"
                        ,allowResetting = true
                        ,primaryFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,primaryClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension<SyncRotation> {
                    BlockMenuOpeningExtension<SyncRotation> { inst -> inst.primaryFirstRaycast }
                }
            }
        }
    }
}