package net.spaceeye.vmod.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.*
import gg.essential.elementa.constraints.*
import net.spaceeye.vmod.PlatformUtils
import net.minecraft.client.gui.GuiGraphics
import net.spaceeye.vmod.gui.additions.ErrorAddition
import net.spaceeye.vmod.gui.additions.HUDAddition
import net.spaceeye.vmod.gui.additions.VEntityChangerWorldMenu


class ScreenWindow private constructor(): WindowScreen(ElementaVersion.V8, drawDefaultBackground = false) {
    private val linearExtensions = mutableListOf<ScreenWindowAddition>()
    private val _extensions = mutableSetOf<ScreenWindowAddition>()
    val extensions: Collection<ScreenWindowAddition> get() = _extensions

    fun addExtension(ext: ScreenWindowAddition): ScreenWindow {
        if (_extensions.add(ext)) {linearExtensions.add(ext); ext.init(screenContainer)}
        return this
    }

    inline fun <reified T: ScreenWindowAddition> getExtensionOfType(): T {
        return extensions.filterIsInstance<T>().first()
    }

    fun <T: ScreenWindowAddition> getExtensionOfType(type: Class<T>): T {
        return extensions.filterIsInstance(type).first()
    }

    val screenContainer = UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 100.percent()
        height = 100.percent()
    } childOf window

    fun onRenderHUD(stack: GuiGraphics, delta: Float) {
        linearExtensions.forEach { it.onRenderHUD(stack.pose(), delta) }

        PlatformUtils.renderScreen(this, stack, 0, 0, delta)
    }

    companion object {
        private val additions = mutableListOf<() -> ScreenWindowAddition>()

        fun addScreenAddition(constructor: () -> ScreenWindowAddition) {
            additions.add(constructor)
        }

        fun makeScreen(): ScreenWindow {
            return ScreenWindow().also { screen -> additions.forEach { screen.addExtension(it.invoke()) } }
        }

        init {
            addScreenAddition { HUDAddition() }
            addScreenAddition { ErrorAddition() }
            addScreenAddition { VEntityChangerWorldMenu }
        }
    }
}