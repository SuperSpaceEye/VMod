package net.spaceeye.vsource.translate

import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.TranslatableComponent

private inline fun makeComponent(s: String) = TranslatableComponent(s)
inline fun TranslatableComponent.get(): String = I18n.get(this.key)

object GUIComponents {
    private const val path = "gui."
    private inline fun t(s: String) = makeComponent(path+s)
    private inline fun s(s: String) = makeComponent(path+"setting."+s)

    val WELD = t("weld")
    val ROPE = t("rope")
    val AABB_WELD = t("aabbweld")

    val COMPLIANCE = s("compliance")
    val MAX_FORCE = s("max_force")
    val FIXED_DISTANCE = s("fixed_distance")
}