package net.spaceeye.vmod.vsStuff

import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.mixin.ShipObjectWorldAccessor
import net.spaceeye.vmod.utils.MyConnectivityInspector
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.jetbrains.annotations.ApiStatus
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultListenableGraph
import org.jgrapht.graph.Multigraph
import org.jgrapht.graph.concurrent.AsSynchronizedGraph
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.mod.common.shipObjectWorld

object VSJointsTracker {
    private val graph = DefaultListenableGraph(AsSynchronizedGraph(Multigraph<Long, DefaultEdge>(DefaultEdge::class.java)))
    private val inspector = MyConnectivityInspector(graph) //todo this is optimal
    private val edgeInfo = mutableMapOf<Set<Long>, Int>()
    //is needed so that connectivity doesn't connect ships constrained to ground
    private var groundId = -1L

    init {
        graph.addGraphListener(inspector)
        inspector.connectedSets()

        AVSEvents.serverShipRemoveEvent.on { (ship), _ ->
            graph.removeVertex(ship.id)
        }
    }

    @JvmStatic
    fun getVSJoints(ids: List<VSJointId>): List<Pair<VSJointId, VSJoint?>> {
        val acc = ServerLevelHolder.server!!.shipObjectWorld as ShipObjectWorldAccessor
        return ids.map { Pair(it, acc.constraints[it]) }
    }

    @JvmStatic
    fun getIdsOfShip(shipId: ShipId): Set<Int> {
        return (ServerLevelHolder.shipObjectWorld!! as ShipObjectWorldAccessor).shipIdToConstraints.getOrDefault(shipId, setOf())
    }

    @JvmStatic
    fun shipsAreConnected(shipId1: ShipId, shipId2: ShipId) = inspector.pathExists(shipId1, shipId2)

    @JvmStatic
    fun getConnected(shipId: ShipId) = inspector.connectedSetOf(shipId).toSet()



    @JvmStatic
    private fun screenShipId(shipId: ShipId?): ShipId {
        if (shipId == null) {return groundId--}
        val dimensionIds = ServerLevelHolder.shipObjectWorld!!.dimensionToGroundBodyIdImmutable.values
        if (dimensionIds.contains(shipId)) {return groundId--}
        return shipId
    }

    @JvmStatic
    @ApiStatus.Internal
    fun onCreateNewConstraint(joint: VSJoint) {
        val shipId1 = screenShipId(joint.shipId0)
        val shipId2 = screenShipId(joint.shipId1)
        val pair = setOf(shipId1, shipId2)

        var count = edgeInfo.getOrPut(pair) { 0 }
        edgeInfo[pair] = count + 1

        graph.addVertex(shipId1)
        graph.addVertex(shipId2)
        graph.addEdge(shipId1, shipId2)
    }

    @JvmStatic
    @ApiStatus.Internal
    fun onUpdateConstraint(id: Int, oldJoint: VSJoint?, newJoint: VSJoint) {
        if (oldJoint == null) {return onCreateNewConstraint(newJoint)}

        val shipId1O = screenShipId(oldJoint.shipId0)
        val shipId2O = screenShipId(oldJoint.shipId1)

        val shipId1N = screenShipId(newJoint.shipId0)
        val shipId2N = screenShipId(newJoint.shipId1)

        if (shipId1O == shipId1N && shipId2O == shipId2N) {return}
        onRemoveConstraint(id, oldJoint)
        onCreateNewConstraint(newJoint)
    }

    @JvmStatic
    @ApiStatus.Internal
    fun onRemoveConstraint(id: Int, joint: VSJoint?) {
        val joint = joint ?: return
        val shipId1 = screenShipId(joint.shipId0)
        val shipId2 = screenShipId(joint.shipId1)
        val pair = setOf(shipId1, shipId2)

        var count = edgeInfo.getOrElse(pair) { ELOG("SHOULDN'T BE POSSIBLE. HOW!!!!!!!!!"); return } - 1
        edgeInfo[pair] = count
        if (count > 0) {return}

        edgeInfo.remove(pair)
        graph.removeEdge(shipId1, shipId2)
    }
}