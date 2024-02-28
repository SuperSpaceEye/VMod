package net.spaceeye.vsource.constraintsManaging

class ManagedConstraintId(@JvmField val id: Int) {
    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManagedConstraintId

        return id == other.id
    }

    override fun toString() = id.toString()
}