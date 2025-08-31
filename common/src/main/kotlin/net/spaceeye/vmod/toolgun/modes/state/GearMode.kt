package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.vEntityManaging.addForVMod
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.vEntityManaging.types.constraints.GearConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.toolgun.gui.Presettable.Companion.presettable
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.toolgun.modes.gui.GearGUI
import net.spaceeye.vmod.toolgun.modes.hud.GearHUD
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.utils.RaycastFunctions
import org.joml.Quaterniond

class GearMode: ExtendableToolgunMode(), GearGUI, GearHUD {
    @JsonIgnore
    private var i = 0

    var maxForce: Float by get(i++, -1f) { ServerLimits.instance.maxForce.get(it) }.presettable()
    var gearRatio: Float by get(i++, 1f) { ServerLimits.instance.gearRatio.get(it) }.presettable()

    var primaryFirstRaycast: Boolean by get(i++, false)

    val posMode: PositionModes get() = getExtensionOfType<PlacementModesExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementModesExtension>().precisePlacementAssistSideNum

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeVEntity(GearConstraint(
            spoint1, spoint2,
            prresult.globalNormalDirection!!,
            rresult.globalNormalDirection!!,
            Quaterniond(ship1?.transform?.shipToWorldRotation ?: Quaterniond()),
            Quaterniond(ship2?.transform?.shipToWorldRotation ?: Quaterniond()),
            shipId1, shipId2, maxForce, gearRatio
            ).addExtension(Strippable())){it.addForVMod(player)}

        resetState()
    }

    override fun eResetState() {
        previousResult = null
        primaryFirstRaycast = false
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(GearMode::class) {
                it.addExtension {
                    BasicConnectionExtension<GearMode>(
                        "gear_mode",
                        allowResetting = true,
                        leftFunction = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) },
                        leftClientCallback = { inst ->
                            inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD()
                        }
                    )
                }.addExtension {
                    PlacementModesExtension(true)
                }.addExtension {
                    BlockMenuOpeningExtension<GearMode> { inst -> inst.primaryFirstRaycast }
                }
            }
        }
    }
}