package org.valkyrienskies.core.api.ships

import org.joml.Matrix3dc
import org.joml.Vector3dc
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl

val PhysShip.mass: Double get() = (this as PhysShipImpl).inertia.shipMass
val PhysShip.momentOfInertia: Matrix3dc get() = (this as PhysShipImpl).inertia.momentOfInertiaTensor
val PhysShip.velocity: Vector3dc get() = (this as PhysShipImpl).poseVel.vel
val PhysShip.omega: Vector3dc get() = (this as PhysShipImpl).poseVel.omega
