package net.spaceeye.vsource.utils.constraintsSaving

import net.spaceeye.vsource.utils.ServerClosable
import org.valkyrienskies.core.apigame.constraints.VSConstraintId

internal class ConstraintIdManager : ServerClosable() {
    private val idMap = mutableMapOf<ManagedConstraintId, VSConstraintId>()
    private var counter = 0

    fun getVSid(id: ManagedConstraintId) = idMap[id]
    fun addVSid(id: VSConstraintId): ManagedConstraintId {
        val newManagedId = ManagedConstraintId(counter)
        counter++
        idMap[newManagedId] = id

        return newManagedId
    }

    fun setVSid(id: VSConstraintId, to: Int): ManagedConstraintId {
        val to = if (to < 0) {counter} else {to}
        val newManagedId = ManagedConstraintId(to)
        idMap[newManagedId] = id
        if (id >= counter) {counter = id + 1}

        return newManagedId
    }

    override fun close() {
        counter = 0
        idMap.clear()
    }
}