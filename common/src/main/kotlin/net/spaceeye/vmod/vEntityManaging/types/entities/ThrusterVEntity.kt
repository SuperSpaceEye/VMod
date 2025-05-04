package net.spaceeye.vmod.vEntityManaging.types.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.Tickable
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import net.spaceeye.vmod.vEntityManaging.util.VEAutoSerializable
import net.spaceeye.vmod.vEntityManaging.util.TickableVEntityExtension
import net.spaceeye.vmod.shipAttachments.ThrustersController
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld

class ThrusterVEntity(): ExtendableVEntity(), Tickable, VEAutoSerializable {
    @JsonIgnore private var i = 0

    var shipId: ShipId by get(i++, -1)
    var pos: Vector3d by get(i++, Vector3d())
    var forceDir: Vector3d by get(i++, Vector3d())
    var force: Double by get(i++, 1.0)
    var channel: String by get(i++, "")
    var thrusterId: Int by get(i++, -1)
    var percentage: Double = 0.0

    constructor(shipId: ShipId, pos: Vector3d, forceDir: Vector3d, force: Double, channel: String): this() {
        this.shipId = shipId
        this.pos = pos
        this.forceDir = forceDir
        this.force = force
        this.channel = channel
    }

    override fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>) = allShips.contains(shipId)
    override fun iAttachedToShips(dimensionIds: Collection<ShipId>) = mutableListOf(shipId)
    override fun iGetAttachmentPoints(qshipId: ShipId): List<Vector3d> = if (shipId == qshipId || qshipId == -1L) listOf(Vector3d(pos)) else emptyList()


    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        if (!VMConfig.SERVER.SCALE_THRUSTERS_THRUST) {return}

        val ship = level.shipObjectWorld.loadedShips.getById(shipId)!!
        val controller = ThrustersController.getOrCreate(ship)

        val thruster = controller.getThruster(thrusterId)!!
        controller.updateThruster(thrusterId, thruster.copy(force = thruster.force * scaleBy * scaleBy * scaleBy))
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
        val nId = mapped[shipId] ?: return null
        val (oldCenter, newCenter) = centerPositions[shipId] ?: return null
        val nShip = level.shipObjectWorld.loadedShips.getById(nId) ?: level.shipObjectWorld.allShips.getById(nId) ?: return null

        val nPos = pos - oldCenter + newCenter

        return ThrusterVEntity(nShip.id, nPos, Vector3d(forceDir), force, channel)
    }

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: return false

        val controller = ThrustersController.getOrCreate(ship)

        thrusterId = controller.newThruster(pos, forceDir, force)

        return true
    }

    override fun iOnDeleteVEntity(level: ServerLevel) {
        wasRemoved = true
        val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: return

        val controller = ThrustersController.getOrCreate(ship)

        controller.removeThruster(thrusterId)
    }

    override fun iNbtSerialize(): CompoundTag? {
        val tag = super<VEAutoSerializable>.iNbtSerialize()
        tag?.putDouble("percentage", percentage)
        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): VEntity? {
        percentage = tag.getDouble("percentage")
        return super<VEAutoSerializable>.iNbtDeserialize(tag, lastDimensionIds)
    }

    private var wasRemoved = false
    private var lastPercentage = percentage

    override fun tick(server: MinecraftServer, unregister: () -> Unit) {
        if (wasRemoved) {unregister(); return}

        getExtensionsOfType<TickableVEntityExtension>().forEach { it.tick(server) }

        if (lastPercentage == percentage) {return}
        lastPercentage = percentage

        if (!ThrustersController
            .getOrCreate(server.shipObjectWorld.loadedShips.getById(shipId) ?: return )
            .activateThruster(thrusterId, percentage)) { unregister(); return }
    }
}