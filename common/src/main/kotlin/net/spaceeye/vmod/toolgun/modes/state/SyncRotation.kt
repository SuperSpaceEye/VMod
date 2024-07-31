package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.SyncRotationMConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.eventsHandling.SyncRotationCEH
import net.spaceeye.vmod.toolgun.modes.gui.SyncRotationGUI
import net.spaceeye.vmod.toolgun.modes.hud.SyncRotationHUD
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.utils.RaycastFunctions
import org.joml.Quaterniond

class SyncRotation: BaseMode, SyncRotationHUD, SyncRotationGUI, SyncRotationCEH {
    var compliance: Double by get(0, 1e-20)
    var maxForce: Double by get(1, 1e20)

    var primaryFirstRaycast: Boolean by get(2, false)

    val conn_primary = register { object : C2SConnection<SyncRotation>("sync_rotation_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SyncRotation>(context.player, buf, ::SyncRotation) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(PositionModes.NORMAL, 1, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeManagedConstraint(
            SyncRotationMConstraint(shipId1, shipId2,
                ship1?.transform?.shipToWorldRotation ?: Quaterniond(),
                ship2?.transform?.shipToWorldRotation ?: Quaterniond(), compliance, maxForce)
        ) { it.addFor(player) }

        resetState()
    }

    override fun resetState() {
        primaryFirstRaycast = false
        previousResult = null
    }
}