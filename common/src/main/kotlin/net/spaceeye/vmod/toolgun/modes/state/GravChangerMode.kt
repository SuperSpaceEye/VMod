package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.shipAttachments.GravityController
import net.spaceeye.vmod.toolgun.modes.gui.GravChangerGUI
import net.spaceeye.vmod.toolgun.modes.hud.GravChangerHUD
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.ShipsColorModulator
import net.spaceeye.vmod.toolgun.gui.Presettable
import net.spaceeye.vmod.toolgun.gui.Presettable.Companion.presettable
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.ConstantClientRaycastingExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.traverseGetAllTouchingShips
import net.spaceeye.vmod.utils.vs.traverseGetConnectedShips
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentHashMap

class GravChangerMode: ExtendableToolgunMode(), GravChangerHUD, GravChangerGUI {
    enum class Mode {
        Individual,
        AllConnected,
        AllConnectedAndTouching
    }
    @JsonIgnore private var i = 0

    var gravityVector: Vector3d by get(i++, Vector3d(0, -10, 0)).presettable()
    var mode: Mode by get(i++, Mode.Individual).presettable().setSetWrapper { old, new -> connectedShips.forEach { ShipsColorModulator.deleteColor(it) }; connectedShips.clear(); new}

    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val origin = level.shipObjectWorld.loadedShips.getById((level.getShipManagingPos(raycastResult.globalHitPos?.toBlockPos() ?: return) ?: return).id) ?: return

        val traversed = when (mode) {
            Mode.Individual -> {
                GravityController.getOrCreate(origin).also {
                    it.gravityVector = Vector3d(gravityVector)
                    it.useDimensionGravity = false
                }
                return
            }
            Mode.AllConnected -> {
                traverseGetConnectedShips(origin.id).traversedShipIds
            }
            Mode.AllConnectedAndTouching -> {
                traverseGetAllTouchingShips(level, origin.id)
            }
        }

        traversed.forEach { id ->
            GravityController
                .getOrCreate(level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach).also {
                    it.gravityVector = Vector3d(gravityVector)
                    it.useDimensionGravity = false
                }
        }
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val origin = level.shipObjectWorld.loadedShips.getById((level.getShipManagingPos(raycastResult.globalHitPos?.toBlockPos() ?: return) ?: return).id) ?: return


        val traversed = when (mode) {
            Mode.Individual -> {
                GravityController.getOrCreate(origin).reset()
                return
            }
            Mode.AllConnected -> {
                traverseGetConnectedShips(origin.id).traversedShipIds
            }
            Mode.AllConnectedAndTouching -> {
                traverseGetAllTouchingShips(level, origin.id)
            }
        }

        traversed.forEach { id ->
            GravityController
                .getOrCreate(level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach)
                .reset()
        }
    }

    override fun eOnCloseMode() {
        connectedShips.forEach { ShipsColorModulator.deleteColor(it) }
        connectedShips.clear()
    }

    companion object {
        private var connectedShips = ConcurrentHashMap.newKeySet<Long>()

        data class C2SQueryConnectedShips(var shipId: ShipId, var alsoTouching: Boolean): AutoSerializable
        val c2sQueryConnectedShips = regC2S<C2SQueryConnectedShips>(MOD_ID, "query_connected_ships", "grav_changer_mode") { pkt, player ->
            val result = if (!pkt.alsoTouching) {
                traverseGetConnectedShips(pkt.shipId).traversedShipIds
            } else {
                traverseGetAllTouchingShips(player.serverLevel(), pkt.shipId)
            }
            s2cQueryResponse.sendToClient(player, S2CQueryResponse(result.toLongArray()))
        }
        data class S2CQueryResponse(var shipIDs: LongArray): AutoSerializable
        val s2cQueryResponse = regS2C<S2CQueryResponse>(MOD_ID, "query_response", "grav_changer_mode") { pkt ->
            connectedShips.clear()
            connectedShips.addAll(pkt.shipIDs.toList())
        }

        init {
            ToolgunModes.registerWrapper(GravChangerMode::class) {
                it.addExtension {
                    BasicConnectionExtension<GravChangerMode>("grav_changer_mode"
                        ,allowResetting = true
                        ,leftFunction  = { item, level, player, rr -> item.activatePrimaryFunction(level, player, rr) }
                        ,rightFunction = { item, level, player, rr -> item.activateSecondaryFunction(level, player, rr)}
                    )
                }.addExtension {
                    ConstantClientRaycastingExtension<GravChangerMode>({ mode, rr ->
                        if (rr.shipId == -1L) {
                            connectedShips.forEach { ShipsColorModulator.deleteColor(it) }
                            connectedShips.clear()
                            return@ConstantClientRaycastingExtension
                        }
                        if (connectedShips.contains(rr.shipId)) {
                            connectedShips.forEach { ShipsColorModulator.setColor(it, floatArrayOf(0.5f, 1f, 0.5f, 1f)) }
                            return@ConstantClientRaycastingExtension
                        }
                        connectedShips.forEach { ShipsColorModulator.deleteColor(it) }
                        connectedShips.clear()
                        if (mode.mode != Mode.Individual) {
                            c2sQueryConnectedShips.sendToServer(C2SQueryConnectedShips(rr.shipId, mode.mode == Mode.AllConnectedAndTouching))
                        } else {
                            connectedShips.add(rr.shipId)
                        }
                    })
                }.addExtension { Presettable() }
            }
        }
    }
}