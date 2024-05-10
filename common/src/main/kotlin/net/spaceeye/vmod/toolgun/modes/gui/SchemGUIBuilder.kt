package net.spaceeye.vmod.toolgun.modes.gui

import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.TranslatableComponent
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.networking.FakePacketContext
import net.spaceeye.vmod.toolgun.modes.GUIBuilder
import net.spaceeye.vmod.toolgun.modes.state.ClientPlayerSchematics
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import net.spaceeye.vmod.toolgun.modes.state.SchemNetworking
import java.awt.Color

interface SchemGUIBuilder: GUIBuilder {
    override val itemName get() = TranslatableComponent("Schem")

    override fun makeGUISettings(parentWindow: UIBlock) {
        this as SchemMode
        val paths = ClientPlayerSchematics.listSchematics()

        Button(Color.GRAY.brighter(), "Save") {
            ClientPlayerSchematics.saveSchemStream.r2tRequestData.transmitData(FakePacketContext(), ClientPlayerSchematics.SendSchemRequest(Minecraft.getInstance().player!!))
        }.constrain {
            x = 0.pixels()
            y = 0.pixels()

            width = ChildBasedSizeConstraint() + 4.pixels()
            height = ChildBasedSizeConstraint() + 4.pixels()
        } childOf parentWindow

        val itemsScroll = ScrollComponent().constrain {
            x = 1f.percent()
            y = SiblingConstraint() + 2.pixels()

            width = 98.percent()
            height = 98.percent()
        } childOf parentWindow

        for (path in paths) {
            val block = UIBlock().constrain {
                x = 0f.pixels()
                y = SiblingConstraint()

                width = 100.percent()
                height = ChildBasedMaxSizeConstraint() + 2.pixels()
            } childOf itemsScroll

            Button(Color.GRAY.brighter(), "Load") {
                schem = ClientPlayerSchematics.loadSchematic(path)
                if (schem != null) {
                    SchemNetworking.c2sLoadSchematic.sendToServer(SchemNetworking.C2SLoadSchematic())
                }
            }.constrain {
                x = 0.pixels()
                y = 0.pixels()

                width = ChildBasedSizeConstraint() + 4.pixels()
                height = ChildBasedSizeConstraint() + 4.pixels()
            } childOf block

            UIText(path.fileName.toString(), false).constrain {
                x = SiblingConstraint() + 2.pixels()
                y = 2.pixels()

                textScale = 1.pixels()
                color = Color.BLACK.toConstraint()
            } childOf block
        }
    }
}