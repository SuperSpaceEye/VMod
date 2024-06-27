package net.spaceeye.vmod.utils

class LimitDeque<T>(val limitSize: Int) : MutableList<T> by ArrayDeque() {
    fun addLimited(element: T): Boolean {
        if (size >= limitSize) removeFirst()
        return add(element)
    }
}