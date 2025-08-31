package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.gui.PhysRopeGUI
import net.spaceeye.vmod.toolgun.modes.hud.PhysRopeHUD
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.types.PhysRopeRenderer
import net.spaceeye.vmod.toolgun.gui.Presettable
import net.spaceeye.vmod.toolgun.gui.Presettable.Companion.presettable
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.addForVMod
import net.spaceeye.vmod.vEntityManaging.extensions.PhysRopeRenderable
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.vEntityManaging.types.constraints.PhysRopeConstraint
import java.awt.Color

class PhysRopeMode: ExtendableToolgunMode(), PhysRopeGUI, PhysRopeHUD {
    @JsonIgnore private var i = 0
    var maxForce: Float by get(i++, -1f) { ServerLimits.instance.maxForce.get(it) }.presettable()
    var stiffness: Float by get(i++, -1f) { ServerLimits.instance.stiffness.get(it) }.presettable()
    var fixedDistance: Float by get(i++, -1f) { ServerLimits.instance.fixedDistance.get(it) }.presettable()

    var segments: Int by get(i++, 16) { ServerLimits.instance.physRopeSegments.get(it) }.presettable()
    var totalMass: Double by get(i++, 1000.0) { ServerLimits.instance.totalMassOfPhysRope.get(it) }.presettable()
    var radius: Double by get(i++, 0.5) { ServerLimits.instance.physRopeRadius.get(it) }.presettable()
    var angleLimit: Double by get(i++, 15.0) { ServerLimits.instance.physRopeAngleLimit.get(it) }.presettable()
    var sides: Int by get(i++, 8) { ServerLimits.instance.physRopeSides.get(it) }.presettable()
    var fullbright: Boolean by get(i++, false).presettable()

    var lengthUVStart: Float by get(i++, 0f).presettable()
    var lengthUVIncMultiplier: Float by get(i++, 1f).presettable()
    var widthUVStart: Float by get(i++, 0f).presettable()
    var widthUVMultiplier: Float by get(i++, 1f).presettable()

    var primaryFirstRaycast: Boolean by get(i++, false)


    val posMode: PositionModes get() = getExtensionOfType<PlacementModesExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementModesExtension>().precisePlacementAssistSideNum

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->
        val dist = if (fixedDistance > 0) {fixedDistance} else {(rpoint1 - rpoint2).dist().toFloat()}

        val sDir1 = prresult.globalNormalDirection!!
        val sDir2 = rresult.globalNormalDirection!!

        var up1 = if (sDir1.y < 0.01 && sDir1.y > -0.01) { Vector3d(0, 1, 0) } else { Vector3d(1, 0, 0) }
        var up2 = if (sDir2.y < 0.01 && sDir2.y > -0.01) { Vector3d(0, 1, 0) } else { Vector3d(1, 0, 0) }

        var right1 = sDir1.cross(up1)
        var right2 = (-sDir2).cross(up2)

        //rendering fuckery
        if (sDir1.y < -0.5) {
            up1 = -up1
            right1 = -right1
        }

        if (sDir2.y > 0.5) {
            up2 = -up2
            right2 = -right2
        }

        PhysRopeConstraint(
            spoint1, spoint2,
            sDir1, sDir2,
            shipId1, shipId2,
            stiffness, maxForce,
            dist, segments, totalMass / segments, radius, Math.toRadians(angleLimit)
        )   .also { it.addExtension(PhysRopeRenderable(PhysRopeRenderer(shipId1, shipId2, spoint1, spoint2, up1, up2, right1, right2, Color(255, 255, 255), sides, fullbright, longArrayOf(), RenderingUtils.ropeTexture, lengthUVStart, lengthUVIncMultiplier, widthUVStart, widthUVMultiplier))) }
            .addExtension(Strippable())
            .also {level.makeVEntity(it) {it.addForVMod(player)} }

        resetState()
    }

    override fun eResetState() {
        previousResult = null
        primaryFirstRaycast = false
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(PhysRopeMode::class) {
                it.addExtension {
                    BasicConnectionExtension<PhysRopeMode>("phys_rope_mode"
                        ,allowResetting = true
                        ,leftFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,leftClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension {
                    PlacementModesExtension(false)
                }.addExtension {
                    BlockMenuOpeningExtension<PhysRopeMode> { inst -> inst.primaryFirstRaycast }
                }.addExtension { Presettable() }
            }
        }
    }
}