package net.spaceeye.vmod.toolgun.modes.gui

import com.google.common.io.Files
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.FloatLimit
import net.spaceeye.vmod.limits.StrLimit
import net.spaceeye.vmod.toolgun.modes.EGUIBuilder
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ClientPlayerSchematics
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import net.spaceeye.vmod.toolgun.modes.state.SchemNetworking
import net.spaceeye.vmod.translate.*
import net.spaceeye.vmod.utils.EmptyPacket
import java.awt.Color
import java.util.*

class SaveForm(val mode: SchemMode): UIBlock(Color.GRAY.brighter()) {
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
            mode.filename = filename
            ClientPlayerSchematics.saveSchemStream.r2tRequestData.transmitData(ClientPlayerSchematics.SendSchemRequest(Minecraft.getInstance().player!!.uuid))
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

interface SchemGUI: GUIBuilder, EGUIBuilder {
    //stupidity but there shouldn't be more than 1 GUI object anyway
    companion object {
        var itemsScroll: ScrollComponent? = null
        lateinit var parentWindow: UIContainer
    }

    override val itemName get() = SCHEMATIC

    fun makeScroll() {
        this as SchemMode
        itemsScroll = ScrollComponent().constrain {
            x = 1f.percent()
            y = SiblingConstraint() + 2.pixels()

            width = 98.percent()
            height = 90.percent()
        } childOf parentWindow

        makeScrollItems()
    }

    fun makeScrollItems() {
        this as SchemMode
        val paths = ClientPlayerSchematics.listSchematics().sortedWith {a, b ->
            a.toString().lowercase(Locale.getDefault())
            .compareTo(b.toString().lowercase(Locale.getDefault()))}

        for (path in paths) {
            val block = UIBlock().constrain {
                x = 0f.pixels()
                y = SiblingConstraint()

                width = 100.percent()
                height = ChildBasedMaxSizeConstraint() + 2.pixels()
            } childOf itemsScroll!!

            Button(Color.GRAY.brighter(), LOAD.get()) {
                schem = ClientPlayerSchematics.loadSchematic(path)
                if (schem != null) {
                    SchemNetworking.c2sLoadSchematic.sendToServer(EmptyPacket())
                }
            }.constrain {
                x = 0.pixels()
                y = 0.pixels()

                width = ChildBasedSizeConstraint() + 4.pixels()
                height = ChildBasedSizeConstraint() + 4.pixels()
            } childOf block

            val name = Files.getNameWithoutExtension(path.fileName.toString())

            UIText(name, false).constrain {
                x = SiblingConstraint() + 2.pixels()
                y = 2.pixels()

                textScale = 1.pixels()
                color = Color.BLACK.toConstraint()
            } childOf block
        }
    }

    fun reloadScrollItems() {
        itemsScroll!!.clearChildren()
        makeScrollItems()
    }

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        this as SchemMode
        makeTextEntry(TRANSPARENCY.get(), ::transparency, 2f, 2f, parentWindow, FloatLimit(0f, 1f))

        Companion.parentWindow = parentWindow

        Button(Color.GRAY.brighter(), SAVE.get()) {
            SaveForm(this) childOf parentWindow
        }.constrain {
            x = 2.pixels()
            y = SiblingConstraint() + 2.pixels()
        } childOf parentWindow

        makeScroll()
    }
}