package net.spaceeye.vsource.utils

import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship

fun posShipToWorld(ship: Ship, pos: Vector3d): Vector3d {
    val scale = Vector3d(ship.transform.shipToWorldScaling)
    val ship_wp = Vector3d(ship.transform.positionInWorld)
    val ship_sp = Vector3d(ship.transform.positionInShip)
    return Vector3d((ship.transform.transformDirectionNoScalingFromShipToWorld(
        ((pos - ship_sp)*scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_wp
}

fun posWorldToShip(ship: Ship, pos: Vector3d): Vector3d {
    val scale = Vector3d(ship.transform.shipToWorldScaling)
    val ship_wp = Vector3d(ship.transform.positionInWorld)
    val ship_sp = Vector3d(ship.transform.positionInShip)
    return Vector3d((ship.transform.transformDirectionNoScalingFromWorldToShip(
            ((pos - ship_wp) / scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_sp
}

fun posShipToWorldRender(ship: ClientShip, pos: Vector3d): Vector3d {
    val scale = Vector3d(ship.renderTransform.shipToWorldScaling)
    val ship_wp = Vector3d(ship.renderTransform.positionInWorld)
    val ship_sp = Vector3d(ship.renderTransform.positionInShip)
    return Vector3d((ship.renderTransform.transformDirectionNoScalingFromShipToWorld(
        ((pos - ship_sp)*scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_wp
}

fun posWorldToShipRender(ship: ClientShip, pos: Vector3d): Vector3d {
    val scale = Vector3d(ship.renderTransform.shipToWorldScaling)
    val ship_wp = Vector3d(ship.renderTransform.positionInWorld)
    val ship_sp = Vector3d(ship.renderTransform.positionInShip)
    return Vector3d((ship.renderTransform.transformDirectionNoScalingFromWorldToShip(
        ((pos - ship_wp) / scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_sp
}