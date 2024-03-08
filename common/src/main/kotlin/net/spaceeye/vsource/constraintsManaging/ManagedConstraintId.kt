package net.spaceeye.vsource.constraintsManaging

import net.minecraft.world.entity.player.Player
import net.spaceeye.vsource.toolgun.ServerToolGunState

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

fun ManagedConstraintId?.addFor(player: Player): ManagedConstraintId? {
    ServerToolGunState.playerStates[player.uuid]?.constraintsStack?.add(this ?: return null)
    return this
}