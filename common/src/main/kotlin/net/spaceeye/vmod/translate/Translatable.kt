package net.spaceeye.vmod.translate

import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component

val registeredComponents = mutableListOf<MyTranslatableComponent>()

data class MyTranslatableComponent(val enTranslation: String, val key: String) {
    init {
        registeredComponents.add(this)
    }

    fun asMC() = Component.translatable(key)
}

private inline fun makeComponent(default: String, key: String) = MyTranslatableComponent(default, key).asMC()
inline fun Component.get(): String = I18n.get(this.string)
inline fun Component.getTranslationKey(): String = this.string
inline fun String.translate(): String = I18n.get(this)
inline fun makeFake(s: String) = Component.translatable(s)

private const val path = "vmod.gui."
private inline fun t(default: String) = makeComponent(default, path                  +default.lowercase().replace(" ", "_").replace("'", ""))
private inline fun s(default: String) = makeComponent(default, path+"setting."       +default.lowercase().replace(" ", "_").replace("'", ""))
private inline fun x(default: String) = makeComponent(default, path+"text."          +default.lowercase().replace(" ", "_").replace("'", ""))
private inline fun v(default: String) = makeComponent(default, path+"server_setting."+default.lowercase().replace(" ", "_").replace("'", ""))
private inline fun a(default: String) = makeComponent(default, path+"tabs."          +default.lowercase().replace(" ", "_").replace("'", ""))
private inline fun p(default: String) = makeComponent(default, path+"popup."         +default.lowercase().replace(" ", "_").replace("'", ""))

private inline fun t(default: String, key: String) = makeComponent(default, path+key)
private inline fun s(default: String, key: String) = makeComponent(default, path+"setting."+key)
private inline fun x(default: String, key: String) = makeComponent(default, path+"text."+key)
private inline fun v(default: String, key: String) = makeComponent(default, path+"server_setting."+key)
private inline fun a(default: String, key: String) = makeComponent(default, path+"tabs."+key)


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
val COMMON_HUD_3 = x("look at the target and press LMB again", "common_hud_3")
val COMMON_HUD_4 = x("configure rotation with mouse wheel, press LMB to confirm", "common_hud_4")
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

val GEAR_HUD_1 = x("LMB on the other ship")

val MASS_CHANGER_HUD_1 = x("LMB - change weight, RMB - reset to default")

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
val SCHEMATIC_HAD_FATAL_ERROR_AND_COULDNT_BE_PLACED = x("Schematic had fatal error and couldn't be placed")
val SCHEMATIC_HAD_FATAL_ERROR_AND_COULDNT_BE_COPIED = x("Schematic had fatal error and couldn't be copied")
val SCHEMATIC_HAD_NONFATAL_ERRORS = x("Schematic had nonfatal errors")