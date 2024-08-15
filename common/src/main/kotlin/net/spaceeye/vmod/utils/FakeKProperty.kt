package net.spaceeye.vmod.utils

import kotlin.reflect.*

class FakeKProperty<T>(
    val onGet: () -> T,
    val onSet: (T) -> Unit,
): KMutableProperty0<T> {
    override fun get(): T {
        return onGet()
    }

    override fun set(value: T) {
        return onSet(value)
    }

    override val annotations: List<Annotation> get() = TODO("Not implemented")
    override val getter: KProperty0.Getter<T> get() = TODO("Not implemented")
    override val isAbstract: Boolean get() = TODO("Not implemented")
    override val isConst: Boolean get() = TODO("Not implemented")
    override val isFinal: Boolean get() = TODO("Not implemented")
    override val isLateinit: Boolean get() = TODO("Not implemented")
    override val isOpen: Boolean get() = TODO("Not implemented")
    override val isSuspend: Boolean get() = TODO("Not implemented")
    override val name: String get() = TODO("Not implemented")
    override val parameters: List<KParameter> get() = TODO("Not implemented")
    override val returnType: KType get() = TODO("Not implemented")
    override val setter: KMutableProperty0.Setter<T> get() = TODO("Not implemented")
    override val typeParameters: List<KTypeParameter> get() = TODO("Not implemented")
    override val visibility: KVisibility? get() = TODO("Not implemented")
    override fun call(vararg args: Any?): T { TODO("Not implemented") }
    override fun callBy(args: Map<KParameter, Any?>): T { TODO("Not implemented") }
    override fun getDelegate(): Any? { TODO("Not implemented") }
    override fun invoke(): T { TODO("Not implemented") }
}