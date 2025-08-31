package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.vEntityManaging.addForVMod
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.vEntityManaging.types.constraints.SyncRotationConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.gui.SyncRotationGUI
import net.spaceeye.vmod.toolgun.modes.hud.SyncRotationHUD
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import org.joml.Quaterniond

class SyncRotation: ExtendableToolgunMode(), SyncRotationHUD, SyncRotationGUI {
    @JsonIgnore private var i = 0

    var maxForce: Float by get(i++, -1f) { ServerLimits.instance.maxForce.get(it) }
    var primaryFirstRaycast: Boolean by get(i++, false)

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(PositionModes.NORMAL, 1, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeVEntity(
            SyncRotationConstraint(
                (Quaterniond(ship1?.transform?.shipToWorldRotation ?: Quaterniond())).invert(),
                (Quaterniond(ship2?.transform?.shipToWorldRotation ?: Quaterniond())).invert(),
                shipId1, shipId2, maxForce
            ).addExtension(Strippable())
        ) { it.addForVMod(player) }

        resetState()
    }

    override fun eResetState() {
        primaryFirstRaycast = false
        previousResult = null
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(SyncRotation::class) {
                it.addExtension {
                    BasicConnectionExtension<SyncRotation>("sync_rotation_mode"
                        ,allowResetting = true
                        ,leftFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,leftClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension {
                    BlockMenuOpeningExtension<SyncRotation> { inst -> inst.primaryFirstRaycast }
                }
            }
        }
    }
}