package net.spaceeye.vmod.utils.vs

import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipTransform

fun posShipToWorld(ship: Ship?, pos: Vector3d, transform: BodyTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.transform).toWorld.transformPosition(pos.toJomlVector3d())) }
fun posWorldToShip(ship: Ship?, pos: Vector3d, transform: BodyTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.transform).toModel.transformPosition(pos.toJomlVector3d())) }

fun posShipToWorldRender(ship: ClientShip?, pos: Vector3d, transform: BodyTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.renderTransform).toWorld.transformPosition(pos.toJomlVector3d())) }
fun posWorldToShipRender(ship: ClientShip?, pos: Vector3d, transform: BodyTransform? = null): Vector3d { return Vector3d((transform ?: ship!!.renderTransform).toModel.transformPosition(pos.toJomlVector3d())) }

fun transformDirectionShipToWorld(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.shipToWorld.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))
fun transformDirectionWorldToShip(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.worldToShip.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))

fun transformDirectionShipToWorld(transform: ShipTransform, dir: Vector3d): Vector3d = Vector3d(transform.shipToWorld.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))
fun transformDirectionWorldToShip(transform: ShipTransform, dir: Vector3d): Vector3d = Vector3d(transform.worldToShip.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))

fun transformDirectionShipToWorldRender(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.shipToWorld.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))
fun transformDirectionWorldToShipRender(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.worldToShip.transformDirection(dir.toJomlVector3d(), org.joml.Vector3d()))

fun transformDirectionShipToWorldNoScaling(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.rotation.transform(dir.toJomlVector3d(), org.joml.Vector3d()))
fun transformDirectionWorldToShipNoScaling(ship: Ship, dir: Vector3d): Vector3d = Vector3d(ship.transform.rotation.transformInverse(dir.toJomlVector3d(), org.joml.Vector3d()))

fun transformDirectionShipToWorldRenderNoScaling(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.rotation.transform(dir.toJomlVector3d(), org.joml.Vector3d()))
fun transformDirectionWorldToShipRenderNoScaling(ship: ClientShip, dir: Vector3d): Vector3d = Vector3d(ship.renderTransform.rotation.transformInverse(dir.toJomlVector3d(), org.joml.Vector3d()))