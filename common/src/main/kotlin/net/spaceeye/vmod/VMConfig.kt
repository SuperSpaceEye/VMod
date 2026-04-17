package net.spaceeye.vmod

import net.spaceeye.vmod.config.*

object VMConfig {
    lateinit var server_config_holder: AbstractConfigBuilder
    lateinit var client_config_holder: AbstractConfigBuilder
    lateinit var common_config_holder: AbstractConfigBuilder

    val SERVER = Server()
    val CLIENT = Client()
    val COMMON = Common()

    class Client: ConfigSubDirectory() {
        val TOOLGUN = ClientToolgunSettings()
        val RENDERING = ClientRenderingSettings()

        val SHUT_UP: Boolean by CBool(false, "Disable patchouli notification")

        class ClientToolgunSettings: ConfigSubDirectory() {
            var MAX_RAYCAST_DISTANCE: Double by CDouble(100.0, "", Pair(1.0, Double.MAX_VALUE))

            val SCHEMATIC_PACKET_PART_SIZE: Int by CInt(30000, "Reload the game for change to take the effect.", Pair(512, 1000000))
        }

        class ClientRenderingSettings: ConfigSubDirectory() {
            val MAX_RENDERING_DISTANCE: Double by CDouble(200.0, "Max distance in blocks some renderers are able to render. Reload the game for change to take the effect.", Pair(1.0, Double.MAX_VALUE))
        }
    }
    class Common: ConfigSubDirectory()
    class Server: ConfigSubDirectory() {

        val SCALE_THRUSTERS_THRUST: Boolean by CBool(true)

        var CONSTRAINT_CREATION_ATTEMPTS: Int by CInt(12000, "The amount of ticks VMod will try to create constraint if it failed to create one. Why is it needed? VS lags sometimes and fails to create constraints, idk why.", 20 to Int.MAX_VALUE)

        val PHYSGUN = ServerPhysgunSettings()
        val TOOLGUN = ServerToolgunSettings()
        val PERMISSIONS = Permissions()
        val SCHEMATICS = Schematics()
        val SHIPYARD_PRUNER = ShipyardPruner()

        class ServerPhysgunSettings(): ConfigSubDirectory() {
            val GRAB_ALL_CONNECTED_SHIPS: Boolean by CBool(false, "A bit buggy.", false)
            val PCONST: Double   by CDouble(160.0, "a", 0.0 to Double.MAX_VALUE)
            val DCONST: Double   by CDouble(20.0 , "a", 0.0 to Double.MAX_VALUE)
            val IDKCONST: Double by CDouble(90.0 , "a", 0.0 to Double.MAX_VALUE)
        }

        class ServerToolgunSettings: ConfigSubDirectory() {
            val MAX_RAYCAST_DISTANCE: Double by CDouble(100.0, "No Comment", Pair(1.0, Double.MAX_VALUE))

            //TODO use this
            val MAX_SHIPS_ALLOWED_TO_COPY: Int by CInt(-1, "Number of connected ships a player can copy in one request. <=0 for unlimited.", do_show = false)

            val SCHEMATIC_PACKET_PART_SIZE: Int by CInt(30000, "Reload the game for change to take the effect.", Pair(512, 1000000))
        }

        class Permissions: ConfigSubDirectory() {
            var VMOD_OP_COMMANDS_PERMISSION_LEVEL: Int by CInt(4, "No Comment", Pair(0, 4))
            var VMOD_CHANGING_SERVER_SETTINGS_LEVEL: Int by CInt(4, "No Comment", Pair(0, 4))
        }

        class Schematics: ConfigSubDirectory() {
            var TIMEOUT_TIME: Int by CInt(50, "No Comment", Pair(0, Int.MAX_VALUE))

            var ALLOW_CHUNK_PLACEMENT_INTERRUPTION: Boolean by CBool(false, "Allows ships to be created over several ticks (so that if you have a huge ship it won't freeze server). May be incompatible with some mods though.")
            var ALLOW_CHUNK_UPDATE_INTERRUPTION: Boolean by CBool(false, "Allows ships to be updated over several ticks (so that if you have a huge ship it won't freeze server). May be incompatible with some mods though.")
            var LOAD_CONTAINERS: Boolean by CBool(false, "Determines whenever or not block entities implementing from Container (chests for example) will be loaded")
            var LOAD_ENTITIES: Boolean by CBool(false, "Determines whenever or not entities will be loaded")
            var BLACKLIST_MODE: Boolean by CBool(false, "If true, will load nbt of all block entities that are not in blacklist. If false, will only load nbt of whitelisted block entities")
            var BLACKLIST: String by CString("", "Write resource location of blocks separated by commas, like \"minecraft:chest, some_mod:idk\"")
            var WHITELIST: String by CString("", "Write resource location of blocks separated by commas, like \"minecraft:chest, some_mod:idk\"")
        }

        class ShipyardPruner: ConfigSubDirectory() {
            var CLEAR_SHIP_PLOT_ON_DELETION: Boolean by CBool(false, "Will automatically delete region files of deleted ships")
        }
    }
}