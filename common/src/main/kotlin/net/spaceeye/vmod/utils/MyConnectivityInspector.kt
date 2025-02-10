package net.spaceeye.vmod.utils

import org.jgrapht.Graph
import org.jgrapht.event.*
import org.jgrapht.graph.AsUndirectedGraph
import org.jgrapht.traverse.BreadthFirstIterator
import org.jgrapht.util.CollectionUtil
import java.util.*

/**
 * Allows obtaining various connectivity aspects of a graph. The *inspected graph* is specified
 * at construction time and cannot be modified. Currently, the inspector supports connected
 * components for an undirected graph and weakly connected components for a directed graph. To find
 * strongly connected components, use [KosarajuStrongConnectivityInspector] instead.
 *
 *
 *
 * The inspector methods work in a lazy fashion: no computation is performed unless immediately
 * necessary. Computation are done once and results and cached within this class for future need.
 *
 *
 *
 *
 * The inspector is also a [GraphListener]. If added as a listener to the
 * inspected graph, the inspector will amend internal cached results instead of recomputing them. It
 * is efficient when a few modifications are applied to a large graph. If many modifications are
 * expected it will not be efficient due to added overhead on graph update operations. If inspector
 * is added as listener to a graph other than the one it inspects, results are undefined.
 *
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Barak Naveh
 * @author John V. Sichi
</E></V> */
class MyConnectivityInspector<V, E>(g: Graph<V, E>) : GraphListener<V, E> {
    private var connectedSets: MutableSet<MutableSet<V>>? = null
    private var vertexToConnectedSet: MutableMap<V, MutableSet<V>> = mutableMapOf<V, MutableSet<V>>()
    private var graph: Graph<V, E>

    /**
     * Creates a connectivity inspector for the specified graph.
     *
     * @param g the graph for which a connectivity inspector to be created.
     */
    init {
        init()
        this.graph = g
        if (g.type.isDirected) this.graph = AsUndirectedGraph<V, E>(g)
    }

    val isConnected: Boolean
        /**
         * Test if the inspected graph is connected. A graph is connected when there is a path between
         * every pair of vertices. In a connected graph, there are no unreachable vertices. When the
         * inspected graph is a *directed* graph, this method returns true if and only if the
         * inspected graph is *weakly* connected. An empty graph is *not* considered
         * connected.
         *
         * @return `true` if and only if inspected graph is connected.
         */
        get() = lazyFindConnectedSets().size == 1

    /**
     * Returns a set of all vertices that are in the maximally connected component together with the
     * specified vertex. For more on maximally connected component, see
     * [
 * http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html](http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html).
     *
     * @param vertex the vertex for which the connected set to be returned.
     *
     * @return a set of all vertices that are in the maximally connected component together with the
     * specified vertex.
     */
    fun connectedSetOf(vertex: V): MutableSet<V> = vertexToConnectedSet[vertex] ?: run {
        if (!graph.containsVertex(vertex)) {return@run mutableSetOf()}
        val connectedSet = mutableSetOf<V>()
        BreadthFirstIterator<V, E>(graph, vertex).forEach { vertex ->
            connectedSet.add(vertex)
            vertexToConnectedSet[vertex] = connectedSet
        }
        connectedSet
    }


    /**
     * Returns a list of `Set` s, where each set contains all vertices that are in the
     * same maximally connected component. All graph vertices occur in exactly one set. For more on
     * maximally connected component, see
     * [
 * http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html](http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html).
     *
     * @return Returns a list of `Set` s, where each set contains all vertices that are
     * in the same maximally connected component.
     */
    fun connectedSets(): MutableSet<MutableSet<V>> {
        return lazyFindConnectedSets()
    }

    /**
     * @see GraphListener.edgeAdded
     */
    override fun edgeAdded(e: GraphEdgeChangeEvent<V, E>) {
        val source = e.getEdgeSource()
        val target = e.getEdgeTarget()
        val sourceSet = connectedSetOf(source)
        val targetSet = connectedSetOf(target)

        // If source and target are in the same set, do nothing, otherwise, merge sets
        if (sourceSet !== targetSet) {
            val merge = CollectionUtil.newHashSetWithExpectedSize<V>(sourceSet.size + targetSet.size)
            merge.addAll(sourceSet)
            merge.addAll(targetSet)
            connectedSets!!.remove(sourceSet)
            connectedSets!!.remove(targetSet)
            connectedSets!!.add(merge)
            for (v in merge) vertexToConnectedSet.put(v, merge)
        }
    }

