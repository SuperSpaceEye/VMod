package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.vEntityManaging.addForVMod
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
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.types.TubeRopeRenderer
import net.spaceeye.vmod.toolgun.gui.Presettable
import net.spaceeye.vmod.toolgun.gui.Presettable.Companion.presettable
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d

class RopeMode: ExtendableToolgunMode(), RopeGUI, RopeHUD {
    @JsonIgnore private var i = 0

    var maxForce: Float by get(i++, -1f) { ServerLimits.instance.maxForce.get(it) }.presettable()
    var stiffness: Float by get(i++, -1f) { ServerLimits.instance.stiffness.get(it) }.presettable()
    var damping: Float by get(i++, -1f) { ServerLimits.instance.damping.get(it) }.presettable()

    var fixedDistance: Float by get(i++, -1.0f) { ServerLimits.instance.fixedDistance.get(it) }.presettable()

    var segments: Int by get(i++, 16).presettable()
    var sides: Int by get(i++, 4).presettable()
    var width: Double by get(i++, .2).presettable()
    var fullbright: Boolean by get(i++, false).presettable()

    var lengthUVStart: Float by get(i++, 0f).presettable()
    var lengthUVIncMultiplier: Float by get(i++, 1f).presettable()
    var widthUVStart: Float by get(i++, 0f).presettable()
    var widthUVMultiplier: Float by get(i++, 1f).presettable()

    var useTubeRenderer: Boolean by get(i++, false).presettable()
    var allowTwisting by get(i++, false).presettable()

    var primaryFirstRaycast: Boolean by get(i++, false)


    val posMode: PositionModes get() = getExtensionOfType<PlacementModesExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementModesExtension>().precisePlacementAssistSideNum

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, sPos1, sPos2, rPos1, rPos2, pr, rr ->

        val length = if (fixedDistance < 0) {(rPos2 - rPos1).dist().toFloat()} else {fixedDistance}

        val sDir1 = pr.globalNormalDirection!!
        val sDir2 = rr.globalNormalDirection!!

        var up1 = if (sDir1.y < 0.01 && sDir1.y > -0.01) { Vector3d(0, 1, 0) } else { Vector3d(1, 0, 0) }
        var up2 = if (sDir2.y < 0.01 && sDir2.y > -0.01) { Vector3d(0, 1, 0) } else { Vector3d(1, 0, 0) }

        var right1 = sDir1.cross(up1)
        var right2 = (-sDir2).cross(up2)

        val onTheOutside = posMode != PositionModes.CENTERED_IN_BLOCK

        level.makeVEntity(RopeConstraint(
            sPos1, sPos2,
            shipId1, shipId2,
            maxForce, stiffness, damping, length
        ).addExtension(RenderableExtension(
            when (useTubeRenderer) {
                true  -> TubeRopeRenderer(ship1?.id ?: -1L, ship2?.id ?: -1L, sPos1, sPos2, up1, up2, right1, right2, length.toDouble(), null, width, sides, segments, fullbright, allowTwisting, onTheOutside, RenderingUtils.ropeTexture, lengthUVStart, lengthUVIncMultiplier, widthUVStart, widthUVMultiplier)
                false -> RopeRenderer(ship1?.id ?: -1L, ship2?.id ?: -1L, sPos1, sPos2, length.toDouble(), null, width, segments, fullbright, RenderingUtils.ropeTexture, lengthUVStart, lengthUVIncMultiplier, widthUVStart, widthUVMultiplier)
            }
        )).addExtension(Strippable())){it.addForVMod(player)}

        resetState()
    }

    override fun eResetState() {
        previousResult = null
        primaryFirstRaycast = false
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(RopeMode::class) {
                it.addExtension {
                    BasicConnectionExtension<RopeMode>("rope_mode"
                        ,allowResetting = true
                        ,leftFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,leftClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension {
                    PlacementModesExtension(true)
                }.addExtension {
                    BlockMenuOpeningExtension<RopeMode> { inst -> inst.primaryFirstRaycast }
                }.addExtension { Presettable() }
            }
        }
    }
}