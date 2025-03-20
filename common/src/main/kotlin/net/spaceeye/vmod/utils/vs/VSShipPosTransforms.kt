package net.spaceeye.vmod.utils.vs

import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship

inline fun posShipToWorld(ship: Ship?, pos: Vector3d, transform: BodyTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.transform).toWorld.transformPosition(pos.toJomlVector3d())) }
inline fun posWorldToShip(ship: Ship?, pos: Vector3d, transform: BodyTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.transform).toModel.transformPosition(pos.toJomlVector3d())) }

inline fun posShipToWorldRender(ship: ClientShip?, pos: Vector3d, transform: BodyTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.renderTransform).toWorld.transformPosition(pos.toJomlVector3d())) }
inline fun posWorldToShipRender(ship: ClientShip?, pos: Vector3d, transform: BodyTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.renderTransform).toModel.transformPosition(pos.toJomlVector3d())) }

inline fun transformDirectionShipToWorld(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.shipToWorld.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))
inline fun transformDirectionWorldToShip(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.worldToShip.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))

inline fun transformDirectionShipToWorldRender(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.shipToWorld.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))
inline fun transformDirectionWorldToShipRender(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.worldToShip.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))

inline fun transformDirectionShipToWorldNoScaling(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.rotation.transform(dir.toJomlVector3d(), org.joml.Vector3d()))
inline fun transformDirectionWorldToShipNoScaling(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.rotation.transformInverse(dir.toJomlVector3d(), org.joml.Vector3d()))

inline fun transformDirectionShipToWorldRenderNoScaling(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.rotation.transform(dir.toJomlVector3d(), org.joml.Vector3d()))
inline fun transformDirectionWorldToShipRenderNoScaling(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.rotation.transformInverse(dir.toJomlVector3d(), org.joml.Vector3d()))