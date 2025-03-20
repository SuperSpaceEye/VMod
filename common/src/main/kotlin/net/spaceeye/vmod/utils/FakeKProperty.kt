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

    override val annotations: List<Annotation> get() = throw NotImplementedError("Not implemented")
    override val getter: KProperty0.Getter<T> get() = throw NotImplementedError("Not implemented")
    override val isAbstract: Boolean get() = throw NotImplementedError("Not implemented")
    override val isConst: Boolean get() = throw NotImplementedError("Not implemented")
    override val isFinal: Boolean get() = throw NotImplementedError("Not implemented")
    override val isLateinit: Boolean get() = throw NotImplementedError("Not implemented")
    override val isOpen: Boolean get() = throw NotImplementedError("Not implemented")
    override val isSuspend: Boolean get() = throw NotImplementedError("Not implemented")
    override val name: String get() = throw NotImplementedError("Not implemented")
    override val parameters: List<KParameter> get() = throw NotImplementedError("Not implemented")
    override val returnType: KType get() = throw NotImplementedError("Not implemented")
    override val setter: KMutableProperty0.Setter<T> get() = throw NotImplementedError("Not implemented")
    override val typeParameters: List<KTypeParameter> get() = throw NotImplementedError("Not implemented")
    override val visibility: KVisibility? get() = throw NotImplementedError("Not implemented")
    override fun call(vararg args: Any?): T { throw NotImplementedError("Not implemented") }
    override fun callBy(args: Map<KParameter, Any?>): T { throw NotImplementedError("Not implemented") }
    override fun getDelegate(): Any? { throw NotImplementedError("Not implemented") }
    override fun invoke(): T { throw NotImplementedError("Not implemented") }
}