package net.spaceeye.vmod

import net.spaceeye.vmod.config.*

//TODO revise
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
        val INTERNAL_DATA = InternalData()

        class ClientToolgunSettings: ConfigSubDirectory() {
            var MAX_RAYCAST_DISTANCE: Double by CDouble(100.0, "", Pair(1.0, Double.MAX_VALUE))

            val SCHEMATIC_PACKET_PART_SIZE: Int by CInt(30000, "Reload the game for change to take the effect.", Pair(512, 1000000))
        }

        class ClientRenderingSettings: ConfigSubDirectory() {
            val MAX_RENDERING_DISTANCE: Double by CDouble(200.0, "Max distance in blocks some renderers are able to render. Reload the game for change to take the effect.", Pair(1.0, Double.MAX_VALUE))
        }

        class InternalData: ConfigSubDirectory() {

        }
    }
    class Common: ConfigSubDirectory()
    class Server: ConfigSubDirectory() {
        val AUTOREMOVE_MASSLESS_SHIPS: Boolean by CBool(true, "MAY BREAK SOMETHING. if true, will automatically remove massless ships if they appear.")

        val SCALE_THRUSTERS_THRUST: Boolean by CBool(true)

        var DIMENSION_GRAVITY_VALUES: String by CString("", "DO NOT CHANGE")

        var CONSTRAINT_CREATION_ATTEMPTS: Int by CInt(12000, "The amount of ticks VMod will try to create constraint if it failed to create one. Why is it needed? VS lags sometimes and fails to create constraints, idk why.", 20 to Int.MAX_VALUE)

        val PHYSGUN = ServerPhysgunSettings()
        val TOOLGUN = ServerToolgunSettings()
        val PERMISSIONS = Permissions()
        val SCHEMATICS = Schematics()

        class ServerPhysgunSettings(): ConfigSubDirectory() {
            val GRAB_ALL_CONNECTED_SHIPS: Boolean by CBool(false, "A bit buggy.")
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
            var VMOD_COMMANDS_PERMISSION_LEVEL: Int by CInt(2, "No Comment", Pair(0, 4))
            var VMOD_OP_COMMANDS_PERMISSION_LEVEL: Int by CInt(4, "No Comment", Pair(0, 4))
            var VMOD_TOOLGUN_PERMISSION_LEVEL: Int by CInt(2, "No Comment", Pair(0, 4))
            var VMOD_CHANGING_SERVER_SETTINGS_LEVEL: Int by CInt(4, "No Comment", Pair(0, 4))

            //TODO using config to store shit
            var ALWAYS_ALLOWED: String by CString("", "DO NOT CHANGE")
            var ALWAYS_DISALLOWED: String by CString("", "DO NOT CHANGE")
        }

        class Schematics: ConfigSubDirectory() {
            var TIMEOUT_TIME: Int by CInt(50, "No Comment", Pair(0, 20000))
        }
    }
}