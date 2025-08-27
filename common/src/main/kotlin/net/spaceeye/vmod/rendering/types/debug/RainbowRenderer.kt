package net.spaceeye.vmod.rendering.types.debug

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.ShipsColorModulator
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color

class RainbowRenderer(): BaseRenderer(), AutoSerializable {
    private class Data: AutoSerializable {
        var shipId: Long by get(0, -1L)
    }

    constructor(shipId: ShipId): this() {with(data) { this.shipId = shipId }}

    private var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }

    var hue: Float = 0.0f

    override fun renderData(
        poseStack: PoseStack,
        camera: Camera,
        timestamp: Long
    ) {
        ShipsColorModulator.setColor(data.shipId, Color(Color.HSBtoRGB(hue, 1f, 1f)))
        hue += 0.01f
        hue %= 300
    }

    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? { return null }
    override fun scaleBy(by: Double) {  }
}