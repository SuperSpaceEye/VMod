mconstraint = managed constraint

## Constraints
1. ✅ Rework current constraints managing logic to support additional data
2. Make additional mconstraint "types"
    * Muscle
    * Motor
3. Expand mconstraints and rendering system to allow for mconstraints to be tied to blocks instead to whole ships. I guess i can mixin into server levelChunk and inject additional data + inject into setState to track when smth becomes air, and check if chunk has constraints and then also check if the block has any constraint connected to it, and if it did, then remove constraint and renderer (if it exists). For saving/loading inject into ChunkMap.

## Rendering
1. Expand rendering system to include additional types of rendering like
   * Timed - After time passes stop rendering
   * Simple Arbitrary - rendering not tied to ships. Have a dedicated channel that will just always render things, or render depending on the position. Of course, it can only be used for effects (ray from toolgun), and not more "permanent" (like ropes placed on the ground only)  
   * Chunk based Arbitrary
2. Add clientside rendering options (like for Rope renderer allow setting maximum number of segments)
3. Add effects for toolgun

## HUD Rendering
1. Make system for HUD rendering

## Sounds
1. Add sounds and shit

## Toolgun Menu
1. Make keybinds changeable
2. Expand menu with more GUI components (sliders, drop down options, etc)
3. Make menu look better
4. ✅ Use translatable components

## Custom Access Levels
1. Allow op users to change server settings of VSource from toolgun menu.
2. Add accessibility levels that op users can modify
3. Make special commands for op users
    * Clean every mconstraint of acc level.
    * Set limit for total mconstraints of acc level.
    * Some other commands idk

## Constraints
1. Add more constraints

## Weld Constraint
1. ✅ Change welding to lclick and make it not break blocks on lclick
2. Make normal weld on lclick and snapping welding on rclick
3. Add texture for weld rendering

## Rope Constraint
1. Add ability to change rendering modes
2. Add more setting options

## Forge
1. Fix elementa dependency on forge

## Config
1. Make config