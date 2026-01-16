# VERY IMPORTANT

* Rework permission level and player roles (not sure what i meant by that)
* I don't think ventities will work correctly after being teleported to another dimension

* Rework "Disable Collisions"
* Move PlayerAccessManager in ServerToolgunInstance

# Maybe

* Mode to change COM (set mass of all blocks to 0 and set mass of target block to total mass)
* Mode to disable collisions for certain blocks (will need to mixin into shipAABB cuz blocks without collisions don't contribute to) (set type to type of air)
* Think of smth to use blockless ships that have collision (set type to smth that is not air)

* Add static mode to physgun
* Add config menu to physgun for finetuning

# Do Eventually

* Redo GIFTexture
* Remove usage of client transform providers with custom renderers
* Clean up physgun code
* Add snapping to physgun rotation

* Make vmod stuff work with phys entities (making constraints, grabbing them, etc)
* Add a way for addons to add their own client/server settings
* Finish Patchouli guide book

* Custom drag

# Gravity Changer
* rework saving/loading

# Connection Mode
* when smth like "onMassChange" exists, create another mode for stiffness, damping, and bounce threshold that makes values depend on mass of the first ship or smth, so that instead of absolute values (like 3000 N of force) it's Nx ship mass (0.5 * 100kg)

## Rendering
* Add effects for toolgun
* Group rendering objects by types to elevate common operations and add shared data between rendering types
* Fix rendering when i modify rendering transform of ships and player stands on the ship (VS2 moves camera when player stands on the ship) 
* Redo how normal renderers and block renderers work

## Constraint disabler
* An extension that will enable/disable it when signal is given
* Maybe also a variant that removes them?

## Add a "wire" thing maybe as an addon to vmod

## General Code Structure
* Create "AutoGUIBuilder"

## Ticking Constraints
* Redo them

## Sounds
* Add sounds and shit

## Toolgun Architecture 
* Make "do" action and its "inverse", so that you can not only undo
* Add "redo" after first one

## Toolgun Menu
* Expand menu with more GUI components (sliders, drop down options, etc)
* Make menu look better

## Motor Constraint
* Code below kinda works, but idk how to get angle from initial rotation, and it's very buggy (might get better on physx)
```kotlin
val fRot1 = Quaterniond(AxisAngle4d(Math.toRadians(angle), sDir1.toJomlVector3d())).mul(getHingeRotation(sDir1))
val fRot2 = getHingeRotation(sDir2)
VSFixedOrientationConstraint(shipId1, shipId2, compliance, fRot1, fRot2, maxForce)
```

## Pulley
A rope with connections to the ground through which it like moves. No idea on how to make it.

## Winch
A rope that can be made longer or shorter

## Wheels
A tool that will create wheels from VS phys entities + maybe 