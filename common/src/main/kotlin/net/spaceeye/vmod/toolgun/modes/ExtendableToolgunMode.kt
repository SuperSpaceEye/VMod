package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import gg.essential.elementa.components.UIContainer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.utils.RegistryObject

interface EGUIBuilder {
    fun eMakeGUISettings(parentWindow: UIContainer) {}
}

interface EHUDBuilder {
    fun eMakeHUD(screen: UIContainer) {}
}

interface EClientEventsHandler {
    fun eOnKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean { return false }
    fun eOnMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult { return EventResult.pass() }
    fun eOnMouseScrollEvent(amount: Double): EventResult { return EventResult.pass() }

    fun eOnOpenMode() {}
    fun eOnCloseMode() {}
}

interface EBase {
    fun eResetState() {}
    fun eInit(type: BaseNetworking.EnvType) {}
}

interface ToolgunModeExtension: RegistryObject, EClientEventsHandler, EBase, EGUIBuilder {
    fun onInit(mode: ExtendableToolgunMode, type: BaseNetworking.EnvType) {}
}

abstract class ExtendableToolgunMode: BaseMode, EBase, EGUIBuilder, EHUDBuilder, EClientEventsHandler {
    private val _extensions = mutableSetOf<ToolgunModeExtension>()
    open val extensions: Collection<ToolgunModeExtension> get() = _extensions

    open fun <T: ExtendableToolgunMode> addExtension(fn: (T) -> ToolgunModeExtension): T {
        _extensions.add(fn(this as T))
        return this
    }

    inline fun <reified T: ToolgunModeExtension> getExtensionsOfType(): Collection<T> {
        return extensions.filterIsInstance<T>()
    }

    final override fun makeGUISettings(parentWindow: UIContainer) {
        return eMakeGUISettings(parentWindow)
    }

    final override fun makeHUD(screen: UIContainer) {
        return eMakeHUD(screen)
    }

    final override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        var res = false
        _extensions.forEach { res = it.eOnKeyEvent(key, scancode, action, mods) || res }
        return eOnKeyEvent(key, scancode, action, mods) || res
    }

    final override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        var res = false
        _extensions.forEach { res = it.eOnMouseButtonEvent(button, action, mods).interruptsFurtherEvaluation() || res }
        res = eOnMouseButtonEvent(button, action, mods).interruptsFurtherEvaluation() || res
        return if (res) EventResult.interruptFalse() else EventResult.pass()
    }

    final override fun onMouseScrollEvent(amount: Double): EventResult {
        return eOnMouseScrollEvent(amount)
    }

    final override fun onOpenMode() {
        return eOnOpenMode()
    }

    final override fun onCloseMode() {
        return eOnCloseMode()
    }

    final override fun serialize(): FriendlyByteBuf {
        return super.serialize()
    }

    final override fun deserialize(buf: FriendlyByteBuf) {
        super.deserialize(buf)
    }

    final override fun serverSideVerifyLimits() {
        super.serverSideVerifyLimits()
    }

    final override fun resetState() {
        eResetState()
    }

    final override fun init(type: BaseNetworking.EnvType) {
        super.eInit(type)
        _extensions.forEach { it.onInit(this, type) }
    }

    final override fun refreshHUD() { super.refreshHUD() }
}