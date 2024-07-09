package net.spaceeye.vmod.utils.vs

import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipTransform

inline fun posShipToWorld(ship: Ship?, pos: Vector3d, transform: ShipTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.transform).shipToWorld.transformPosition(pos.toJomlVector3d())) }
inline fun posWorldToShip(ship: Ship?, pos: Vector3d, transform: ShipTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.transform).worldToShip.transformPosition(pos.toJomlVector3d())) }

inline fun posShipToWorldRender(ship: ClientShip?, pos: Vector3d, transform: ShipTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.renderTransform).shipToWorld.transformPosition(pos.toJomlVector3d())) }
inline fun posWorldToShipRender(ship: ClientShip?, pos: Vector3d, transform: ShipTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.renderTransform).worldToShip.transformPosition(pos.toJomlVector3d())) }

inline fun transformDirectionShipToWorld(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(dir.toJomlVector3d(), org.joml.Vector3d()))
inline fun transformDirectionWorldToShip(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.transformDirectionNoScalingFromWorldToShip(dir.toJomlVector3d(), org.joml.Vector3d()))

inline fun transformDirectionShipToWorldRender(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.transformDirectionNoScalingFromShipToWorld(dir.toJomlVector3d(), org.joml.Vector3d()))
inline fun transformDirectionWorldToShipRender(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.transformDirectionNoScalingFromWorldToShip(dir.toJomlVector3d(), org.joml.Vector3d()))