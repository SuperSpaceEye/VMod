## Rendering
* Add clientside rendering options (like for Rope renderer allow setting maximum number of segments)
* Add clientside validation 
* Add effects for toolgun
* Group rendering objects by types to elevate common operations and add shared data between rendering types
* "Double rendering" so that if the "main" ship is out of rendering distance, it will still render correctly
* Fix rendering when i modify rendering transform of ships and player stands on the ship (VS2 moves camera when player stands on the ship) 

## General Code Structure
* Extract more code from Placement Assist for an easier use
* Extract more common code (weld/axis/hydraulics)

## Ticking Constraints
* Redo them

## Synchronised Data
* Redo everything

## HUD Rendering
* Make system for HUD rendering
* Use it to make clearer to player that they are in the middle of making a constraint, etc.

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
* Add accessibility levels that op users can modify
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
* Copy ship attachments (maybe, idk if i really need to do it)

## Rope Constraint
* Add ability to change rendering modes
* Add more setting options

## Hydraulics
* Make it more stable
* Fix the bug that crashes VS

## Elastic
A rope that can stretch or push (if option is chosen)

## Motor Constraint

## Pulley
A rope with connections to the ground through which it like moves. No idea on how to make it.

## Slider

## Winch
A rope that can be made longer or shorter

## Wheels
A tool that will create wheels from VS phys entities + maybe 