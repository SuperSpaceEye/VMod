package net.spaceeye.vmod.translate

import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.ELOG

val registeredComponents = mutableListOf<MyTranslatableComponent>()

data class MyTranslatableComponent(val enTranslation: String, val key: String) {
    init {
        registeredComponents.add(this)
    }

    fun asMC() = TranslatableComponent(key)
}

private fun makeComponent(default: String, key: String) = MyTranslatableComponent(default, key).asMC()
fun TranslatableComponent.get(): String = I18n.get(this.key)
fun TranslatableComponent.getTranslationKey(): String = this.key
fun String.translate(): String = I18n.get(this)
fun Component.get(): String = if (this is TranslatableComponent) {this.get()} else { ELOG(".get() WAS CALLED ON A NOT TRANSLATABLE COMPONENT");"Not a translatable component."}
fun makeFake(s: String) = TranslatableComponent(s)

private const val path = "vmod.gui."
private fun t(default: String) = makeComponent(default, path                  +default.lowercase().replace(" ", "_").replace("'", ""))
private fun s(default: String) = makeComponent(default, path+"setting."       +default.lowercase().replace(" ", "_").replace("'", ""))
private fun x(default: String) = makeComponent(default, path+"text."          +default.lowercase().replace(" ", "_").replace("'", ""))
private fun v(default: String) = makeComponent(default, path+"server_setting."+default.lowercase().replace(" ", "_").replace("'", ""))
private fun a(default: String) = makeComponent(default, path+"tabs."          +default.lowercase().replace(" ", "_").replace("'", ""))
private fun p(default: String) = makeComponent(default, path+"popup."         +default.lowercase().replace(" ", "_").replace("'", ""))

private fun t(default: String, key: String) = makeComponent(default, path+key)
private fun s(default: String, key: String) = makeComponent(default, path+"setting."+key)
private fun x(default: String, key: String) = makeComponent(default, path+"text."+key)
private fun v(default: String, key: String) = makeComponent(default, path+"server_setting."+key)
private fun a(default: String, key: String) = makeComponent(default, path+"tabs."+key)


val CONNECTION = t("Connection")
val ROPE = t("Rope")
val HYDRAULICS = t("Hydraulics")
val PHYS_ROPE = t("Physics Rope", "phys_rope")
val SLIDER = t("Slider")
val GRAVITY_CHANGER = t("Gravity Changer")
val DISABLE_COLLISIONS = t("Disable Collisions")
val SCHEMATIC = t("Schematic")
val SCALE = t("Scale")
val STRIP = t("Strip")
val SYNC_ROTATION = t("Sync Rotation")
val GEAR = t("Gear")
val THRUSTER = t("Thruster")
val SENSOR = t("Sensor")
val SHIP_REMOVER = t("Ship Remover")
val MASS_CHANGER = t("Mass Changer")
val COM_CHANGER = t("COM Changer")
val VENTITY_CHANGER = t("VEntity Changer")

val DIMENSIONAL_GRAVITY = v("Dimensional Gravity")
val PLAYER_ROLE_MANAGER = v("Player Role Manager")
val ROLES_SETTINGS = v("Roles Settings")
val SERVER_LIMITS = v("Sever Limits")
val CLIENT_LIMITS = v("Client Limits")

val MAIN = a("Main")
val CLIENT_SETTINGS = a("Client Settings")
val SERVER_SETTINGS = a("Server Settings")
val SETTING_PRESETS = a("Setting Presets")

val MAX_FORCE = s("Max Force")
val FIXED_DISTANCE = s("Fixed Distance")
val WIDTH = s("Width")
val SEGMENTS = s("Segments")
val EXTENSION_SPEED = s("Extension Speed")
val EXTENSION_DISTANCE = s("Extension Distance")
val CHANNEL = s("Channel")
val APPLY_CHANGES = s("Apply Changes")
val REMOVED = s("Removed")
val DISTANCE_FROM_BLOCK = s("Distance From Block")
val RADIUS = s("Radius")
val TOTAL_MASS = s("Total Mass")
val STIFFNESS = s("Stiffness")
val DAMPING = s("Damping")
val SSCALE = s("Scale")
val FORCE = s("Force")
val GEAR_RATIO = s("Gear Ratio")
val MAX_DISTANCE = s("Max Distance")
val IGNORE_SELF_SHIP = s("Ignore Self Ship")
val TRANSMIT = s("Transmit")
val NEW_MASS = s("New Mass")
val PERSISTENT = s("Persistent")
val ANGLE_LIMIT = s("Angle Limit")
val SIDES = s("Sides")
val FULLBRIGHT = s("Fullbright")
val TRANSPARENCY = s("Transparency")

val PLACEMENT_ASSIST_SCROLL_STEP = s("Placement Assist Scroll Step")

val HITPOS_MODES = s("Hitpos Modes")
val NORMAL = s("Normal")
val CENTERED_ON_SIDE = s("Centered On Side")
val CENTERED_IN_BLOCK = s("Centered In Block")

val PRECISE_PLACEMENT_ASSIST_SIDES = s("Precise Placement Assist Sides")
val PRECISE_PLACEMENT = s("Precise Placement")

val STRIP_MODES = s("Strip Modes")
val STRIP_ALL = s("Strip All")
val STRIP_IN_RADIUS = s("Strip In Radius")

val CONNECTION_MODES = s("Connection Modes")
val FIXED_ORIENTATION = s("Fixed Orientation")
val HINGE_ORIENTATION = s("Hinge Orientation")
val FREE_ORIENTATION = s("Free Orientation")

val SCALING_MODE = s("Scaling Mode")
val SCALE_ALL_CONNECTED = s("Scale All Connected")
val SCALE_SINGLE_SHIP = s("Scale Single Ship")

