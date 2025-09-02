package net.spaceeye.vmod.guiElements

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.FloatLimit
import net.spaceeye.vmod.limits.IntLimit
import net.spaceeye.vmod.limits.LongLimit
import net.spaceeye.vmod.limits.StrLimit
import java.awt.Color
import kotlin.reflect.KMutableProperty0

class TextEntry(strName: String, text_scale: Float = 1f, fnToApply: (str: String, entry: TextEntry) -> Unit): UIBlock() {
    val textArea: UITextInput
    val textHolder: UIBlock

    init {
        constrain {
            width = FillConstraint()
            height = ChildBasedMaxSizeConstraint() + 4.pixels
        }

        val strHolder = UIBlock().constrain {
            x = 4.pixels()
            y = CenterConstraint()

            width = ChildBasedSizeConstraint() + 4.pixels()
            height = ChildBasedMaxSizeConstraint()
        } childOf this

        val str = UIText(strName, shadow = false).constrain {
            y = CenterConstraint()

            textScale = text_scale.pixels()

            color = Color(0, 0, 0).toConstraint()
        } childOf strHolder

        textHolder = UIBlock(Color(170, 170, 170)).constrain {
            x = SiblingConstraint()
            y = CenterConstraint()

            width = FillConstraint() - 4.pixels() - 4.pixels()
            height = (textScale * 9f)
        } childOf this
        textArea = (((UITextInput().constrain {
            x = CenterConstraint()
            y = CenterConstraint()

            width = FillConstraint()
            height = FillConstraint()

            color = Color.BLACK.toConstraint()
        }.onMouseClick {
            grabWindowFocus()
        }.onFocus {
            focused = true
        }.onFocusLost {
            focused = false
        }) as UITextInput).onUpdate { fnToApply(it, this) } childOf textHolder) as UITextInput
    }

    companion object {
        var focused: Boolean = false
    }
}

fun <T> makeTextEntryBase(
    name: String,
    value: KMutableProperty0<T>,
    xPadding: Float,
    yPadding: Float,
    makeChildOf: UIComponent,
    strToValue: (String) -> T?,
    blockValue: (T) -> Boolean,
): TextEntry {
    val entry = TextEntry(name) { str, entry ->
        val parsedValue = strToValue(str)

        if (parsedValue == null || blockValue(parsedValue)) {
            entry.textHolder.setColor(Color(230, 0, 0))
            return@TextEntry
        }
        entry.textHolder.setColor(Color(170, 170, 170))

        value.set(parsedValue)
    }.constrain {
        x = xPadding.pixels()
        y = SiblingConstraint(yPadding/2) + (yPadding/2).pixels()
        width = 100.percent() - (xPadding * 2).pixels()
    } childOf makeChildOf
    entry.textArea.setText(value.get().toString())
    return entry
}

fun makeTextEntry(name: String, value: KMutableProperty0<Double>, xPadding: Float, yPadding: Float, makeChildOf: UIComponent, limits: DoubleLimit = DoubleLimit()): TextEntry {
    return makeTextEntryBase(name, value, xPadding, yPadding, makeChildOf, {it.toDoubleOrNull()}) { (it < limits.minValue || it > limits.maxValue) }
}

fun makeTextEntry(name: String, value: KMutableProperty0<Float>, xPadding: Float, yPadding: Float, makeChildOf: UIComponent, limits: FloatLimit = FloatLimit()): TextEntry {
    return makeTextEntryBase(name, value, xPadding, yPadding, makeChildOf, {it.toFloatOrNull()}) { (it < limits.minValue || it > limits.maxValue) }
}

fun makeTextEntry(name: String, value: KMutableProperty0<Long>, xPadding: Float, yPadding: Float, makeChildOf: UIComponent, limits: LongLimit = LongLimit()): TextEntry {
    return makeTextEntryBase(name, value, xPadding, yPadding, makeChildOf, {it.toLongOrNull()}) { (it < limits.minValue || it > limits.maxValue) }
}

fun makeTextEntry(name: String, value: KMutableProperty0<Int>, xPadding: Float, yPadding: Float, makeChildOf: UIComponent, limits: IntLimit = IntLimit()): TextEntry {
    return makeTextEntryBase(name, value, xPadding, yPadding, makeChildOf, {it.toIntOrNull()}) { (it < limits.minValue || it > limits.maxValue) }
}

fun makeTextEntry(name: String, value: KMutableProperty0<String>, xPadding: Float, yPadding: Float, makeChildOf: UIComponent, limit: StrLimit = StrLimit()): TextEntry {
    return makeTextEntryBase(name, value, xPadding, yPadding, makeChildOf, {it}) { (it.length > limit.sizeLimit) }
}

fun makeTextEntriesLimit(name: String, names: List<String>, values: List<KMutableProperty0<*>>, xPadding: Float, yPadding: Float, makeChildOf: UIComponent, limit: Any? = null): UIContainer {
    return makeTextEntries(name, names, values, xPadding, yPadding, makeChildOf, limit?.let { (0 until names.size).map { limit } })
}

fun makeTextEntries(name: String, names: List<String>, values: List<KMutableProperty0<*>>, xPadding: Float, yPadding: Float, makeChildOf: UIComponent, limits: List<*>? = null): UIContainer {
    val storage = UIContainer().constrain {
        x = xPadding.pixels()
        y = SiblingConstraint(yPadding/2)
        width = 100.percent() - (xPadding * 2).pixels()
        height = ChildBasedMaxSizeConstraint()
    } childOf makeChildOf

    val text = UIText(name, false) constrain {
        y = CenterConstraint()
        x = SiblingConstraint(1f) + 1f.pixels
        color = Color.BLACK.toConstraint()
    } childOf storage

    names.zip(values).forEachIndexed { i, (name, value) ->
        when (value.get()) {
            is Double -> makeTextEntry(name, value as KMutableProperty0<Double>, 2f, 2f, storage, limits?.let { it[i] as DoubleLimit } ?: DoubleLimit())
            is Float  -> makeTextEntry(name, value as KMutableProperty0<Float>,  2f, 2f, storage, limits?.let { it[i] as FloatLimit } ?: FloatLimit())
            is Long   -> makeTextEntry(name, value as KMutableProperty0<Long>,   2f, 2f, storage, limits?.let { it[i] as LongLimit } ?: LongLimit())
            is Int    -> makeTextEntry(name, value as KMutableProperty0<Int>,    2f, 2f, storage, limits?.let { it[i] as IntLimit } ?: IntLimit())
            is String -> makeTextEntry(name, value as KMutableProperty0<String>, 2f, 2f, storage, limits?.let { it[i] as StrLimit } ?: StrLimit())
            else -> throw NotImplementedError()
        }.constrain {
            width = ((100.percent - text.getWidth().pixels) / names.size) - (names.size * 1f).pixels
            y = CenterConstraint()
            x = SiblingConstraint(1f) + 1f.pixels
        }
    }
    return storage
}