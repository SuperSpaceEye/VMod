package net.spaceeye.vmod.toolgun.gui

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeText
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.StrLimit
import net.spaceeye.vmod.reflectable.ReflectableItemDelegate
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.toolgun.ToolgunInstance
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModeExtension
import net.spaceeye.vmod.translate.CANCEL
import net.spaceeye.vmod.translate.DELETE
import net.spaceeye.vmod.translate.FAILED_TO_MAKE_PRESET
import net.spaceeye.vmod.translate.FILENAME
import net.spaceeye.vmod.translate.INVALID_FILENAME
import net.spaceeye.vmod.translate.LOAD
import net.spaceeye.vmod.translate.MAKE_PRESET
import net.spaceeye.vmod.translate.NO_PRESETS
import net.spaceeye.vmod.translate.PRESETS
import net.spaceeye.vmod.translate.SAVE
import net.spaceeye.vmod.translate.SOMETHING_WENT_WRONG
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.getMapper
import java.awt.Color
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

class SaveForm(val obj: ReflectableObject, instance: ToolgunInstance): UIBlock(Color.GRAY.brighter()) {
    var filename = ""

    init {
        constrain {
            x = CenterConstraint()
            y = CenterConstraint()

            width = 150.pixels()
            height = 50.pixels()
        }

        val entry = makeTextEntry(FILENAME.get(), ::filename, 2f, 2f, this, StrLimit(50))
        entry.focus()

        Button(Color.GRAY.brighter().brighter(), SAVE.get()) {
            parent.removeChild(this)
            var dirName = instance.instanceStorage["Presettable-dir-name"] as String

            try {
                File("${filename}.json").canonicalPath
            } catch (e: Exception) {
                instance.client.closeWithError(INVALID_FILENAME.get())
                return@Button
            }

            val objData = obj.getAllReflectableItems(true) { it.metadata.contains("presettable") }
            val json = SettingPresets.toJsonStr(objData)

            try {
                Files.createDirectories(Paths.get("$dirName/${obj::class.simpleName}"))
            } catch (e: IOException) {}
            try {
                Files.writeString(Paths.get("$dirName/${obj::class.simpleName}/${filename}.json"), json)
            } catch (e: Exception) {
                instance.client.closeWithError(FAILED_TO_MAKE_PRESET.get())
            }
        }.constrain {
            x = 2.pixels()
            y = SiblingConstraint() + 2.pixels()
        } childOf this

        Button(Color.GRAY.brighter().brighter(), CANCEL.get()) {
            parent.removeChild(this)
        }.constrain {
            x = 2.pixels()
            y = SiblingConstraint() + 2.pixels()
        } childOf this
    }
}

class Presettable: ToolgunModeExtension {
    lateinit var mode: ExtendableToolgunMode

    override fun onInit(mode: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        if (type != BaseNetworking.EnvType.Client) return
        this.mode = mode
        modes[mode::class.simpleName!!] = mode
    }

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        parentWindow as ScrollComponent

        val btn = Button(Color(150, 150, 150), MAKE_PRESET.get()) {
            val mode = mode as ReflectableObject
            SaveForm(mode, this.mode.instance) childOf parentWindow
        }

        val children = parentWindow.allChildren.map { it }
        parentWindow.clearChildren()

        btn constrain {
            x = 2f.pixels()
            y = SiblingConstraint(1f) + 2.pixels
            width = 100.percent - 4.pixels
        } childOf parentWindow

        children.forEach { parentWindow.addChild(it) }
    }

    val modes: MutableMap<String, ExtendableToolgunMode>
        get() = this.mode.instance.instanceStorage.getOrPut("Presettable") { mutableMapOf<String, ExtendableToolgunMode>() } as MutableMap<String, ExtendableToolgunMode>

    companion object {
        fun <T : Any> ReflectableItemDelegate<T>.presettable(): ReflectableItemDelegate<T> {
            this.metadata["presettable"] = true
            return this
        }
    }
}

class SettingPresets(val mainWindow: UIBlock, val instance: ToolgunInstance): BaseToolgunGUIWindow(mainWindow) {
    init { init() }

    val pModes: MutableMap<String, ExtendableToolgunMode>
        get() = this.instance.instanceStorage.getOrPut("Presettable") { mutableMapOf<String, ExtendableToolgunMode>() } as MutableMap<String, ExtendableToolgunMode>

