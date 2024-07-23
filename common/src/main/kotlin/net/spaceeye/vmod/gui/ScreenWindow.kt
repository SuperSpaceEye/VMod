package net.spaceeye.vmod.gui

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.dsl.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.DefaultHUD
import net.spaceeye.vmod.utils.LimitDeque
import net.spaceeye.vmod.utils.MPair
import net.spaceeye.vmod.utils.getNow_ms
import java.awt.Color

class ScreenWindow: WindowScreen(ElementaVersion.V5, drawDefaultBackground = false) {
    var maxErrorTime = 3000L

    val screenContainer = UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 100.percent()
        height = 100.percent()
    } childOf window

    val errorsContainer = UIContainer().constrain {
        x = 2.pixels(true)
        y = 2.pixels

        width = 40.percent
        height = 100.percent

    } childOf window

    val bulkErrorsContainer = UIContainer().constrain {
        x = 0.pixels
        y = SiblingConstraint()

        width = 100.percent
        height = 100.percent
    } childOf errorsContainer

    private var refreshHUD = true
    fun refreshHUD() {
        refreshHUD = true
    }

    val errorsDequeue = LimitDeque<MPair<UIBlock, Long>>(5)
    val toAdd = ArrayDeque<MPair<UIBlock, Long>>(1)

    fun addError(string: String) {
        val errorBlock = UIBlock() constrain {
            x = 0.pixels
            y = SiblingConstraint() + 2.pixels()

            width = 100.percent
            height = ChildBasedSizeConstraint() + 4.pixels

            color = Color.YELLOW.toConstraint()
        }

        val text = UIWrappedText(string, false) constrain {
            x = 5.pixels
            y = CenterConstraint()

            color = Color.BLACK.toConstraint()

            textScale = 1.pixels
        } childOf errorBlock

        toAdd.add(MPair(errorBlock, getNow_ms()))
        refreshHUD()
    }

    var animating = false

    private fun animatePop(block: UIBlock) {
        bulkErrorsContainer.removeChild(block)
        errorsContainer.clearChildren()

        errorsContainer.addChild(block)
        errorsContainer.addChild(bulkErrorsContainer)

        block.animate {
            setXAnimation(Animations.OUT_EXP, 0.15f, 150.percent())
        }
    }

    private fun animateMoveUp() {
        val removedChild = errorsDequeue.first().first
        bulkErrorsContainer.animate {
            setYAnimation(Animations.OUT_EXP, 0.15f, SiblingConstraint() - removedChild.getHeight().pixels - 2f.pixels, 0.15f).onComplete {
                errorsDequeue.removeFirst()
                errorsContainer.removeChild(removedChild)

                bulkErrorsContainer constrain {
                    x = 0.pixels
                    y = SiblingConstraint()

                    width = 100.percent
                    height = 100.percent
                }
                animating = false
            }
        }
    }

    private fun checkErrorWindows() {
        while (toAdd.isNotEmpty()) {
            if (errorsDequeue.size < errorsDequeue.limitSize) {
                val pair = toAdd.first()
                errorsDequeue.add(pair)
                toAdd.removeFirst()

                val block = pair.first
                bulkErrorsContainer.addChild(block)
                block.animate {
                    setColorAnimation(Animations.OUT_EXP, 2f, Color.WHITE.toConstraint())
                }
            } else {
                errorsDequeue.first().second = 0L
                break
            }
        }
        if (errorsDequeue.isEmpty()) {return}

        val now = getNow_ms()

        if (now - errorsDequeue.first().second < maxErrorTime) { return }

        if (animating) {return}
        animating = true

        val toRemove = errorsDequeue.first().first
        animatePop(toRemove)
        animateMoveUp()
    }

    fun onRenderHUD(stack: PoseStack, delta: Float) {
        val currentMode = ClientToolGunState.currentMode ?: defaultHUD

        checkErrorWindows()

        if (refreshHUD) {
            screenContainer.clearChildren()
            currentMode.makeHUD(screenContainer)
            refreshHUD = false
        }

        render(stack, 0, 0, delta)
    }

    companion object {
        val defaultHUD = DefaultHUD()
    }
}