    /**
     * @see GraphListener.edgeRemoved
     */
    override fun edgeRemoved(e: GraphEdgeChangeEvent<V, E>) {
        val source = e.edgeSource
        val target = e.edgeTarget

        val iter = BreadthFirstIterator(graph, source)
        iter.forEach { if (it == target) {return} }

        connectedSets!!.remove(vertexToConnectedSet[source])

        vertexToConnectedSet.remove(source)
        vertexToConnectedSet.remove(target)

        val sourceSet = connectedSetOf(source)
        val targetSet = connectedSetOf(target)

        connectedSets!!.add(sourceSet)
        connectedSets!!.add(targetSet)
    }

    /**
     * Tests whether two vertices lay respectively in the same connected component (undirected
     * graph), or in the same weakly connected component (directed graph).
     *
     * @param sourceVertex one end of the path.
     * @param targetVertex another end of the path.
     *
     * @return `true` if and only if the source and target vertex are in the same
     * connected component (undirected graph), or in the same weakly connected component
     * (directed graph).
     */
    fun pathExists(sourceVertex: V, targetVertex: V): Boolean {
        return connectedSetOf(sourceVertex).contains(targetVertex)
    }

    /**
     * @see VertexSetListener.vertexAdded
     */
    override fun vertexAdded(e: GraphVertexChangeEvent<V>) {
        val component: MutableSet<V> = HashSet<V>()
        component.add(e.getVertex())
        connectedSets!!.add(component)
        vertexToConnectedSet.put(e.getVertex(), component)
    }

    /**
     * @see VertexSetListener.vertexRemoved
     */
    override fun vertexRemoved(e: GraphVertexChangeEvent<V>) {
        val removed = e.vertex!!

        val connected = vertexToConnectedSet.remove(removed)!!
        connectedSets!!.remove(connected)
        connected.remove(removed)
        if (connected.isEmpty()) {
            return
        }

        val subLists = mutableListOf<MutableSet<V>>()

        while (connected.isNotEmpty()) {
            val subConnected = mutableSetOf<V>()

            val aVertex = connected.first()!!
            BreadthFirstIterator(graph, aVertex).forEach { subConnected.add(it!!) }
            connected.removeAll(subConnected)
            subLists.add(subConnected)
        }

        subLists.forEach { set ->
            connectedSets!!.add(set)
            set.forEach {
                vertexToConnectedSet[it] = set
            }
        }
    }

    private fun init() {
        connectedSets = null
        vertexToConnectedSet = mutableMapOf<V, MutableSet<V>>()
    }

    private fun lazyFindConnectedSets(): MutableSet<MutableSet<V>> {
        if (connectedSets == null) {
            connectedSets = mutableSetOf<MutableSet<V>>()

            val vertexSet = graph.vertexSet()

            if (!vertexSet.isEmpty()) {
                val i = BreadthFirstIterator<V, E>(graph)
                i.addTraversalListener(MyTraversalListener())

                while (i.hasNext()) {
                    i.next()
                }
            }
        }

        return connectedSets!!
    }

    /**
     * A traversal listener that groups all vertices according to their containing connected set.
     *
     * @author Barak Naveh
     */
    private inner class MyTraversalListener : TraversalListenerAdapter<V, E>() {
        private var currentConnectedSet: MutableSet<V> = mutableSetOf()

        /**
         * @see TraversalListenerAdapter.connectedComponentFinished
         */
        override fun connectedComponentFinished(e: ConnectedComponentTraversalEvent?) {
            connectedSets!!.add(currentConnectedSet)
        }

        /**
         * @see TraversalListenerAdapter.connectedComponentStarted
         */
        override fun connectedComponentStarted(e: ConnectedComponentTraversalEvent?) {
            currentConnectedSet = mutableSetOf()
        }

        /**
         * @see TraversalListenerAdapter.vertexTraversed
         */
        override fun vertexTraversed(e: VertexTraversalEvent<V>) {
            val v = e.getVertex()
            currentConnectedSet.add(v)
            vertexToConnectedSet.put(v, currentConnectedSet)
        }
    }
}
