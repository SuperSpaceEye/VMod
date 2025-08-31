package net.spaceeye.vmod.gui.additions

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.gui.ScreenWindowAddition
import net.spaceeye.vmod.toolgun.modes.DefaultHUD

class HUDAddition: ScreenWindowAddition() {
    private lateinit var hudContainer: UIContainer
    private var refreshHUD = true
    var renderHUD = true

    var defaultHUD = DefaultHUD()

    fun refreshHUD() {
        refreshHUD = true
    }

    override fun init(screenContainer: UIContainer) {
        hudContainer = UIContainer().constrain {
            x = CenterConstraint()
            y = CenterConstraint()

            width = 100.percent()
            height = 100.percent()
        } childOf screenContainer
    }

    override fun onRenderHUD(stack: PoseStack, delta: Float) {
        if (!renderHUD || !instance.client.playerIsUsingToolgun()) {
            hudContainer.clearChildren()
            refreshHUD = true
            return
        }

        val currentMode = instance.client.currentMode ?: defaultHUD

        if (refreshHUD) {
            hudContainer.clearChildren()
            currentMode.makeHUD(hudContainer)
            refreshHUD = false
        }
    }

    companion object {
        fun refreshHUD() { ScreenWindow.screen?.getExtensionsOfType<HUDAddition>()?.forEach { it.refreshHUD() } }
    }
}