package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import gg.essential.elementa.components.UIContainer
import net.minecraft.network.FriendlyByteBuf
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.jetbrains.annotations.ApiStatus.OverrideOnly

interface EGUIBuilder {
    @OverrideOnly fun eMakeGUISettings(parentWindow: UIContainer) {}
}

interface EHUDBuilder {
    @OverrideOnly fun eMakeHUD(screen: UIContainer) {}
}

interface EClientEventsHandler {
    @OverrideOnly fun eOnKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Boolean { return false }
    @OverrideOnly fun eOnMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult { return EventResult.pass() }
    @OverrideOnly fun eOnMouseScrollEvent(amount: Double): EventResult { return EventResult.pass() }

    @OverrideOnly fun eOnOpenMode() {}
    @OverrideOnly fun eOnCloseMode() {}
}

interface EBase {
    @OverrideOnly fun eResetState() {}
    @OverrideOnly fun eInit(type: BaseNetworking.EnvType) {}
}

interface ToolgunModeExtension: MSerializable, EBase, EClientEventsHandler, EGUIBuilder {
    @OverrideOnly fun preInit(mode: ExtendableToolgunMode, type: BaseNetworking.EnvType) {}
    @OverrideOnly fun onInit(mode: ExtendableToolgunMode, type: BaseNetworking.EnvType) {}
    @NonExtendable @Deprecated("DO NOT USE THIS", ReplaceWith("super.onInit"), level = DeprecationLevel.ERROR)
    @OverrideOnly override fun eInit(type: BaseNetworking.EnvType) {}
}

abstract class ExtendableToolgunMode: BaseMode, EBase, EGUIBuilder, EHUDBuilder, EClientEventsHandler {
    private val linearExtensions = mutableListOf<ToolgunModeExtension>()
    private val _extensions = mutableSetOf<ToolgunModeExtension>()
    open val extensions: Collection<ToolgunModeExtension> get() = _extensions

    open fun <T: ExtendableToolgunMode> addExtension(fn: (T) -> ToolgunModeExtension): T {
        val ext = fn(this as T)
        if (_extensions.add(ext)) {linearExtensions.add(ext)}
        return this
    }

    inline fun <reified T: ToolgunModeExtension> getExtensionsOfType(): Collection<T> {
        return extensions.filterIsInstance<T>()
    }

    inline fun <T: ToolgunModeExtension> getExtensionsOfType(type: Class<T>): Collection<T> {
        return extensions.filterIsInstance(type)
    }

    inline fun <reified T: ToolgunModeExtension> getExtensionOfType(): T {
        for (it in getExtensionsOfType<T>()) { return it }
        throw AssertionError("No instance of type ${T::class.java.name}")
    }

    inline fun <T: ToolgunModeExtension> getExtensionOfType(type: Class<T>): T {
        for (it in getExtensionsOfType(type)) { return it }
        throw AssertionError("No instance of type ${type.name}")
    }

    final override fun makeGUISettings(parentWindow: UIContainer) {
        eMakeGUISettings(parentWindow)
        linearExtensions.forEach { it.eMakeGUISettings(parentWindow) }
    }

    override fun makeHUD(screen: UIContainer) {
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
        var res = false
        _extensions.forEach { res = it.eOnMouseScrollEvent(amount).interruptsFurtherEvaluation() || res }
        res = eOnMouseScrollEvent(amount).interruptsFurtherEvaluation() || res
        return if (res) EventResult.interruptFalse() else EventResult.pass()
    }

    final override fun onOpenMode() {
        _extensions.forEach { it.eOnOpenMode() }
        return eOnOpenMode()
    }

    final override fun onCloseMode() {
        _extensions.forEach { it.eOnCloseMode() }
        return eOnCloseMode()
    }

    final override fun serialize(): FriendlyByteBuf {
        val mainBuf = super.serialize()

        linearExtensions.forEach {
            val extBuf = it.serialize()
            if (extBuf.writerIndex() == 0) {return@forEach}
            mainBuf.writeInt(ToolgunExtensions.typeToIdx(it.javaClass)!!)
            mainBuf.writeBytes(extBuf)
        }

        return mainBuf
    }


    final override fun deserialize(buf: FriendlyByteBuf) {
        super.deserialize(buf)

        while (buf.isReadable) {
            val idx = buf.readInt()
            val type = ToolgunExtensions.idxToType(idx)
            val ext = getExtensionOfType(type)
            ext.deserialize(buf)
        }
    }

    final override fun serverSideVerifyLimits() {
        super.serverSideVerifyLimits()
    }

    final override fun resetState() {
        _extensions.forEach { it.eResetState() }
        eResetState()
    }

    var wasInitialized = false

    final override fun init(type: BaseNetworking.EnvType) {
        if (wasInitialized) return

        _extensions.forEach { it.preInit(this, type) }
        super.eInit(type)
        _extensions.forEach { it.onInit(this, type) }

        wasInitialized = true
    }

    final override fun refreshHUD() { super.refreshHUD() }
}