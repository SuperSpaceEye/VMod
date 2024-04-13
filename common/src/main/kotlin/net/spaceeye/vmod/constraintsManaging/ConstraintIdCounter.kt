package net.spaceeye.vmod.constraintsManaging

import net.spaceeye.vmod.utils.ServerClosable

internal class ConstraintIdCounter : ServerClosable() {
    private var counter = 0

    fun peekID(): ManagedConstraintId = counter
    fun getID(): ManagedConstraintId = counter++
    fun dec(): ManagedConstraintId = counter--

    fun setCounter(id: Int) {counter = id}

    override fun close() {
        counter = 0
    }
}