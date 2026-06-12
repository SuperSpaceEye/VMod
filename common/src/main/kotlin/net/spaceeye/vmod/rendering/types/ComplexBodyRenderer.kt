package net.spaceeye.vmod.rendering.types

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.vertex.PoseStack
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.valkyrien_ship_schematics.containers.v1.BlockItem
import net.spaceeye.valkyrien_ship_schematics.containers.v1.BlockPaletteHashMapV1
import net.spaceeye.valkyrien_ship_schematics.containers.v1.ChunkyBlockData
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.reflectable.ByteSerializableItem.getMutableList
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.types.special.BakedBlockGhost
import net.spaceeye.vmod.rendering.types.special.BlockGhostBaker
import net.spaceeye.vmod.rendering.types.special.FakeLevel
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.accessByteBufWithCorrectSize
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.core.api.bodies.properties.BodyId
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld
import org.valkyrienskies.core.util.*
import org.valkyrienskies.mod.api.toBlockPos
import org.valkyrienskies.mod.common.shipObjectWorld

class BodyGhostRenderer(
    val bodyId: BodyId,
    val baked: BakedBlockGhost,
    var transparency: Float = 1f
) {
    fun render(poseStack: PoseStack, sources: MultiBufferSource, camera: Camera) {
        val level = Minecraft.getInstance().level!!
        val body = (level.shipObjectWorld as ShipObjectClientWorld).getClientBody(bodyId) ?: return

        val t = body.renderTransform

        poseStack.pushPose()
        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)
        poseStack.translate(t.position.x, t.position.y, t.position.z)
        poseStack.mulPose(t.rotation.get(Quaternionf()))
        poseStack.scale(t.scaling.x.toFloat(), t.scaling.y.toFloat(), t.scaling.z.toFloat())

        // localMatrix in BakedBlockGhost already encodes the -COM offset,
        // so the mesh draws centered on the physics origin.
        baked.render(poseStack, sources, transparency, renderBlockEntities = true)

        poseStack.popPose()
    }
}

class ComplexBodyRenderer(): BlockRenderer(), ReflectableObject {
    companion object {
        init {
            ByteSerializableItem.rsi(BlockPaletteHashMapV1::class, {it, buf ->
                buf.writeInt(it.statePaletteMap.size())
                for (i in 0 until it.statePaletteMap.size()) {
                    val tmp = Unpooled.buffer()
                    NbtIo.writeCompressed(NbtUtils.writeBlockState(it.fromId(i)!!), ByteBufOutputStream(tmp))
                    buf.writeByteArray(tmp.accessByteBufWithCorrectSize())
                }
            }) {buf ->
                val map = BlockPaletteHashMapV1()
                val size = buf.readInt()
                val lookup = BuiltInRegistries.BLOCK.asLookup()
                repeat(size) {
                    val bytes = buf.readByteArray()
                    val tag = NbtIo.readCompressed(ByteBufInputStream(Unpooled.wrappedBuffer(bytes)))
                    val state = NbtUtils.readBlockState(lookup, tag)
                    map.statePaletteMap.add(state) // or whatever mutator your map has
                }
                map
            }
        }
    }

    private class Data: AutoSerializable {
        @JsonIgnore private var i = 0

        var bodyId: Long by get(i++, -1L)
        var positions: MutableList<Vector3i> by getMutableList(i++, mutableListOf(), Vector3i())
        var ids: MutableList<Int> by getMutableList(i++, mutableListOf(), -1)
        var palette: BlockPaletteHashMapV1 by get(i++, BlockPaletteHashMapV1())
        var COM: Vector3d by get(i++, Vector3d())
    }
    private var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }

    private var baked: BodyGhostRenderer? = null
    private var baking = false

    constructor(bodyId: BodyId, items: List<Pair<Vector3i, BlockState>>, COM: Vector3d): this() {
        data.bodyId = bodyId
        data.positions = mutableListOf()
        data.ids = mutableListOf()
        items.forEach { (pos, state) ->
            data.positions.add(pos)
            data.ids.add(data.palette.toId(state))
        }
        data.COM = COM
    }

    override fun renderBlockData(
        poseStack: PoseStack,
        camera: Camera,
        buffer: MultiBufferSource,
        timestamp: Long
    ) {
        val baked = baked ?: run {
            if (baking) return
            baking = true
            Thread {
                val chunky = ChunkyBlockData<BlockItem>()
                val blocks = data.positions.zip(data.ids).map { (pos, id) ->
                    chunky.add(pos.x, pos.y, pos.z, BlockItem(id, -1))
                    pos.toBlockPos()
                }
                val localMatrix = Matrix4f().translate(-data.COM.x.toFloat(), -data.COM.y.toFloat(), -data.COM.z.toFloat())

                val fake = FakeLevel(Minecraft.getInstance().level!!, chunky, emptyList(), data.palette)
                val baked = BlockGhostBaker.bake(fake, blocks, localMatrix)
                Minecraft.getInstance().execute {
                    baked.upload()
                    this.baked = BodyGhostRenderer(data.bodyId, baked)
                }
            }.start()
            return
        }

        baked.render(poseStack, buffer, camera)
    }

    override fun copy(
        oldToNew: Map<ShipId, Ship>,
        centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>
    ): BaseRenderer? {
        TODO("Not yet implemented")
    }

    override fun scaleBy(by: Double) {
        TODO("Not yet implemented")
    }
}