package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.vEntityManaging.addFor
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.vEntityManaging.types.constraints.RopeConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.toolgun.modes.gui.RopeGUI
import net.spaceeye.vmod.toolgun.modes.hud.RopeHUD
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.RaycastFunctions

class RopeMode: ExtendableToolgunMode(), RopeGUI, RopeHUD {
    @JsonIgnore private var i = 0

    var maxForce: Float by get(i++, -1f) { ServerLimits.instance.maxForce.get(it) }
    var stiffness: Float by get(i++, -1f) { ServerLimits.instance.stiffness.get(it) }
    var damping: Float by get(i++, -1f) { ServerLimits.instance.damping.get(it) }

    var fixedDistance: Float by get(i++, -1.0f) { ServerLimits.instance.fixedDistance.get(it) }

    var primaryFirstRaycast: Boolean by get(i++, false)

    var segments: Int by get(i++, 16)
    var width: Double by get(i++, .2)
    var fullbright: Boolean by get(i++, false)


    val posMode: PositionModes get() = getExtensionOfType<PlacementModesExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementModesExtension>().precisePlacementAssistSideNum

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val dist = if (fixedDistance < 0) {(rpoint2 - rpoint1).dist().toFloat()} else {fixedDistance}

        level.makeVEntity(RopeConstraint(
            spoint1, spoint2,
            shipId1, shipId2,
            maxForce, stiffness, damping, dist,
            listOf(prresult.blockPosition, rresult.blockPosition),
        ).addExtension(RenderableExtension(RopeRenderer(
            ship1?.id ?: -1L,
            ship2?.id ?: -1L,
            spoint1, spoint2,
            dist.toDouble(), width, segments, fullbright
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