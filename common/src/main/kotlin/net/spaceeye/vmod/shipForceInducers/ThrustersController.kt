package net.spaceeye.vmod.shipForceInducers

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import java.util.concurrent.locks.ReentrantLock

data class ThrusterData(
    var id: Int,
    var pos: JVector3d,
    var forceDir: JVector3d,
    var force: Double,
    var percentage: Double,

    var compiledForce: JVector3d,
) {
    fun toSave(): ThrustersController.ThrusterDataToSave =
        ThrustersController.ThrusterDataToSave(
            id,
            pos,
            forceDir,
            force,
            percentage
        )

    constructor(save: ThrustersController.ThrusterDataToSave): this(
        save.id,
        save.pos,
        save.forceDir,
        save.force,
        save.percentage,

        save.forceDir.mul(save.force, JVector3d())
    )

    fun deepCopy(): ThrusterData {
        return ThrusterData(
            id,
            JVector3d(pos),
            JVector3d(forceDir),
            force,
            percentage,
            JVector3d(compiledForce)
        )
    }
}

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class ThrustersController: ShipForcesInducer {
    data class ThrusterDataToSave(var id:Int, var pos: JVector3d, var forceDir: JVector3d, var force: Double, var percentage: Double)

    @JsonIgnore
    var lock = ReentrantLock()

    @JsonIgnore
    private var thrustersData = mutableMapOf<Int, ThrusterData>()
    private var id = 0

    override fun applyForces(physShip: PhysShip) {
        physShip as PhysShipImpl
        synchronized(lock) {
            thrustersData.values.forEach {
                if (it.percentage <= 0.0) {return@forEach}
                val pos = it.pos.sub(physShip.transform.positionInShip, JVector3d())

                physShip.applyRotDependentForceToPos(it.compiledForce, pos)
            }
        }
    }

    fun newThruster(pos: Vector3d, forceDir: Vector3d, force: Double): Int {
        synchronized(lock) {
            id++
            thrustersData[id] = ThrusterData(id, pos.toJomlVector3d(), forceDir.toJomlVector3d(), force, 0.0, (forceDir * force * 0.0).toJomlVector3d())
            return id
        }
    }

    fun getThruster(id: Int): ThrusterData? {
        val it = thrustersData[id] ?: return null
        return it.deepCopy()
    }

    fun updateThruster(id: Int, data: ThrusterData): Boolean {
        synchronized(lock) {
            if (data.id != id) { return false }
            thrustersData[id] ?: return false
            val copy = data.deepCopy()
            copy.compiledForce = copy.forceDir.mul(copy.force * copy.percentage, JVector3d())
            thrustersData[id] = copy
            return true
        }
    }

    fun removeThruster(id: Int): Boolean {
        synchronized(lock) {
            return thrustersData.remove(id) != null
        }
    }

    fun activateThruster(id: Int, percentage: Double): Boolean {
        val it = thrustersData[id] ?: return false

        it.compiledForce = it.forceDir.mul(it.force * percentage, JVector3d())
        it.percentage = percentage

        return true
    }

    private var thrustersDataSave: List<ThrusterDataToSave>
        get() = thrustersData.map { (k, v) -> v.toSave() }
        set(value) {thrustersData = value.associate { Pair(it.id, ThrusterData(it)) }.toMutableMap()}

    companion object {
        fun getOrCreate(ship: ServerShip) =
            ship.getAttachment<ThrustersController>()
                ?: ThrustersController().also {
                    ship.saveAttachment(it)
                }
    }
}