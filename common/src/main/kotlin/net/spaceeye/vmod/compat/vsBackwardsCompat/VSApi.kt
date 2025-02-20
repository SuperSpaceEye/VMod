package org.valkyrienskies.mod.api

object vsApi {
    class Builder {
        fun useTransientSerializer() {}
        fun build() = Builder()
    }
    fun registerAttachment(it: Any, fn: Builder.() -> Unit = {}) {}
}