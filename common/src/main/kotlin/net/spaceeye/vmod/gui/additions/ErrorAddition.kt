package net.spaceeye.vmod.gui.additions

import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.animate
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.toConstraint
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.events.SessionEvents
import net.spaceeye.vmod.gui.ScreenWindow
import net.spaceeye.vmod.gui.ScreenWindowAddition
import net.spaceeye.vmod.gui.ServersideNetworking
import net.spaceeye.vmod.gui.additions.ErrorAddition.Companion.addHUDError
import net.spaceeye.vmod.gui.additions.ErrorAdditionNetworking.s2cErrorHappened
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.toolgun.ToolgunInstance
import net.spaceeye.vmod.translate.getTranslationKey
import net.spaceeye.vmod.translate.translate
import net.spaceeye.vmod.utils.LimitDeque
import net.spaceeye.vmod.utils.MPair
import net.spaceeye.vmod.utils.getNow_ms
import java.awt.Color
import net.spaceeye.vmod.toolgun.ServerToolGunState.S2CErrorHappened

class ErrorAddition: ScreenWindowAddition() {
    var maxErrorTime = 3000L

    lateinit var errorsContainer: UIContainer
    lateinit var bulkErrorsContainer: UIContainer

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

            width = 100.percent
        } childOf errorBlock

        toAdd.add(MPair(errorBlock, getNow_ms()))
        HUDAddition.refreshHUD()
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

    override fun init(screenContainer: UIContainer) {
        errorsContainer = UIContainer().constrain {
            x = 2.pixels(true)
            y = 2.pixels

            width = 40.percent
            height = 100.percent

        } childOf screenContainer

        bulkErrorsContainer = UIContainer().constrain {
            x = 0.pixels
            y = SiblingConstraint()

            width = 100.percent
            height = 100.percent
        } childOf errorsContainer
    }

    override fun onRenderHUD(stack: PoseStack, delta: Float) {
        checkErrorWindows()
    }

    companion object {
        @JvmStatic
        fun addHUDError(str: String) {
            ScreenWindow.screen?.getExtensionOfType<ErrorAddition>()?.addError(str)
        }

        @JvmStatic fun sendErrorTo(player: ServerPlayer, errorStr: String, translatable: Boolean = true, closeGUI: Boolean = false) = s2cErrorHappened.sendToClient(player, S2CErrorHappened(errorStr, translatable, closeGUI))
        @JvmStatic fun sendErrorTo(player: ServerPlayer, errorStr: Component, closeGUI: Boolean = false) = s2cErrorHappened.sendToClient(player, S2CErrorHappened(errorStr.getTranslationKey(), true, closeGUI))
    }
}

object ErrorAdditionNetworking: ServersideNetworking {
    val s2cErrorHappened = regS2C<S2CErrorHappened>(MOD_ID, "error_happened", "error_addition") { (errorStr, translate, closeGUI) ->
        SessionEvents.clientOnTick.on { _, unsub -> unsub.invoke()
            addHUDError(if (translate) errorStr.translate() else errorStr)
        }
    }

    override fun initConnections(instance: ToolgunInstance) {}
}