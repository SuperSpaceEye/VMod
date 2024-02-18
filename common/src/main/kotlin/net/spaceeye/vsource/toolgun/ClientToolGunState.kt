package net.spaceeye.vsource.toolgun

import net.minecraft.client.Minecraft
import net.spaceeye.vsource.toolgun.modes.BaseMode
import net.spaceeye.vsource.toolgun.modes.WeldMode
import net.spaceeye.vsource.utils.ClientClosable
import org.lwjgl.glfw.GLFW

object ClientToolGunState : ClientClosable() {
    val guiOpenKey = GLFW.GLFW_KEY_E

    val components = listOf<BaseMode>(WeldMode())

    lateinit var gui: ToolgunGUI

    fun guiIsOpened() = Minecraft.getInstance().screen == gui

    fun init() {
        gui = ToolgunGUI()
        gui.makeScrollComponents(components)
    }

    override fun close() {
    }
}