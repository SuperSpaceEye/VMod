package net.spaceeye.vmod.utils.vs

import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.mod.api.vsApi


fun ServerShip.body() = vsApi.getServerShipWorld()!!.allBodies.getById(this.bodyId!!)
fun ClientShip.body() = vsApi.getClientShipWorld()!!.allBodies.getById(this.bodyId!!)
fun Ship.body() = when (this) {
    is ClientShip -> this.body()
    is ServerShip -> this.body()
    else -> error("Unsupported ship type")
}