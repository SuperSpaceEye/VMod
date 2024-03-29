## Constraints
* Add scale tool that would scale all ships connected to constraint

## Rendering
* Add clientside rendering options (like for Rope renderer allow setting maximum number of segments)
* Add clientside validation 
* (kinda) ✅ Add effects for toolgun
* Group rendering objects by types to elevate common operations and add shared data between rendering types
* "Double rendering" so that if the "main" ship is out of rendering distance, it will still render correctly
* Fix rendering when i modify rendering transform of ships and player stands on the ship (VS2 moves camera when player stands on the ship) 

## HUD Rendering
* Make system for HUD rendering

## Sounds
* Add sounds and shit

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

## Commands
* Add custom command for teleporting of the whole structure
* Add custom command for scaling of the whole structure

## Server side things
* Add server side validation for (kinda ✅) constraints and rendering
* Add the ability for server to change limits
* Add the ability for server and clients to synchronise limits

## Weld Constraint
* Add texture for weld rendering

## Rope Constraint
* Add ability to change rendering modes
* Add more setting options

## Axis Constraint
* Figure out how VSHingeOrientationConstraint works and use it to make axis constraint more stable

## Hydraulics
* Add more options, like one to disable fixed orientation

## Ball Socket Constraint
Basically just one attachment constraint

## Elastic
A rope that can stretch or push (if option is chosen)

## Motor Constraint

## Pulley
A rope with connections to the ground through which it like moves. No idea on how to make it.
Ig i can use attachments to do something, but i'm really not sure.

## Slider

## Winch
A rope that can be made longer or shorter