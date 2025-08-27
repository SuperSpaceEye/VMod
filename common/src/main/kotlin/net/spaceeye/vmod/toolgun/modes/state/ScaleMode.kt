package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.toolgun.modes.gui.ScaleGUI
import net.spaceeye.vmod.toolgun.modes.hud.ScaleHUD
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
import net.spaceeye.vmod.utils.vs.teleportShipWithConnected
import net.spaceeye.vmod.utils.vs.traverseGetConnectedShips
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentHashMap

class ScaleMode: ExtendableToolgunMode(), ScaleGUI, ScaleHUD {
    @JsonIgnore private var i = 0

    var scale: Double by get(i++, 1.0) { ServerLimits.instance.scale.get(it) }.presettable()
    var scaleAllConnected: Boolean by get(i++, true).presettable().setSetWrapper { old, new -> connectedShips.forEach { ShipsColorModulator.deleteColor(it) }; connectedShips.clear(); new}

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel
        val ship: ServerShip = raycastResult.ship as ServerShip? ?: return

        if (scaleAllConnected) {
            teleportShipWithConnected(level, ship, Vector3d(ship.transform.positionInWorld), Quaterniond(ship.transform.shipToWorldRotation), scale)
        } else {
            level.shipObjectWorld.teleportShip(ship, ShipTeleportDataImpl(ship.transform.positionInWorld, ship.transform.shipToWorldRotation, ship.velocity, ship.omega, ship.chunkClaimDimension, scale))
        }
    }

    override fun eOnCloseMode() {
        connectedShips.forEach { ShipsColorModulator.deleteColor(it) }
        connectedShips.clear()
    }

    companion object {
        private var connectedShips = ConcurrentHashMap.newKeySet<Long>()

        data class C2SQueryConnectedShips(var shipId: ShipId): AutoSerializable
        val c2sQueryConnectedShips = regC2S<C2SQueryConnectedShips>(MOD_ID, "query_connected_ships", "scale_mode") { pkt, player ->
            val result = traverseGetConnectedShips(pkt.shipId)
            s2cQueryResponse.sendToClient(player, S2CQueryResponse(result.traversedShipIds.toLongArray()))
        }
        data class S2CQueryResponse(var shipIDs: LongArray): AutoSerializable
        val s2cQueryResponse = regS2C<S2CQueryResponse>(MOD_ID, "query_response", "scale_mode") { pkt ->
            connectedShips.clear()
            connectedShips.addAll(pkt.shipIDs.toList())
        }

        init {
            ToolgunModes.registerWrapper(ScaleMode::class) {
                it.addExtension {
                    BasicConnectionExtension<ScaleMode>("scale_mode"
                        ,leftFunction = { item, level, player, rr -> item.activatePrimaryFunction(level, player, rr) }
                    )
                }.addExtension {
                    ConstantClientRaycastingExtension<ScaleMode>({ mode, rr ->
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
                        if (mode.scaleAllConnected) {
                            c2sQueryConnectedShips.sendToServer(C2SQueryConnectedShips(rr.shipId))
                        } else {
                            connectedShips.add(rr.shipId)
                        }
                    })
                }.addExtension { Presettable() }
            }
        }
    }
}