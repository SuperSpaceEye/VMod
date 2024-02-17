package net.spaceeye.vsource.toolgun

import net.minecraft.client.Minecraft
import net.spaceeye.vsource.gui.ExampleGui
import net.spaceeye.vsource.gui.ToolGunGUI
import net.spaceeye.vsource.utils.ClientClosable
import org.lwjgl.glfw.GLFW

object ClientToolGunState : ClientClosable() {
    val guiOpenKey = GLFW.GLFW_KEY_E

    lateinit var gui: ExampleGui

    fun guiIsOpened() = Minecraft.getInstance().screen == gui

    fun init() {
        gui = ExampleGui()
    }

    override fun close() {
    }
}