package net.spaceeye.vmod.utils

import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

//why? mc sometimes doesn't release cursor on breakpoint hit
//GLFW.glfwSetInputMode(Minecraft.getInstance().window.window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL); true
fun debuggerReleaseControl(): Boolean {
    GLFW.glfwSetInputMode(Minecraft.getInstance().window.window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);return true
}