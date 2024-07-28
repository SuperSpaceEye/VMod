# VERY IMPORTANT

nothing

# Do Eventually

## Rendering
* Add clientside rendering options (like for Rope renderer allow setting maximum number of segments)
* Add clientside validation 
* Add effects for toolgun
* Group rendering objects by types to elevate common operations and add shared data between rendering types
* "Double rendering" so that if the "main" ship is out of rendering distance, it will still render correctly
* Fix rendering when i modify rendering transform of ships and player stands on the ship (VS2 moves camera when player stands on the ship) 
* Redo how normal renderers and block renderers work

## Constraint disabler
* A "wrapper" MConstraint that will "wrap" around MConstraint and enable/disable it when signal is given

## Revise whole MConstraint structure
## Redo how MConstraint renderers are saved

## java.awt.Color doesn't exist in headless mode

## Redo ConstraintManager's tryMakeConstraint

## Toolgun settings presets menu

## Toolgun settings for shit like gravity etc

## Add a "wire" thing maybe as an addon to vmod

## General Code Structure
* Extract more code from Placement Assist for an easier use
* Extract more common code (connection/hydraulics)

## Ticking Constraints
* Redo them

## HUD Rendering
* Add info window for like part weight, total weight, connections, etc
* Add popup messages things

## Sounds
* Add sounds and shit

## Toolgun Architecture 
* Make "do" action and its "inverse", so that you can not only undo 

## Toolgun Modes
* Remake them again to use events for CRI events, reset, etc for an easier additions of PA, etc.

## Toolgun Menu
* Expand menu with more GUI components (sliders, drop down options, etc)
* Make menu look better

## Custom Access Levels
* Allow op users to change server settings of VMod from toolgun menu.
* Add different access levels
* Make special commands for op users
    * Clean every mconstraint of acc level.
    * Set limit for total mconstraints of acc level.
    * Some other commands idk

## Server side things
* Add server side validation for (kinda ✅) constraints and rendering
* Add the ability for server to change limits
* Add the ability for server and clients to synchronise limits

## Schematic
* Copy entities (maybe, idfk)
* Copy phys entities (not very important rn)

## Rope Constraint
* Add ability to change rendering modes
* Add more setting options

## Hydraulics
* Make it more stable
* Fix the bug that crashes VS
* Fix bad copying

## Elastic
A rope that can stretch or push (if option is chosen)

## Motor Constraint

## Pulley
A rope with connections to the ground through which it like moves. No idea on how to make it.

## Winch
A rope that can be made longer or shorter
Maybe have 2 ropes.
first rope will be "supporting rope" - it will have max compliance and force, but during retraction it will only shorten to diff between target and point
second rope will be "retracting rope" - it will have max force == needed force to counteract other forces + more, but idk how to calculate other forces

## Wheels
A tool that will create wheels from VS phys entities + maybe 