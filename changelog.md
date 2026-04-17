# 1.9.1
* Probably broke something
* Fixed misspelling in command /vmod schem save-to-server
* Removed VMOD_COMMANDS_PERMISSION_LEVEL
* Default role now can't do anything with toolgun or commands (unless old config exists)
* VSchematic now doesn't load block entities or containers by default (unless old config exists)

# 1.9.0
* Works on 2.4 VS
* Phys Rope, Slider, Sync Rotation are disabled
* Dimensional Gravity is removed, use VS datapacks
* Progress on patchouli guide book

# 1.8.0
* Added function "/vmod op prune-shipyard-chunks" to delete chunks of deleted ships
* Added automatic region cleanup on ship deletion
* Added more config options for schematics
* Added WIP guide book
* Removed "Original Weight" from info window
* Improved logic of 2 "Hitpos Modes" (Centered On Side, Precise Placement) and "Placement Assist"
* Changed defaults of ALLOW_CHUNK_PLACEMENT_INTERRUPTION and ALLOW_CHUNK_UPDATE_INTERRUPTION to false (you should probably change them to false too in the server config)
* Sensor can now be placed in world and not just ships
* Fixed "/vmod op clear-vmod-attachments" not working
* Fixed Custom Mass not working correctly
* Fixed VEntityChanger crashing sometimes
* Fixed "MMB - join mode" being always visible in "Connection" and "Hydraulics" modes when it can only be used with "Bearing" mode chosen

# 1.7.1
* Fixed bug with "Open or Close Toolgun GUI" keybind stopping working sometimes
* Fixed ~~maybe~~ some issues with schematics
* Fixed bug where ServerLimits reset to default
* Fixed bug where custom per-ship gravity weren't saving

# 1.7.0
* "Open or Close Toolgun GUI" keybind is now ignored while writing in the parameter space
* New config values for schematic control
* Added experimental ship highlighting (doesn't work with sodium)
* Added texture options for rope and phys rope
* Fixed inconsistent texture width and length in ropes
* Fixed HUD not hiding
* Fixed VMod sometimes incorrectly thinking that two ships are connected when they are actually not 
* Internal changes and bugfixes

# 1.6.1
* Fixed precise placement assist side num not updating after changing preset
* Fixed game crashing on pressing delete in Settings Preset menu without choosing preset
* Made GravChangerMode, MassChangerMode, PhysRopeMode, ScaleMode, SensorMode, SliderMode, ThrusterMode, presettable
* Fixed VMod not working on servers 

# 1.6.0
* Added delete button to VEntityChanger
* Added Settings Presets
* Fixed rare crash when raycasting
* Rolled back schematic renderer change cuz it didn't work
* Fixed phys bearing schem compat not working
* Fixed possible crash when changing color in VEntity Changer

# 1.5.1
* Fixed incompat with control craft
* Changed schematic renderer
# 1.5.0
* Added a way to open client/server setting via commands ("/vmod-client open-client-settings", "/vmod-client open-server-settings")
* Added HUD Info Window if you hold toolgun and press "I"
* Fixed issue when placing schematic multiple times
* Fixed create blocks not updating correctly upon schematic placement
* Fixed constrains not correctly deserializing