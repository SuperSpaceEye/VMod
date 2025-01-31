package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.SliderMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.gui.SliderGUI
import net.spaceeye.vmod.toolgun.modes.hud.SliderHUD
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementAssistExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.EmptyPacket
import net.spaceeye.vmod.utils.RaycastFunctions
import org.joml.Quaterniond

class SliderMode: ExtendableToolgunMode(), SliderGUI, SliderHUD {
    @JsonIgnore private var i = 0

    var maxForce: Float by get(i++, -1f, { ServerLimits.instance.maxForce.get(it) })
    var connectionMode: SliderMConstraint.ConnectionMode by get(i++, SliderMConstraint.ConnectionMode.FIXED_ORIENTATION)


    val posMode: PositionModes get() = getExtensionOfType<PlacementModesExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementModesExtension>().precisePlacementAssistSideNum

    var shipRes1: RaycastFunctions.RaycastResult? = null
    var shipRes2: RaycastFunctions.RaycastResult? = null
    var axisRes1: RaycastFunctions.RaycastResult? = null

    var primaryTimes = 0

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycastAndActivateFn(posMode, precisePlacementAssistSideNum, level, raycastResult) {
        level, shipId, ship, spoint, rpoint, rresult ->

        player as ServerPlayer

        if (shipRes1 == null && rresult.ship == null) { return@serverRaycastAndActivateFn sresetState(player) }
        val shipRes1 = shipRes1 ?: run {
            shipRes1 = rresult
            return@serverRaycastAndActivateFn
        }

        val shipRes2 = shipRes2 ?: run {
            shipRes2 = rresult
            if (shipRes1.shipId != shipRes2!!.shipId) { return@serverRaycastAndActivateFn sresetState(player) }
            return@serverRaycastAndActivateFn
        }

        val axisRes1 = axisRes1 ?: run {
            axisRes1 = rresult
            return@serverRaycastAndActivateFn
        }

        val axisRes2 = rresult

        if (axisRes1.shipId != axisRes2.shipId) { return@serverRaycastAndActivateFn sresetState(player) }
        if (axisRes1.shipId == shipRes1.shipId) { return@serverRaycastAndActivateFn sresetState(player) }

        val axisPair = getModePositions(posMode, axisRes1, axisRes2, precisePlacementAssistSideNum)
        val shipPair = getModePositions(posMode, shipRes1, shipRes2, precisePlacementAssistSideNum)

        level.makeManagedConstraint(SliderMConstraint(
            (axisPair.first + axisPair.second) / 2,
            (shipPair.first + shipPair.second) / 2,
            (axisPair.first - axisPair.second).normalize(),
            (shipPair.first - shipPair.second).normalize(),
            Quaterniond(axisRes1.ship?.transform?.shipToWorldRotation ?: Quaterniond()),
            Quaterniond(shipRes1.ship?.transform?.shipToWorldRotation ?: Quaterniond()),
            axisRes1.shipId, shipRes1.shipId,
            maxForce, connectionMode,
            setOf(axisRes1.blockPosition, shipRes1.blockPosition,
                  axisRes2.blockPosition, shipRes2.blockPosition).toList()
        ).addExtension(Strippable())) {it.addFor(player)}

        sresetState(player)
    }

    fun sresetState(player: ServerPlayer) {
        ServerToolGunState.s2cTooglunWasReset.sendToClient(player, EmptyPacket())
        resetState()
    }

    override fun eResetState() {
        shipRes1 = null
        shipRes2 = null
        axisRes1 = null
        primaryTimes = 0
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(SliderMode::class) {
                it.addExtension<SliderMode> {
                    BasicConnectionExtension<SliderMode>("slider_mode"
                        ,allowResetting = true
                        ,leftFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,leftClientCallback = { inst -> inst.primaryTimes++; inst.refreshHUD() }
                    )
                }.addExtension<SliderMode> {
                    BlockMenuOpeningExtension<SliderMode> { inst -> inst.primaryTimes != 0 }
                }.addExtension<SliderMode> {
                    PlacementModesExtension(true)
                }
            }
        }
    }
}