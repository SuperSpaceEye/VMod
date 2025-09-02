{.e_name "Various Parameters"}
{.e_sortnum 0}
# Visual Parameters

Player can change visual parameters per VEntity, however each player can set an upper/lower limit on each parameter for themselves

* **Width** - Determines visual width of a constraint
* **Fullbright** - Determines whenever constraint will always be bright, or use brightness values from the world
* **Segments** - Amount of visual segments thing will have (Rope)
* **Use Tube Renderer** - If enabled, will use tube renderer (Rope). Best visual result will be if you set Hitpos to "Centered In Block" and disable "Allow Twisting". All other combinations are kinda ugly
* * **Allow Twisting** - If enabled, will lerp rotation for each segment. If disabled and hit pos is not "Centered In Block", one segment can twist pretty ugly
* **Texture Options** - Advanced texture options
* * **Length UV Start** - Start pos of texture for length
* * **Length UV Step Multiplier** - Amount of texture lengths renderer will loop per unit length
* * **Width UV Start** - Start pos of texture for width
* * **Width UV Multiplier** - Amount of texture lengths renderer will loop per unit width
* **RGBA** - Controls color

# IDK Parameters

* **Max Force** - Determines how much force a constraint can apply before giving up and stopping working. <0 for max
* **Stiffness** - Determines how stiff a constraint is. <0 for max
* **Fixed Distance** - If <0, then calculates distance between points itself, if >=0, then uses given distance instead.
* **Hitpos Modes** - If constraint needs positions, this option will appear
* * **Normal** - Will use hit position as is
* * **Centered On Side** - Will get the face of a block hit position is on, and use center of that face as position.
* * **Centered In Block** - Will get the block hit position is on, and use center of the block as position
* * **Precise Placement** - Will get the face of a block hit position is on, create n^n points (determined by Precise Placement Assist Sides) uniformly distributed across that face, and use closest point to hit position (will light up green)
* **Placement Assist Scroll Step** - Determines how big scroll step is of rotation in Placement Assist. In degrees
* **Distance From Block** - Determines how far away object will be after finishing Placement Assist
