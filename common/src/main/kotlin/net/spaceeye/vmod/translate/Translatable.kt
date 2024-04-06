package net.spaceeye.vmod.translate

import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component

private inline fun makeComponent(s: String) = Component.translatable(s)
inline fun Component.get(): String = I18n.get(this.string)

object GUIComponents {
    private const val path = "vmod.gui."
    private inline fun t(s: String) = makeComponent(path+s)
    private inline fun s(s: String) = makeComponent(path+"setting."+s)

    val WELD = t("weld")
    val ROPE = t("rope")
    val HYDRAULICS = t("hydraulics")
    val AXIS = t("axis")
    val SCALE = t("scale")
    val STRIP = t("strip")

    val COMPLIANCE = s("compliance")
    val MAX_FORCE = s("max_force")
    val FIXED_DISTANCE = s("fixed_distance")
    val WIDTH = s("width")
    val SEGMENTS = s("segments")
    val EXTENSION_SPEED = s("extension_speed")
    val EXTENSION_DISTANCE = s("extension_distance")
    val CHANNEL = s("channel")
    val FUNCTION = s("function")
    val ACTIVATE = s("activate")
    val DEACTIVATE = s("deactivate")
    val APPLY_CHANGES = s("apply_changes")
    val REMOVED = s("removed")
    val DISABLE_COLLISIONS = s("disable_collisions")
    val DISTANCE_FROM_BLOCK = s("distance_from_block")
    val RADIUS = s("radius")

    val HITPOS_MODES = s("hitpos_modes")
    val NORMAL = s("normal")
    val CENTERED_ON_SIDE = s("centered_on_side")
    val CENTERED_IN_BLOCK = s("centered_in_block")

    val STRIP_MODES = s("strip_modes")
    val STRIP_ALL = s("strip_all")
    val STRIP_IN_RADIUS = s("strip_in_radius")
}