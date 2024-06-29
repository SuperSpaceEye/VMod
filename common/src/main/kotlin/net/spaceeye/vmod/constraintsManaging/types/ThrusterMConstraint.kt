package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.network.Message
import net.spaceeye.vmod.network.MessagingNetwork
import net.spaceeye.vmod.network.Signal
import net.spaceeye.vmod.rendering.SynchronisedRenderingData
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.shipForceInducers.ThrustersController
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getVector3d
import net.spaceeye.vmod.utils.putVector3d
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.mod.common.shipObjectWorld

class ThrusterMConstraint(): MConstraint, MRenderable, Tickable {
    override val typeName: String get() = "ThrusterMConstraint"
    override var mID: ManagedConstraintId = -1
    override var saveCounter: Int = -1

    var shipId: ShipId = -1
    var pos = Vector3d()
    var bpos = BlockPos(0, 0, 0)
    var forceDir = Vector3d()
    var force: Double = 1.0
    var percentage: Double = 0.0

    var channel = ""

    var thrusterId: Int = -1

    override var renderer: BaseRenderer? = null

    var rID = -1

    constructor(shipId: ShipId, pos: Vector3d, bpos: BlockPos, forceDir: Vector3d, force: Double, channel: String, renderer: BaseRenderer): this() {
        this.shipId = shipId
        this.pos = pos
        this.bpos = bpos
        this.forceDir = forceDir
        this.force = force
        this.channel = channel

        this.renderer = renderer
    }

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        return allShips.contains(shipId)
    }

    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        return mutableListOf(shipId)
    }

    override fun getAttachmentPoints(): List<BlockPos> = listOf(bpos)
    override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        throw AssertionError()
    }
    override fun getVSIds(): Set<VSConstraintId> = setOf()
    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        throw AssertionError()
//        return ThrusterMConstraint(mapped[shipId] ?: return null, Vector3d(pos), bpos, Vector3d(forceDir), force, channel)
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: return false

        MessagingNetwork.register(channel) {
            msg, unregister ->
            if (wasRemoved) {unregister(); return@register}
            signalTick(msg)
        }

        val controller = ThrustersController.getOrCreate(ship)

        thrusterId = controller.newThruster(pos, forceDir, force)

        if (renderer != null) { rID = SynchronisedRenderingData.serverSynchronisedData.addRenderer(shipId, shipId, renderer!!)
        } else { renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(rID) }

        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        wasRemoved = true
        SynchronisedRenderingData.serverSynchronisedData.removeRenderer(rID)
        val ship = level.shipObjectWorld.allShips.getById(shipId) ?: return

        val controller = ThrustersController.getOrCreate(ship)

        controller.removeThruster(thrusterId)
    }

    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        throw AssertionError()
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = CompoundTag()
        tag.putInt("managedId", mID)

        tag.putLong("shipId", shipId)
        tag.putInt("thrusterId", thrusterId)
        tag.putDouble("force", force)
        tag.putVector3d("pos", pos.toJomlVector3d())
        tag.putVector3d("forceDir", forceDir.toJomlVector3d())
        tag.putLong("bpos", bpos.asLong())
        tag.putDouble("percentage", percentage)
        tag.putString("channel", channel)

        serializeRenderer(tag)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("managedId")

        shipId = tag.getLong("shipId")
        thrusterId = tag.getInt("thrusterId")
        force = tag.getDouble("force")
        pos = Vector3d(tag.getVector3d("pos") ?: return null)
        forceDir = Vector3d(tag.getVector3d("forceDir") ?: return null)
        bpos = BlockPos.of(tag.getLong("bpos"))
        percentage = tag.getDouble("percentage")
        channel = tag.getString("channel")

        deserializeRenderer(tag)

        return this
    }

    private fun signalTick(msg: Message) {
        if (msg !is Signal) {return}
        percentage = msg.percentage
    }

    private var wasRemoved = false
    private var lastPercentage = percentage

    override fun tick(server: MinecraftServer, unregister: () -> Unit) {
        if (wasRemoved) {unregister(); return}
        if (lastPercentage == percentage) {return}
        lastPercentage = percentage

        if (!ThrustersController
            .getOrCreate(server.shipObjectWorld.loadedShips.getById(shipId) ?: return )
            .activateThruster(thrusterId, percentage)) { unregister(); return }
    }
}