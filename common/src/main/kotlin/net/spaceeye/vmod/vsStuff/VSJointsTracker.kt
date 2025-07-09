package net.spaceeye.vmod.vsStuff

import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.utils.MyConnectivityInspector
import net.spaceeye.vmod.utils.SafeEventEmitter
import net.spaceeye.vmod.utils.ServerObjectsHolder
import org.jetbrains.annotations.ApiStatus
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultListenableGraph
import org.jgrapht.graph.Multigraph
import org.jgrapht.graph.concurrent.AsSynchronizedGraph
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.core.apigame.world.PhysLevelCore
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.locks.ReentrantLock

object VSJointsTracker {
    private val graph = DefaultListenableGraph(AsSynchronizedGraph(Multigraph<Long, DefaultEdge>(DefaultEdge::class.java)))
    private val inspector = MyConnectivityInspector(graph) //todo this is not optimal
    private val edgeInfo = mutableMapOf<Set<Long>, Int>()

    val connectionAdded   = SafeEventEmitter<OnConnectionAdded>()
    val connectionRemoved = SafeEventEmitter<OnConnectionRemoved>()

    data class OnConnectionAdded  (val shipId1: ShipId, val shipId2: ShipId)
    data class OnConnectionRemoved(val shipId1: ShipId, val shipId2: ShipId)

    //evil hack
    private val lock = ReentrantLock()
    private val physLevels = mutableMapOf<String, PhysLevelCore>()

    init {
        graph.addGraphListener(inspector)
        inspector.connectedSets()

        AVSEvents.serverShipRemoveEvent.on { (ship), _ ->
            graph.removeVertex(ship.id)
        }

        vsApi.physTickEvent.on {
            if (!lock.tryLock()) return@on
            physLevels[it.world.dimension] = (it.world as PhysLevelCore)
            lock.unlock()
        }
    }

    @JvmStatic
    fun getVSJoints(ids: List<VSJointId>): List<Pair<VSJointId, VSJoint?>> {
        lock.lock()
        val exist = mutableSetOf<VSJointId>()
        val ret = physLevels
            .map { (_, level) -> ids
                .filter { !exist.contains(it) }
                .mapNotNull { id -> level.getJointById(id)?.let { exist.add(id); Pair<VSJointId, VSJoint?>(id, it) } } }
            .flatten().toMutableList()
        ids.forEach { if (!exist.contains(it)) ret.add(Pair<VSJointId, VSJoint?>(it, null)) }

        lock.unlock()
        return ret
    }

    @JvmStatic
    fun getIdsOfShip(shipId: ShipId): Set<Int> {
        physLevels.forEach {
            val ship = it.value.getShipById(shipId) ?: return@forEach
            return it.value.getJointsFromShip(ship.id)
        }
        return emptySet()
    }

    @JvmStatic
    fun shipsAreConnected(shipId1: ShipId, shipId2: ShipId) = inspector.pathExists(shipId1, shipId2)

    @JvmStatic
    fun getConnected(shipId: ShipId) = inspector.connectedSetOf(shipId).toSet()



    @JvmStatic
    private fun screenShipId(shipId: ShipId?): ShipId? {
        if (shipId == null || ServerObjectsHolder.shipObjectWorld!!.dimensionToGroundBodyIdImmutable.values.contains(shipId)) {return null}
        return shipId
    }


    //TODO this shit is called from diff thread
    @JvmStatic
    @ApiStatus.Internal
    fun onCreateNewConstraint(joint: VSJoint) {
        val shipId1 = screenShipId(joint.shipId0) ?: return
        val shipId2 = screenShipId(joint.shipId1) ?: return
        if (shipId1 == shipId2) {return}
        val pair = setOf(shipId1, shipId2)

        var count = edgeInfo.getOrPut(pair) { 0 }
        if (count == 0) {
            graph.addVertex(shipId1)
            graph.addVertex(shipId2)
            graph.addEdge(shipId1, shipId2)
            connectionAdded.emit(OnConnectionAdded(shipId1, shipId2))
        }
        edgeInfo[pair] = count + 1
    }

    @JvmStatic
    @ApiStatus.Internal
    fun onUpdateConstraint(id: Int, oldJoint: VSJoint?, newJoint: VSJoint) {
        if (oldJoint == null) {return onCreateNewConstraint(newJoint)}
        if (oldJoint.shipId0 == newJoint.shipId0 && oldJoint.shipId1 == newJoint.shipId1) {return}

        onRemoveConstraint(id, oldJoint)
        onCreateNewConstraint(newJoint)
    }

    @JvmStatic
    @ApiStatus.Internal
    fun onRemoveConstraint(id: Int, joint: VSJoint?) {
        val joint = joint ?: return
        val shipId1 = screenShipId(joint.shipId0) ?: return
        val shipId2 = screenShipId(joint.shipId1) ?: return
        val pair = setOf(shipId1, shipId2)

        var count = edgeInfo.getOrElse(pair) { ELOG("SHOULDN'T BE POSSIBLE. HOW!!!!!!!!!"); return } - 1
        edgeInfo[pair] = count
        if (count > 0) {return}

        edgeInfo.remove(pair)
        graph.removeEdge(shipId1, shipId2)
        connectionRemoved.emit(OnConnectionRemoved(shipId1, shipId2))
    }
}