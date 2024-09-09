package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.ExtendableMConstraint
import net.spaceeye.vmod.network.Message
import net.spaceeye.vmod.network.MessagingNetwork
import net.spaceeye.vmod.network.Signal
import net.spaceeye.vmod.shipForceInducers.ThrustersController
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getVector3d
import net.spaceeye.vmod.utils.putVector3d
import net.spaceeye.vmod.utils.vs.getCenterPos
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.mod.common.shipObjectWorld

class ThrusterMConstraint(): ExtendableMConstraint(), Tickable {
    var shipId: ShipId = -1
    var pos = Vector3d()
    var bpos = BlockPos(0, 0, 0)
    var forceDir = Vector3d()
    var force: Double = 1.0
    var percentage: Double = 0.0

    var channel = ""

    var thrusterId: Int = -1

    constructor(shipId: ShipId, pos: Vector3d, bpos: BlockPos, forceDir: Vector3d, force: Double, channel: String): this() {
        this.shipId = shipId
        this.pos = pos
        this.bpos = bpos
        this.forceDir = forceDir
        this.force = force
        this.channel = channel
    }

    override fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>) = allShips.contains(shipId)
    override fun iAttachedToShips(dimensionIds: Collection<ShipId>) = mutableListOf(shipId, shipId)
    override fun iGetAttachmentPositions(): List<BlockPos> = listOf(bpos, bpos)
    override fun iGetAttachmentPoints(): List<Vector3d> = listOf(Vector3d(pos), Vector3d(pos))
    override fun iGetVSIds(): Set<VSConstraintId> = setOf()


    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        if (!VMConfig.SERVER.SCALE_THRUSTERS_THRUST) {return}

        val ship = level.shipObjectWorld.loadedShips.getById(shipId)!!
        val controller = ThrustersController.getOrCreate(ship)

        val thruster = controller.getThruster(thrusterId)!!
        controller.updateThruster(thrusterId, thruster.copy(force = thruster.force * scaleBy * scaleBy * scaleBy))
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        val nId = mapped[shipId] ?: return null

        val nShip = level.shipObjectWorld.loadedShips.getById(nId) ?: level.shipObjectWorld.allShips.getById(nId) ?: return null

        val oCentered = getCenterPos(pos.x.toInt(), pos.z.toInt())
        val nCentered = getCenterPos(nShip.transform.positionInShip.x().toInt(), nShip.transform.positionInShip.z().toInt())

        val nPos = pos - oCentered + nCentered
        val nBPos = (Vector3d(bpos) - oCentered + nCentered).toBlockPos()

        return ThrusterMConstraint(nShip.id, nPos, nBPos, Vector3d(forceDir), force, channel)
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: return false

        MessagingNetwork.register(channel) {
            msg, unregister ->
            if (wasRemoved) {unregister(); return@register}
            signalTick(msg)
        }

        val controller = ThrustersController.getOrCreate(ship)

        thrusterId = controller.newThruster(pos, forceDir, force)

        return true
    }

    override fun iOnDeleteMConstraint(level: ServerLevel) {
        wasRemoved = true
        val ship = level.shipObjectWorld.allShips.getById(shipId) ?: return

        val controller = ThrustersController.getOrCreate(ship)

        controller.removeThruster(thrusterId)
    }

    override fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        throw NotImplementedError()
        if (previous != bpos) {return}

        val oShip = level.shipObjectWorld.loadedShips.getById(shipId) ?: return
        val oController = ThrustersController.getOrCreate(oShip)
        oController.removeThruster(thrusterId)

//        ServerRenderingData.removeRenderer(rID)

        shipId = newShipId
        pos = (pos - Vector3d(bpos) + Vector3d(new))
        bpos = new

        val nShip = level.shipObjectWorld.loadedShips.getById(newShipId) ?: level.shipObjectWorld.allShips.getById(newShipId) ?: return
        val nController = ThrustersController.getOrCreate(nShip)
        thrusterId = nController.newThruster(pos, forceDir, force)

//        rID = ServerRenderingData.addRenderer(shipId, shipId, renderer!!)
    }

    override fun iNbtSerialize(): CompoundTag? {
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

        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("managedId")

        shipId = tag.getLong("shipId")
        thrusterId = tag.getInt("thrusterId")
        force = tag.getDouble("force")
        pos = Vector3d(tag.getVector3d("pos") ?: return null)
        forceDir = Vector3d(tag.getVector3d("forceDir") ?: return null)
        bpos = BlockPos.of(tag.getLong("bpos"))
        percentage = tag.getDouble("percentage")
        channel = tag.getString("channel")

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