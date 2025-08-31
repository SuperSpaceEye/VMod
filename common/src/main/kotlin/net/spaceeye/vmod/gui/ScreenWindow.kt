package net.spaceeye.vmod.gui

import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.*
import gg.essential.elementa.constraints.*
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.PlatformUtils
import net.minecraft.client.gui.GuiGraphics
import net.spaceeye.vmod.gui.additions.ErrorAddition
import net.spaceeye.vmod.gui.additions.ErrorAdditionNetworking
import net.spaceeye.vmod.toolgun.ToolgunInstance
import net.spaceeye.vmod.toolgun.VMToolgun

fun addScreenAddition(constructor: () -> ScreenWindowAddition) {
    EnvExecutor.runInEnv(Env.CLIENT) {Runnable {
        ScreenWindow.additions.add(constructor)
    }}
}

fun <T: ServersideNetworking> addScreenAddition(instance: ToolgunInstance, obj: T, constructor: () -> ScreenWindowAddition) {
    obj.initConnections(instance)
    EnvExecutor.runInEnv(Env.CLIENT) {Runnable {
        ScreenWindow.additions.add(constructor)
    }}
}

//TODO maybe make it a class so that i can hot reload when elementa crashes
object ScreenWindow: WindowScreen(ElementaVersion.V8, drawDefaultBackground = false) {
    var screen: ScreenWindow? = null

    private val linearExtensions = mutableListOf<ScreenWindowAddition>()
    private val _extensions = mutableSetOf<ScreenWindowAddition>()
    val extensions: Collection<ScreenWindowAddition> get() = _extensions

    fun addExtension(ext: ScreenWindowAddition): ScreenWindow {
        if (_extensions.add(ext)) {linearExtensions.add(ext); ext.init(screenContainer)}
        return this
    }

    inline fun <reified T: ScreenWindowAddition> getExtensionsOfType(): List<T> = extensions.filterIsInstance<T>()
    fun <T: ScreenWindowAddition> getExtensionsOfType(type: Class<T>): List<T> = extensions.filterIsInstance(type)
    inline fun <reified T: ScreenWindowAddition> getExtensionOfType(): T = extensions.filterIsInstance<T>().first()
    fun <T: ScreenWindowAddition> getExtensionOfType(type: Class<T>): T = extensions.filterIsInstance(type).first()

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

    internal val additions = mutableListOf<() -> ScreenWindowAddition>()

    private var initialized = false
    fun makeScreen(): ScreenWindow {
        if (!initialized) {
            additions.forEach { addExtension(it.invoke()) }
            initialized = true
        }

        return this
    }

    init {
        addScreenAddition(VMToolgun, ErrorAdditionNetworking) { ErrorAddition() }

        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            ClientLifecycleEvent.CLIENT_STARTED.register { initScreen() }
            ClientGuiEvent.RENDER_HUD.register { stack, delta -> onRenderHUD(stack, delta) }
        }}
    }

    @JvmStatic fun onRenderHUD(stack: PoseStack, delta: Float) {
        try { (screen ?: initScreen()).onRenderHUD(stack, delta)
        } catch (e: Exception) { ELOG("HUD rendering failed\n${e.stackTraceToString()}")
        } catch (e: Error) { ELOG("HUD rendering failed\n${e.stackTraceToString()}") }
    }

    @JvmStatic fun initScreen(): ScreenWindow {
        screen = makeScreen()
        // why? it doesn't correctly remap in dev env when added as a dependency for another project for some reason, this works though
        PlatformUtils.initScreen(screen!!)
        return screen!!
    }
}