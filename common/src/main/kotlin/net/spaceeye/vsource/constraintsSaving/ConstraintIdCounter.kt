package net.spaceeye.vsource.constraintsSaving

import net.spaceeye.vsource.utils.ServerClosable

internal class ConstraintIdCounter : ServerClosable() {
    private var counter = 0

    fun getID() = ManagedConstraintId(counter++)

    fun setCounter(id: Int) {counter = id}

    override fun close() {
        counter = 0
    }
}