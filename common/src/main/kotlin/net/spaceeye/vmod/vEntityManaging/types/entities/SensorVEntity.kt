package net.spaceeye.vmod.vEntityManaging.types.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.Tickable
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import net.spaceeye.vmod.vEntityManaging.util.VEAutoSerializable
import net.spaceeye.vmod.network.MessagingNetwork
import net.spaceeye.vmod.network.Signal
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.utils.vs.transformDirectionShipToWorld
import org.valkyrienskies.mod.common.dimensionId
import kotlin.math.min

class SensorVEntity(): ExtendableVEntity(), Tickable, VEAutoSerializable {
    @JsonIgnore private var i = 0

    var shipId: ShipId by get(i++, -1)
    var pos: Vector3d by get(i++, Vector3d())
    var lookDir: Vector3d by get(i++, Vector3d())
    var maxDistance: Double by get(i++, 10.0)
    var channel: String by get(i++, "")
    var ignoreSelf: Boolean by get(i++, false)
    var scale: Double by get(i++, 1.0)


    constructor(shipId: ShipId,
                pos: Vector3d,
                lookDir: Vector3d,
                distance: Double,
                ignoreSelf: Boolean,
                scale: Double,
                channel: String): this() {
        this.shipId = shipId
        this.pos = pos
        this.channel = channel
        this.lookDir = lookDir
        this.maxDistance = distance
        this.ignoreSelf = ignoreSelf
        this.scale = scale
    }

    override fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>) = allShips.contains(shipId)
    override fun iAttachedToShips(dimensionIds: Collection<ShipId>) = mutableListOf(shipId)
    override fun iGetAttachmentPoints(qshipId: ShipId): List<Vector3d> = if (shipId == qshipId || qshipId == -1L) listOf(Vector3d(pos)) else emptyList()


    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        maxDistance *= scaleBy
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
        val nId = mapped[shipId] ?: return null
        val (oldCenter, newCenter) = centerPositions[shipId] ?: return null
        val nShip = level.shipObjectWorld.loadedShips.getById(nId) ?: level.shipObjectWorld.allShips.getById(nId) ?: return null

        val nPos = pos - oldCenter + newCenter

        return SensorVEntity(nShip.id, nPos, lookDir, maxDistance, ignoreSelf, scale, channel)
    }

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        level.shipObjectWorld.loadedShips.getById(shipId) ?: return false
        return true
    }

    override fun iOnDeleteVEntity(level: ServerLevel) {}

    override fun tick(server: MinecraftServer, unregister: () -> Unit) {
        val ship = server.shipObjectWorld.allShips.getById(shipId) ?: return
        ship.chunkClaimDimension

        var serverLevel: ServerLevel? = null
        for (level in server.allLevels) {
            if (level.dimensionId != ship.chunkClaimDimension) {continue}
            serverLevel = level
            break
        }
        if (serverLevel == null) {return}

        val result = RaycastFunctions.raycast(
            serverLevel,
            RaycastFunctions.Source(
                transformDirectionShipToWorld(ship, lookDir),
                posShipToWorld(ship, pos + lookDir * 0.5 * scale)
            ),
            maxDistance,
            if (ignoreSelf) {ship.id} else {null}
        )

        val distance = (result.origin - (result.worldHitPos ?: (result.origin + result.lookVec * maxDistance))).dist()

        MessagingNetwork.notify(channel, Signal(min(distance / maxDistance, 1.0)))
    }
}