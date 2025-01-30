package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
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
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementAssistExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.RaycastFunctions

class RopeMode: ExtendableToolgunMode(), RopeGUI, RopeHUD {
    @JsonIgnore private var i = 0

    var maxForce: Float by get(i++, -1f, { ServerLimits.instance.maxForce.get(it) })
    var stiffness: Float by get(i++, 0f, {ServerLimits.instance.stiffness.get(it)})
    var damping: Float by get(i++, 0f, {ServerLimits.instance.damping.get(it)})

    var fixedDistance: Float by get(i++, -1.0f, {ServerLimits.instance.fixedDistance.get(it)})

    var primaryFirstRaycast: Boolean by get(i++, false)

    var segments: Int by get(i++, 16, { IntLimit(1, 100).get(it)})
    var width: Double by get(i++, .2, { DoubleLimit(0.01).get(it)})


    val posMode: PositionModes get() = getExtensionOfType<PlacementAssistExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementAssistExtension>().precisePlacementAssistSideNum

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val dist = if (fixedDistance > 0) {fixedDistance} else {(rpoint1 - rpoint2).dist().toFloat()}

        level.makeManagedConstraint(RopeMConstraint(
            spoint1, spoint2,
            shipId1, shipId2,
            maxForce, stiffness, damping, dist,
            listOf(prresult.blockPosition, rresult.blockPosition),
        ).addExtension(RenderableExtension(RopeRenderer(
            ship1?.id ?: -1L,
            ship2?.id ?: -1L,
            spoint1, spoint2,
            dist.toDouble(), width, segments
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
                        ,leftFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,leftClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension<RopeMode> {
                    PlacementModesExtension(true)
                }.addExtension<RopeMode> {
                    BlockMenuOpeningExtension<RopeMode> { inst -> inst.primaryFirstRaycast }
                }
            }
        }
    }
}