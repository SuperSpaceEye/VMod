package net.spaceeye.vmod.toolgun.modes.util

import gg.essential.elementa.components.UIContainer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.ClientRenderingData
import net.spaceeye.vmod.rendering.types.special.PrecisePlacementAssistRenderer
import net.spaceeye.vmod.translate.*

//TODO convert all modes to extendable and remove this
interface PlacementModesState {
    var posMode: PositionModes
    var precisePlacementAssistSideNum: Int
    var precisePlacementAssistRendererId: Int
}

interface PlacementModesCEH: PlacementModesState {
    fun pmOnOpen() {}
    fun pmOnClose() {}
}

interface PlacementModesGUI: PlacementModesState {
    fun pmMakePlacementModesGUIPart(parentWindow: UIContainer, offset: Float) {}
    fun pmMakePlacementModesNoCenteredInBlockGUIPart(parentWindow: UIContainer, offset: Float) {}
}