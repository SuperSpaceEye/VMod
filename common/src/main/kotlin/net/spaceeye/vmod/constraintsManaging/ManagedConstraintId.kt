package net.spaceeye.vmod.constraintsManaging

import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.toolgun.ServerToolGunState

typealias ManagedConstraintId = Int

fun ManagedConstraintId?.addFor(player: Player): ManagedConstraintId? {
    ServerToolGunState.playersConstraintsStack.getOrPut(player.uuid) { mutableListOf() }.add(this ?: return null)
    return this
}