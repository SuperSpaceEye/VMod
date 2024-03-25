package net.spaceeye.vmod.utils

import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipTransform

fun posShipToWorld(ship: Ship?, pos: Vector3d, transform: ShipTransform? = null): Vector3d {
    val transform = transform ?: ship!!.transform
    val scale = Vector3d(transform.shipToWorldScaling)
    val ship_wp = Vector3d(transform.positionInWorld)
    val ship_sp = Vector3d(transform.positionInShip)
    return Vector3d((transform.transformDirectionNoScalingFromShipToWorld(
        ((pos - ship_sp)*scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_wp
}

fun posWorldToShip(ship: Ship?, pos: Vector3d, transform: ShipTransform? = null): Vector3d {
    val transform = transform ?: ship!!.transform
    val scale = Vector3d(transform.shipToWorldScaling)
    val ship_wp = Vector3d(transform.positionInWorld)
    val ship_sp = Vector3d(transform.positionInShip)
    return Vector3d((transform.transformDirectionNoScalingFromWorldToShip(
            ((pos - ship_wp) / scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_sp
}

fun posShipToWorldRender(ship: ClientShip?, pos: Vector3d, transform: ShipTransform? = null): Vector3d {
    val transform = transform ?: ship!!.renderTransform
    val scale = Vector3d(transform.shipToWorldScaling)
    val ship_wp = Vector3d(transform.positionInWorld)
    val ship_sp = Vector3d(transform.positionInShip)
    return Vector3d((transform.transformDirectionNoScalingFromShipToWorld(
        ((pos - ship_sp)*scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_wp
}

fun posWorldToShipRender(ship: ClientShip?, pos: Vector3d, transform: ShipTransform? = null): Vector3d {
    val transform = transform ?: ship!!.renderTransform
    val scale = Vector3d(transform.shipToWorldScaling)
    val ship_wp = Vector3d(transform.positionInWorld)
    val ship_sp = Vector3d(transform.positionInShip)
    return Vector3d((transform.transformDirectionNoScalingFromWorldToShip(
        ((pos - ship_wp) / scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_sp
}

inline fun transformDirectionShipToWorld(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(dir.toJomlVector3d(), org.joml.Vector3d()))
inline fun transformDirectionWorldToShip(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.transformDirectionNoScalingFromWorldToShip(dir.toJomlVector3d(), org.joml.Vector3d()))

inline fun transformDirectionShipToWorldRender(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.transformDirectionNoScalingFromShipToWorld(dir.toJomlVector3d(), org.joml.Vector3d()))
inline fun transformDirectionWorldToShipRender(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.transformDirectionNoScalingFromWorldToShip(dir.toJomlVector3d(), org.joml.Vector3d()))