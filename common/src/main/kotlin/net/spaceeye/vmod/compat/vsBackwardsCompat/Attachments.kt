package net.spaceeye.vmod.compat.vsBackwardsCompat

import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.getAttachment

inline fun <reified T : Any> ServerShip.getAttachment() = this.getAttachment<T>()
inline fun <reified T : Any> ServerShip.removeAttachment() = this.saveAttachment<T>(T::class.java, null)