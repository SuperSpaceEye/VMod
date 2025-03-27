package net.spaceeye.vmod.vEntityManaging.legacy

import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISerializable
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.rendering.RenderingTypes
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.ServerLevelHolder
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getMyVector3d
import net.spaceeye.vmod.utils.getQuatd
import net.spaceeye.vmod.vEntityManaging.LEGACY_SAVE_TAG_NAME
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.VEntityManager
import net.spaceeye.vmod.vEntityManaging.extensions.ConvertedFromLegacy
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.extensions.SignalActivator
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.vEntityManaging.types.constraints.ConnectionConstraint
import net.spaceeye.vmod.vEntityManaging.types.constraints.DisabledCollisionConstraint
import net.spaceeye.vmod.vEntityManaging.types.constraints.HydraulicsConstraint
import net.spaceeye.vmod.vEntityManaging.types.constraints.RopeConstraint
import net.spaceeye.vmod.vEntityManaging.types.constraints.SyncRotationConstraint
import net.spaceeye.vmod.vEntityManaging.types.entities.ThrusterVEntity
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color
import kotlin.math.max

object LegacyConstraintFixers {
    @JvmStatic fun tryFixRenderer(tag: CompoundTag): BaseRenderer? {
        try {
            val extensions = tag.getCompound("Extensions")
            if (!extensions.contains("RenderableExtension")) {return null}
            val rtag = extensions.getCompound("RenderableExtension")
            val strType = rtag.getString("rendererType")
            return when(strType) {
                "RopeRenderer" -> RenderingTypes.strTypeToSupplier(strType).get().also { it.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(rtag.getByteArray("renderer")))) }
                "A2BRenderer"  -> RenderingTypes.strTypeToSupplier(strType).get().also { it.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(rtag.getByteArray("renderer")))) }
                "ConeBlockRenderer" -> RenderingTypes.strTypeToSupplier(strType).get().also {
                    val byteArray = rtag.getByteArray("renderer").toMutableList()

                    byteArray.addAll(Unpooled.buffer().writeInt(Color(255, 255, 255).rgb).array().toList())

                    it.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(byteArray.toByteArray())))
                }
                else -> throw NotImplementedError("Unhandled type $strType")
            }
        } catch (e: Exception) { ELOG(e.stackTraceToString())
        } catch (e: Error    ) { ELOG(e.stackTraceToString())}
        return null
    }

    @OptIn(ExperimentalStdlibApi::class)
    @JvmStatic fun fixConnection(tag: CompoundTag, oldToNew: Map<ShipId, ShipId>): VEntity {
        val main = tag.getCompound("Main")
        val mode = ConnectionConstraint.ConnectionModes.entries[main.getInt("mode")]

        val c1 = main.getCompound("c1")
        val sPos1 = c1.getMyVector3d("localPos0")
        val sPos2 = c1.getMyVector3d("localPos1")
        val shipId1 = c1.getLong("shipId0").let { oldToNew[it] ?: it }
        val shipId2 = c1.getLong("shipId1").let { oldToNew[it] ?: it }
        val distance = c1.getDouble("fixedDistance").toFloat()

        val (sRot1, sRot2) = when(mode) {
            ConnectionConstraint.ConnectionModes.FIXED_ORIENTATION -> {
                val c3 = main.getCompound("c3")

                val sRot1 = c3.getQuatd("localRot0")!!.invert()
                val sRot2 = c3.getQuatd("localRot1")!!.invert()
                sRot1 to sRot2
            }
            ConnectionConstraint.ConnectionModes.HINGE_ORIENTATION -> Quaterniond() to Quaterniond()
            ConnectionConstraint.ConnectionModes.FREE_ORIENTATION -> Quaterniond() to Quaterniond()
        }

        val convertFn = {
            val c2 = main.getCompound("c2")

            val sPos12 = c2.getMyVector3d("localPos0")
            val sPos22 = c2.getMyVector3d("localPos1")

            val sDir1 = sPos1 - sPos12
            val sDir2 = sPos22 - sPos2

            sDir1.normalize() to sDir2.normalize()
        }

        val (sDir1, sDir2) = when(mode) {
            ConnectionConstraint.ConnectionModes.FIXED_ORIENTATION -> convertFn()
            ConnectionConstraint.ConnectionModes.HINGE_ORIENTATION -> convertFn()
            ConnectionConstraint.ConnectionModes.FREE_ORIENTATION -> Vector3d() to Vector3d()
        }

        val renderer = tryFixRenderer(tag)

        return ConnectionConstraint(
            sPos1, sPos2, sDir1, sDir2, sRot1, sRot2, shipId1, shipId2, -1f, -1f, -1f, distance, mode
        )
            .addExtension(Strippable())
            .addExtension(ConvertedFromLegacy())
            .also {it.mID = tag.getInt("mID")}
            .also { ventity -> renderer?.let { ventity.addExtension(RenderableExtension(it)) } }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @JvmStatic fun fixHydraulics(tag: CompoundTag, oldToNew: Map<ShipId, ShipId>): VEntity {
        val signalActivator = tag.getCompound("Extensions").getCompound("SignalActivator")
        val percentageNameReflection = signalActivator.getString("percentageNameReflection")
        val channelNameReflection = signalActivator.getString("channelNameReflection")

        val main = tag.getCompound("Main")
        val mode = HydraulicsConstraint.ConnectionMode.entries[main.getInt("constraintMode")]
        val minDistance = main.getDouble("minDistance").toFloat()
        val maxDistance = main.getDouble("maxDistance").toFloat()
        val extendedDist = main.getDouble("extendedDist").toFloat()
        val extensionSpeed = main.getDouble("extensionSpeed").toFloat()
        val channel = main.getString("channel")

        val c1 = main.getCompound("c1")
        val sPos1 = c1.getMyVector3d("localPos0")
        val sPos2 = c1.getMyVector3d("localPos1")
        val shipId1 = c1.getLong("shipId0").let { oldToNew[it] ?: it }
        val shipId2 = c1.getLong("shipId1").let { oldToNew[it] ?: it }

        val convertFn = {
            val c2 = main.getCompound("c2")

            val sPos12 = c2.getMyVector3d("localPos0")
            val sPos22 = c2.getMyVector3d("localPos1")

            val sDir1 = sPos1 - sPos12
            val sDir2 = sPos22 - sPos2

            sDir1.normalize() to sDir2.normalize()
        }

        val (sDir1, sDir2) = when(mode) {
            HydraulicsConstraint.ConnectionMode.FIXED_ORIENTATION -> convertFn()
            HydraulicsConstraint.ConnectionMode.HINGE_ORIENTATION -> convertFn()
            HydraulicsConstraint.ConnectionMode.FREE_ORIENTATION -> Vector3d() to Vector3d()
        }

        val (sRot1, sRot2) = when(mode) {
            HydraulicsConstraint.ConnectionMode.FIXED_ORIENTATION -> {
                val c3 = main.getCompound("c3")

                val sRot1 = c3.getQuatd("localRot0")!!.invert()
                val sRot2 = c3.getQuatd("localRot1")!!.invert()
                sRot1 to sRot2
            }
            HydraulicsConstraint.ConnectionMode.HINGE_ORIENTATION -> Quaterniond() to Quaterniond()
            HydraulicsConstraint.ConnectionMode.FREE_ORIENTATION -> Quaterniond() to Quaterniond()
        }

        val renderer = tryFixRenderer(tag)

        return HydraulicsConstraint(
            sPos1, sPos2, sDir1, sDir2, sRot1, sRot2, shipId1, shipId2, -1f, -1f, -1f,
            minDistance, maxDistance, extensionSpeed, channel, mode
        )
            .also { it.extendedDist = extendedDist }
            .addExtension(Strippable())
            .addExtension(ConvertedFromLegacy())
            .addExtension(SignalActivator(channelNameReflection, percentageNameReflection))
            .also {it.mID = tag.getInt("mID")}
            .also { ventity -> renderer?.let { ventity.addExtension(RenderableExtension(it)) } }
    }

    @JvmStatic fun fixRope(tag: CompoundTag, oldToNew: Map<ShipId, ShipId>): VEntity {
        val main = tag.getCompound("Main")

        val sPos1 = main.getMyVector3d("localPos0")
        val sPos2 = main.getMyVector3d("localPos1")
        val shipId1 = main.getLong("shipId0").let { oldToNew[it] ?: it }
        val shipId2 = main.getLong("shipId1").let { oldToNew[it] ?: it }

        val ropeLength = main.getDouble("ropeLength").toFloat()

        val renderer = tryFixRenderer(tag)

        return RopeConstraint(
            sPos1, sPos2, shipId1, shipId2, -1f, -1f, -1f, ropeLength
        )
            .addExtension(Strippable())
            .addExtension(ConvertedFromLegacy())
            .also { it.mID = tag.getInt("mID") }
            .also { ventity -> renderer?.let { ventity.addExtension(RenderableExtension(it)) } }

    }

    @JvmStatic fun fixSyncRotation(tag: CompoundTag, oldToNew: Map<ShipId, ShipId>): VEntity {
        val main = tag.getCompound("Main")

        val c1 = main.getCompound("c1")
        val sRot1 = c1.getQuatd("localRot0")!!
        val sRot2 = c1.getQuatd("localRot1")!!

        val shipId1 = c1.getLong("shipId0").let { oldToNew[it] ?: it }
        val shipId2 = c1.getLong("shipId1").let { oldToNew[it] ?: it }

        return SyncRotationConstraint(
            sRot1, sRot2, shipId1, shipId2, -1f
        )
            .addExtension(Strippable())
            .addExtension(ConvertedFromLegacy())
            .also { it.mID = tag.getInt("mID") }
    }

    @JvmStatic fun fixThruster(tag: CompoundTag, oldToNew: Map<ShipId, ShipId>): VEntity {
        val signalActivator = tag.getCompound("Extensions").getCompound("SignalActivator")
        val percentageNameReflection = signalActivator.getString("percentageNameReflection")
        val channelNameReflection = signalActivator.getString("channelNameReflection")

        val main = tag.getCompound("Main")
        val shipId = main.getLong("shipId").let { oldToNew[it] ?: it }
        val pos = main.getMyVector3d("pos")
        val forceDir = main.getMyVector3d("forceDir")
        val channel = main.getString("channel")
        val force = main.getDouble("force")

        val renderer = tryFixRenderer(tag)

        return ThrusterVEntity(
            shipId, pos, forceDir, force, channel
        ).addExtension(Strippable())
            .addExtension(ConvertedFromLegacy())
            .addExtension(SignalActivator(channelNameReflection, percentageNameReflection))
            .also { it.mID = tag.getInt("mID") }
            .also { ventity -> renderer?.let { ventity.addExtension(RenderableExtension(it)) } }
    }

    @JvmStatic fun fixDisabledCollisions(tag: CompoundTag, oldToNew: Map<ShipId, ShipId>): VEntity {
        return DisabledCollisionConstraint(
            tag.getLong("shipId1").let { oldToNew[it] ?: it },
            tag.getLong("shipId2").let { oldToNew[it] ?: it }
        )
            .addExtension(Strippable())
            .addExtension(ConvertedFromLegacy())
            .also { it.mID = tag.getInt("managedId") }

    }

    @JvmStatic fun tryUpdateMConstraint(strType: String, tag: CompoundTag, oldToNew: Map<ShipId, ShipId>): VEntity {
        return when (strType) {
            "ConnectionMConstraint" -> fixConnection(tag, oldToNew)
            "HydraulicsMConstraint" -> fixHydraulics(tag, oldToNew)
            "RopeMConstraint" -> fixRope(tag, oldToNew)
            "SyncRotationMConstraint" -> fixSyncRotation(tag, oldToNew)
            "ThrusterMConstraint" -> fixThruster(tag, oldToNew)
            "DisabledCollisionMConstraint" -> fixDisabledCollisions(tag, oldToNew)
            else -> throw NotImplementedError("Conversion not implemented")
        }
    }

    @JvmStatic fun tryLoadLegacyVModSchemData(level: ServerLevel, ships: List<Pair<ServerShip, Long>>, data: Map<String, ISerializable>) {
        val str = "VMod Constraint Manager"

        val file = CompoundTagSerializable(CompoundTag())
        file.deserialize(data[str]?.serialize() ?: return)
        val instance = VEntityManager.getInstance()

        val tag = file.tag!!
        val toInitConstraints = mutableListOf<VEntity>()

        val lastDimensionIds = instance.loadDimensionIds(tag)
        val newDimensionIds = ServerLevelHolder.shipObjectWorld!!.dimensionToGroundBodyIdImmutable
        val oldToNew = lastDimensionIds.map { Pair(it.key, newDimensionIds[it.value]!!) }.toMap()

        val shipsTag = tag[LEGACY_SAVE_TAG_NAME]!! as CompoundTag

        var count = 0
        var maxId = -1
        for (shipId in shipsTag.allKeys) {
            val shipConstraintsTag = shipsTag[shipId]!! as ListTag
            val constraints = mutableListOf<VEntity>()
            var strType = "None"
            for (ctag in shipConstraintsTag) {
                try {
                    ctag as CompoundTag
                    strType = ctag.getString("MConstraintType")

                    val vEntity = tryUpdateMConstraint(strType, ctag, oldToNew)

                    maxId = max(maxId, vEntity.mID)
                    constraints.add(vEntity)
                    count++
                } catch (e: Exception) { ELOG("Failed to update constraint of type $strType\n${e.stackTraceToString()}")
                } catch (e: Error    ) { ELOG("Failed to update constraint of type $strType\n${e.stackTraceToString()}")}
            }
            toInitConstraints.addAll(constraints)
        }

        val mapped = ships.associate {
            if (lastDimensionIds.containsKey(it.second)) {
                Pair(level.shipObjectWorld.dimensionToGroundBodyIdImmutable[lastDimensionIds[it.second]]!!, it.first.id)
            } else {
                Pair(it.second, it.first.id)
            }
        }

        val changedIds = mutableMapOf<Int, Int>()
        for (it in toInitConstraints) {
            level.makeVEntity(it.copyVEntity(level, mapped) ?: continue) {newId ->
                changedIds[it.mID] = newId ?: return@makeVEntity
            }
        }
    }
}