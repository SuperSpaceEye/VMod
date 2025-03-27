# VERY IMPORTANT

* Rework permission level and player roles

# Maybe

* Rework schems?

* Mode to change COM (set mass of all blocks to 0 and set mass of target block to total mass)
* Mode to disable collisions for certain blocks (will need to mixin into shipAABB cuz blocks without collisions don't contribute to) (set type to type of air)
* Think of smth to use blockless ships that have collision (set type to smth that is not air)

# Do Eventually

# Connection Mode
* when smth like "onMassChange" exists, create another mode for stiffness, damping, and bounce threshold that makes values depend on mass of the first ship or smth, so that instead of absolute values (like 3000 N of force) it's Nx ship mass (0.5 * 100kg)

## Rendering
* Add effects for toolgun
* Group rendering objects by types to elevate common operations and add shared data between rendering types
* Fix rendering when i modify rendering transform of ships and player stands on the ship (VS2 moves camera when player stands on the ship) 
* Redo how normal renderers and block renderers work
* Redo ConeBlockRenderer into a more general version

## Constraint disabler
* A "wrapper" MConstraint that will "wrap" around MConstraint and enable/disable it when signal is given

## Toolgun settings presets menu
* maybe when pressing alt or smth it will show all presets for this mode, and allow player to cycle them

## Add a "wire" thing maybe as an addon to vmod

## General Code Structure
* Create "AutoGUIBuilder"

## Ticking Constraints
* Redo them

## HUD Rendering
* Add info window for like part weight, total weight, connections, etc

## Sounds
* Add sounds and shit

## Toolgun Architecture 
* Make "do" action and its "inverse", so that you can not only undo
* Add "redo" after first one

## Toolgun Menu
* Expand menu with more GUI components (sliders, drop down options, etc)
* Make menu look better

## Custom Access Levels
* Make special commands for op users
    * Clean every mconstraint of acc level.
    * Set limit for total mconstraints of acc level.
    * Some other commands idk

## Schematic
* Right now schematic logic assumes default mc world height. Make it not do that

## Motor Constraint

## Pulley
A rope with connections to the ground through which it like moves. No idea on how to make it.

## Winch
A rope that can be made longer or shorter

## Wheels
A tool that will create wheels from VS phys entities + maybe 