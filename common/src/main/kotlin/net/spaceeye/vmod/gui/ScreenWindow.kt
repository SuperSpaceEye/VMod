package net.spaceeye.vmod.gui

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.*
import gg.essential.elementa.constraints.*
import net.spaceeye.vmod.PlatformUtils
import net.spaceeye.vmod.gui.additions.ErrorAddition
import net.spaceeye.vmod.gui.additions.HUDAddition
import net.spaceeye.vmod.gui.additions.InfoAddition
import net.spaceeye.vmod.gui.additions.PresetsAddition
import net.spaceeye.vmod.gui.additions.VEntityChangerWorldMenu


//TODO maybe make it a class so that i can hot reload when elementa crashes
object ScreenWindow: WindowScreen(ElementaVersion.V8, drawDefaultBackground = false) {
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

    fun onRenderHUD(stack: PoseStack, delta: Float) {
        linearExtensions.forEach { it.onRenderHUD(stack, delta) }

        PlatformUtils.renderScreen(this, stack, 0, 0, delta)
    }

    private val additions = mutableListOf<() -> ScreenWindowAddition>()
    private var initialized = false

    fun addScreenAddition(constructor: () -> ScreenWindowAddition) {
        additions.add(constructor)
    }

    fun makeScreen(): ScreenWindow {
        if (!initialized) {
            additions.forEach { addExtension(it.invoke()) }
            initialized = true
        }

        return this
    }

    init {
        addScreenAddition { HUDAddition() }
        addScreenAddition { ErrorAddition() }
        addScreenAddition { VEntityChangerWorldMenu }
        addScreenAddition { InfoAddition }
        addScreenAddition { PresetsAddition }
    }
}