    fun init() {
        settingsComponent constrain {
            val offset = 2

            x = offset.percent()
            y = 2.percent()

            width = 100.percent() - offset.percent() - 2.percent()
            height = 100.percent() - 4.percent()
        }

        val presets = listPresets(this.instance.instanceStorage["Presettable-dir-name"] as String)
        val modes = presets.filterKeys { pModes.contains(it) }

        if (modes.isEmpty()) {
            val text = makeText(NO_PRESETS.get(), Color.BLACK, 2f, 2f, settingsScrollComponent)
            text constrain {
                width = text.getTextWidth().pixels

                x = CenterConstraint()
                y = CenterConstraint()
            }
            return
        }

        val sortedKeys = modes.keys.sortedWith {a, b -> a.compareTo(b) }
        sortedKeys.forEach { typeName ->
            val presets = modes[typeName]!!
            var chosenPreset = Paths.get("")

            val mode = pModes[typeName]!!

            val holder = UIContainer() constrain {
                x = 2.pixels
                y = SiblingConstraint(2f) + 1.pixels

                width = ChildBasedSizeConstraint()
                height = ChildBasedMaxSizeConstraint()
            }

            makeText(mode.itemName.get(), Color.BLACK, 2f, 2f, holder) constrain {
                x = SiblingConstraint(2f) + 1.pixels
                y = CenterConstraint()
            }

            //TODO maybe rename to chosen preset name?
            makeDropDown(PRESETS.get(), holder, 2f, 2f, presets.map {
                DItem(it.nameWithoutExtension, false) {chosenPreset = it}
            }) constrain {
                x = SiblingConstraint(2f) + 1.pixels
                y = CenterConstraint()
            }

            Button(Color(150, 150, 150), LOAD.get()) {
                val jsonStr = try { Files.readString(chosenPreset) } catch (e: Exception) {return@Button}
                val items = mode.getAllReflectableItems(true) {it.metadata.contains("presettable")}
                try {
                    fromJsonStr(jsonStr, items)
                } catch (e: Exception) {
                    ELOG(e.stackTraceToString())
                    instance.client.closeWithError(SOMETHING_WENT_WRONG.get())
                }
            } constrain {
                x = SiblingConstraint(2f) + 1.pixels
                y = CenterConstraint()
            } childOf holder

            //TODO it rebuilds whole window on delete, but do i care?
            Button(Color(150, 150, 150), DELETE.get()) {
                if (chosenPreset == Paths.get("")) return@Button
                try {
                    if (!Files.deleteIfExists(chosenPreset)) return@Button
                } catch (e: Exception) {
                    ELOG(e.stackTraceToString())
                    instance.client.closeGUI()
                    return@Button
                }
                settingsScrollComponent.clearChildren()
                init()
            } constrain {
                x = SiblingConstraint(2f) + 1.pixels
                y = CenterConstraint()
            } childOf holder

            holder childOf settingsScrollComponent
        }
    }

    override fun onGUIOpen() {}

    companion object {
        @JvmStatic fun listPresets(mode: String, dirName: String): List<Path> {
            if (!Files.exists(Paths.get(dirName))) return emptyList()
            if (!Files.exists(Paths.get("$dirName/${mode}"))) return emptyList()
            return Files
                .list(Paths.get("$dirName/${mode}")).toList().toList()
                .filter { it.isRegularFile() && it.extension == "json" }
        }

        @JvmStatic fun listPresets(dirName: String): Map<String, List<Path>> {
            if (!Files.exists(Paths.get(dirName))) return emptyMap()
            return Files
                .list(Paths.get(dirName)).toList().toList()
                .filter { it.isDirectory() && Files.list(it).toList().toList().any { it.extension == "json" }}
                .associate { Pair(it.name, Files.list(it).toList().toList().filter { it.extension == "json" }) }
        }

        @JvmStatic fun toJsonStr(items: List<ReflectableItemDelegate<*>>): String {
            val serData = items.associate { Pair(it.cachedName, it.it!!) }
            return getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(serData)
        }

        @JvmStatic fun fromJsonStr(json: String, items: List<ReflectableItemDelegate<*>>) {
            val mapper = getMapper()

            val deserData = items.associate { Pair(it.cachedName, it) }
            val tree = mapper.readTree(json)
            for (name in tree.fieldNames()) {
                val defaultItem = deserData[name]?.it ?: continue

                val node = tree.get(name)
                val newItem = mapper.treeToValue(node, defaultItem::class.java)
                deserData[name]?.setValue(null, null, newItem)
            }
        }
    }
}