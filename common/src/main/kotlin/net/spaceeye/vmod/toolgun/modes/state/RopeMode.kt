package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.extensions.RenderableExtension
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.RopeMConstraint
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.IntLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.toolgun.modes.gui.RopeGUI
import net.spaceeye.vmod.toolgun.modes.hud.RopeHUD
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.RaycastFunctions

class RopeMode: ExtendableToolgunMode(), RopeGUI, RopeHUD {
    var compliance: Double by get(0, 1e-20, { ServerLimits.instance.compliance.get(it) })
    var maxForce: Double by get(1, 1e10, { ServerLimits.instance.maxForce.get(it) })
    var fixedDistance: Double by get(2, -1.0, {ServerLimits.instance.fixedDistance.get(it)})

    var primaryFirstRaycast: Boolean by get(3, false)

    var segments: Int by get(4, 16, { IntLimit(1, 100).get(it)})
    var width: Double by get(5, .2, { DoubleLimit(0.01).get(it)})


    var posMode: PositionModes = PositionModes.NORMAL
    var precisePlacementAssistSideNum: Int = 3

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val dist = if (fixedDistance > 0) {fixedDistance} else {(rpoint1 - rpoint2).dist()}

        level.makeManagedConstraint(RopeMConstraint(
            shipId1, shipId2,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, dist,
            listOf(prresult.blockPosition, rresult.blockPosition),
        ).addExtension(RenderableExtension(RopeRenderer(
            ship1?.id ?: -1L,
            ship2?.id ?: -1L,
            spoint1, spoint2,
            dist, width, segments
        ))).addExtension(Strippable())){it.addFor(player)}

        resetState()
    }

    override fun eResetState() {
        previousResult = null
        primaryFirstRaycast = false
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(RopeMode::class) {
                it.addExtension<RopeMode> {
                    BasicConnectionExtension<RopeMode>("rope_mode"
                        ,allowResetting = true
                        ,primaryFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,primaryClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension<RopeMode> {
                    PlacementModesExtension(true, {mode -> it.posMode = mode}, {num -> it.precisePlacementAssistSideNum = num})
                }.addExtension<RopeMode> {
                    BlockMenuOpeningExtension<RopeMode> { inst -> inst.primaryFirstRaycast }
                }
            }
        }
    }
}