package net.spaceeye.vmod.gui.additions

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import net.spaceeye.vmod.gui.ScreenWindowAddition
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ToolgunItem.Companion.playerIsUsingToolgun
import net.spaceeye.vmod.toolgun.modes.DefaultHUD

class HUDAddition: ScreenWindowAddition {
    private lateinit var hudContainer: UIContainer
    private var refreshHUD = true

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
        if (!playerIsUsingToolgun()) {
            hudContainer.clearChildren()
            return
        }

        val currentMode = ClientToolGunState.currentMode ?: defaultHUD

        if (refreshHUD) {
            hudContainer.clearChildren()
            currentMode.makeHUD(hudContainer)
            refreshHUD = false
        }
    }

    companion object {
        private val defaultHUD = DefaultHUD()
    }
}