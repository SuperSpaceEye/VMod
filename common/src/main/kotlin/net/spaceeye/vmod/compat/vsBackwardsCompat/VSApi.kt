package net.spaceeye.vmod.compat.vsBackwardsCompat

object vsApi {
    class Builder {
        fun useTransientSerializer() {}
        fun build() = Builder()
    }
    fun registerAttachment(it: Any, fn: Builder.() -> Unit = {}) {}
}