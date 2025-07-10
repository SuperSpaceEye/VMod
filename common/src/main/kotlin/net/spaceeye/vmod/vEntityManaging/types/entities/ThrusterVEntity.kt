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
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.makeVEntityWithId
import net.spaceeye.vmod.vEntityManaging.removeVEntity
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.world.PhysLevelCore
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.CompletableFuture

class ThrusterVEntity(): ExtendableVEntity(), Tickable, VEAutoSerializable {
    @JsonIgnore private var i = 0

    var shipId: ShipId by get(i++, -1)
    var pos: Vector3d by get(i++, Vector3d())
    var forceDir: Vector3d by get(i++, Vector3d())
    var force: Double by get(i++, 1.0)
    var channel: String by get(i++, "")
    var percentage: Double = 0.0

    var compiledForce = JVector3d()

    constructor(shipId: ShipId, pos: Vector3d, forceDir: Vector3d, force: Double, channel: String): this() {
        this.shipId = shipId
        this.pos = pos
        this.forceDir = forceDir
        this.force = force
        this.channel = channel
    }

    override fun iStillExists(allShips: QueryableShipData<Ship>) = allShips.contains(shipId)
    override fun iAttachedToShips() = mutableListOf(shipId)
    override fun iGetAttachmentPoints(qshipId: ShipId): List<Vector3d> = if (shipId == qshipId || qshipId == -1L) listOf(Vector3d(pos)) else emptyList()

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        if (!VMConfig.SERVER.SCALE_THRUSTERS_THRUST) {return}

        force *= scaleBy * scaleBy * scaleBy
        lastPercentage = -1.0 //to force update
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
        val nId = mapped[shipId] ?: return null
        val (oldCenter, newCenter) = centerPositions[shipId] ?: return null
        val nShip = level.shipObjectWorld.loadedShips.getById(nId) ?: level.shipObjectWorld.allShips.getById(nId) ?: return null

        val nPos = pos - oldCenter + newCenter

        return ThrusterVEntity(nShip.id, nPos, Vector3d(forceDir), force, channel)
    }

    override fun iMoveAttachmentPoints(level: ServerLevel, pointsToMove: List<Vector3d>, oldShipId: ShipId, newShipId: ShipId, oldCenter: Vector3d, newCenter: Vector3d): Boolean {
        val copy = copyVEntity(level, mapOf(oldShipId to newShipId), mapOf(oldShipId to (oldCenter to newCenter)))!!
        level.removeVEntity(this)
        level.makeVEntityWithId(copy, mID) {}
        return false
    }

    override fun iOnMakeVEntity(level: ServerLevel) = listOf(CompletableFuture<Boolean>().also { it.complete(level.shipObjectWorld.allShips.getById(shipId) != null) })
    override fun iOnDeleteVEntity(level: ServerLevel) { wasRemoved = true }

    override fun iNbtSerialize(): CompoundTag? {
        val tag = super<VEAutoSerializable>.iNbtSerialize()
        tag?.putDouble("percentage", percentage)
        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag): VEntity? {
        percentage = tag.getDouble("percentage")
        return super<VEAutoSerializable>.iNbtDeserialize(tag)
    }

    private var wasRemoved = false
    private var lastPercentage = percentage

    override fun serverTick(server: MinecraftServer, unregister: () -> Unit) {
        if (wasRemoved) {unregister(); return}

        getExtensionsOfType<TickableVEntityExtension>().forEach { it.tick(server) }

        if (lastPercentage == percentage) {return}
        lastPercentage = percentage

        compiledForce = (forceDir * (force * percentage)).toJomlVector3d()
    }

    @OptIn(VsBeta::class)
    override fun physTick(level: PhysLevelCore, delta: Double) {
        println("working")
        val ship = level.getShipById(shipId) ?: return

        val forcePos = pos - Vector3d(ship.transform.positionInModel)
        ship.applyRotDependentForceToPos(compiledForce, forcePos.toJomlVector3d())
    }
}