val SAVE = s("Save")
val CANCEL = s("Cancel")
val FILENAME = s("Filename")
val LOAD = s("Load")

val X_GRAVITY = s("X Gravity")
val Y_GRAVITY = s("Y Gravity")
val Z_GRAVITY = s("Z Gravity")

val SAMPLING_MODES = s("Sampling Modes")

val INDIVIDUAL = s("Individual")
val ALL_CONNECTED = s("All Connected")
val ALL_CONNECTED_AND_TOUCHING = s("All Connected And Touching")

val PRESS_R_TO_RESET_STATE = p("Press R to reset state")
val DIMENSIONAL_GRAVITY_UPDATE_WAS_REJECTED = p("Dimensional Gravity update was rejected")

val COMMON_HUD_1 = x("LMB - main mode, RMB - secondary mode, MMB - join mode", "common_hud_1")
val COMMON_HUD_2 = x("LMB on the other ship or the ground", "common_hud_2")
val COMMON_HUD_3 = x("Look at the target and press RMB again", "common_hud_3")
val COMMON_HUD_4 = x("Configure rotation with mouse wheel, press RMB to confirm", "common_hud_4")
val COMMON_HUD_5 = x("LMB - main mode", "common_hud_5")

val DISABLE_COLLISIONS_HUD_1 = x("LMB to disable collisions, RMB to enable all collisions", "disable_collisions_hud_1")
val SCALE_HUD_1 = x("LMB - scale ship/s", "scale_hud_1")

val SCHEM_HUD_1 = x("LMB - create schematic from ship", "schem_hud_1")
val SCHEM_HUD_2 = x("LMB - save ship as schematic\nRMB - paste schematic\nmouse wheel - rotate schematic", "schem_hud_2")

val STRIP_HUD_1 = x("LMB - strip constraints", "strip_hud_1")

val SLIDER_HUD_1 = x("LMB to make first ship point", "slider_hud_1")
val SLIDER_HUD_2 = x("LMB to make second ship point (should be the same ship)", "slider_hud_2")
val SLIDER_HUD_3 = x("LMB to make first axis point (should be a different ship or a ground)", "slider_hud_3")
val SLIDER_HUD_4 = x("LMB to make second axis point (should be the same ship or a ground)", "slider_hud_4")

val GRAV_CHANGER_HUD_1 = x("LMB - set gravity. RMB - reset gravity", "grav_changer_hud_1")

val SYNC_ROTATION_HUD_1 = x("LMB to select first", "sync_rotation_hud_1")
val SYNC_ROTATION_HUD_2 = x("LMB to select second ship", "sync_rotation_hud_2")

val VENTITY_CHANGER_HUD_1 = x("LMB - Select ship for viewing", "ventity_changer_hud_1")
val VENTITY_CHANGER_HUD_2 = x("LMB - Select ship for viewing or ground to reset\nMouse Scroll - Choose VEntity to change\nRMB - Confirm selection", "ventity_changer_hud_2")

val GEAR_HUD_1 = x("LMB on the other ship", "gear_hud_1")
val MASS_CHANGER_HUD_1 = x("LMB - change weight, RMB - reset to default", "mass_changer_hud_1")
val COM_CHANGER_HUD_1 = x("LMB - concentrate mass to block, RMB - reset", "com_changer_hud_1")
val THRUSTER_HUD_1 = x("LMB - create thruster", "thruster_hud_1")
val SENSOR_HUD_1 = x("LMB - create sensor", "sensor_hud_1")
val SHIP_REMOVER_HUD_1 = x("LMB - remove ship", "ship_remover_hud_1")

val YOU_DONT_HAVE_ACCESS_TO_THIS = x("You don't have access to this")
val APPLY_NEW_GRAVITY_SETTINGS = x("Apply new Gravity Settings")
val LEVELS = x("Levels")
val ROLES = x("Roles")
val APPLY_NEW_SERVER_LIMITS = x("Apply new Server Limits")
val APPLY_NEW_CLIENT_LIMITS = x("Apply new Client Limits")
val APPLY_NEW_ROLE_PERMISSIONS = x("Apply new Role Permissions")
val ENABLE_ALL = x("Enable All")
val DISABLE_ALL = x("Disable All")
val NEW_ROLE = x("New Role")
val REMOVE = x("Remove")
val OK = x("Ok")
val ROLE_NAME = x("Role Name")
val DEFAULT_HUD_GUI = x("Press ==GUI_MENU_OPEN_OR_CLOSE== to open or close GUI", "default_hud_gui")

val YOU_DONT_HAVE_PERMISSION_TO_USE_TOOLGUN = x("You don't have the permission to use toolgun")
val TOOLGUN_MODE_ACTIVATION_HAS_FAILED = x("Toolgun mode activation has failed")
val SERVER_LIMITS_UPDATE_WAS_REJECTED = x("Server Limits update was rejected")
val RENDERING_HAS_THROWN_AN_EXCEPTION = x("Rendering has thrown an exception")
val SCHEMATIC_HAD_ERROR_DURING_PLACING = x("Schematic had error during placing")
val SCHEMATIC_HAD_ERROR_DURING_COPYING = x("Schematic had error during copying")
val ONE_OF_THE_SHIPS_IS_TOO_TALL = x("One of the ships in the schematic is too tall for this dimension. Schematic cannot be placed", "one_of_the_ships_is_too_tall")
val COULDNT_LOAD_VMODSCHEM_V1 = x("VMod can't load this schematic. Install VMod 0.1.3 (or 1.0.0 if that's when schematic was created), place schematic, run \\\"/vmod op clear-vmod-attachments\\\", immediately leave, load mc with newest VMod, and make new schematic.", "couldnt_load_vmodschem_v1")