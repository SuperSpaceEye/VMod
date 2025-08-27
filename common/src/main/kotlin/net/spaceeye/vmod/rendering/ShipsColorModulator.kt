package net.spaceeye.vmod.rendering

import net.spaceeye.vmod.utils.ClientClosable
import java.awt.Color

//TODO doesn't work with sodium
object ShipsColorModulator: ClientClosable() {
    private val idToColor = mutableMapOf<Long, FloatArray>()
    private val defaultColor = floatArrayOf(1f, 1f, 1f, 1f)

    override fun close() { idToColor.clear() }

    fun setColor(id: Long, color: Color) { idToColor[id] = color.getRGBComponents(null) }
    fun setColor(id: Long, color: FloatArray) { idToColor[id] = color }

    fun deleteColor(id: Long) { idToColor.remove(id) }
    fun getColor(id: Long) = idToColor[id] ?: defaultColor
}