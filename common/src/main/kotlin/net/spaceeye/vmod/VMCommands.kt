package net.spaceeye.vmod

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.google.common.primitives.Ints.min
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import dev.architectury.platform.Platform
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.StreamTagVisitor
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.level.chunk.storage.ChunkSerializer
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.mixin.ChunkStorageAccessor
import net.spaceeye.vmod.mixin.IOWorkerAccessor
import net.spaceeye.vmod.mixin.RegionFileStorageAccessor
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.textures.GIFReader
import net.spaceeye.vmod.rendering.textures.WrappedByteArrayInputStream
import net.spaceeye.vmod.rendering.types.debug.DebugRenderer
import net.spaceeye.vmod.schematic.placeAt
import net.spaceeye.vmod.shipAttachments.CustomMassSave
import net.spaceeye.vmod.shipAttachments.GravityController
import net.spaceeye.vmod.shipAttachments.WeightSynchronizer
import net.spaceeye.vmod.toolgun.PlayerAccessManager
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.state.PlayerSchematics
import net.spaceeye.vmod.utils.BlockPos
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import net.spaceeye.vmod.utils.vs.teleportShipWithConnected
import net.spaceeye.vmod.utils.vs.traverseGetAllTouchingShips
import net.spaceeye.vmod.utils.vs.traverseGetConnectedShips
import net.spaceeye.vmod.vsStuff.VSGravityManager
import org.joml.Quaterniond
import org.lwjgl.system.MemoryUtil
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ChunkClaim
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.command.arguments.RelativeVector3Argument
import org.valkyrienskies.mod.common.command.arguments.ShipArgument
import org.valkyrienskies.mod.common.command.shipWorld
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.isChunkInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import java.nio.file.Paths
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.math.max

typealias MCS = CommandContext<CommandSourceStack>
typealias MCSN = CommandContext<CommandSourceStack>

object VMCommands {
    private fun lt(name: String) = LiteralArgumentBuilder.literal<CommandSourceStack>(name)
    private fun <T> arg(name: String, type: ArgumentType<T>) = RequiredArgumentBuilder.argument<CommandSourceStack, T>(name, type)

    //do not change
    const val permissionString = "Allow Command Usage"

    init {
        PlayerAccessManager.addPermission(permissionString, false)
    }

    //TODO this is stupid
    private fun hasPermission(it: CommandSourceStack): Boolean {
        val player = try {
            it.playerOrException
        } catch (e: Exception) {
            return it.hasPermission(4)
        }

        return PlayerAccessManager.hasPermission(player, permissionString)
    }

    private fun findClosestShips(cc: CommandContext<CommandSourceStack>): List<Ship> {
        val dimensionId = cc.source.level.dimensionId
        val pos = cc.source.position.toJOML()

        return cc.source.shipWorld.allShips
            .filter { it.chunkClaimDimension == dimensionId }
            .sortedBy { it.transform.positionInWorld.distanceSquared(pos) }
    }

    //==========//==========//==========//==========//==========//==========//==========//==========//==========//==========

    private fun teleportCommand(cc: CommandContext<CommandSourceStack>): Int {
        val source = cc.source as CommandSourceStack

        val mainShip = ShipArgument.getShip(cc, "ship") as ServerShip
        val position = Vec3Argument.getVec3(cc, "position")
        val dimensionId = (cc.source as CommandSourceStack).level.dimensionId

        val rotation = try {RelativeVector3Argument.getRelativeVector3(cc as MCSN, "euler-angles").toEulerRotation(0.0, 0.0, 0.0)} catch (e: Exception) {mainShip.transform.shipToWorldRotation}
//        val velocity = try {RelativeVector3Argument.getRelativeVector3(cc as MCSN, "velocity").toVector3d(0.0, 0.0, 0.0)} catch (e: Exception) {JVector3d(0.0, 0.0, 0.0)}
//        val angularVelocity = try {RelativeVector3Argument.getRelativeVector3(cc as MCSN, "angular-velocity").toVector3d(0.0, 0.0, 0.0)} catch (e: Exception) {JVector3d(0.0, 0.0, 0.0)}

        teleportShipWithConnected(source.level, mainShip, Vector3d(position), rotation, null, dimensionId)

        return 0
    }

    private fun scaleCommand(cc: CommandContext<CommandSourceStack>): Int {
        val source = cc.source as CommandSourceStack
        val dimensionId = cc.source.level.dimensionId
        val mainShip = ShipArgument.getShip(cc, "ship") as ServerShip
        val scale = DoubleArgumentType.getDouble(cc, "scale")

        teleportShipWithConnected(source.level, mainShip, Vector3d(mainShip.transform.positionInWorld), mainShip.transform.shipToWorldRotation, scale, dimensionId)

        return 0
    }

    private fun vsDeleteMasslessShips(cc: CommandContext<CommandSourceStack>): Int {
        val shipObjectWorld = cc.source.level.shipObjectWorld

        shipObjectWorld.allShips
            .filter { it.inertiaData.mass == 0.0 }
            .forEach { shipObjectWorld.deleteShip(it) }

        return 0
    }

    private fun saveSchemToServer(cc: CommandContext<CommandSourceStack>): Int {
        val name = StringArgumentType.getString(cc, "name")
        val uuid = try { cc.source.playerOrException.uuid } catch (e: Exception) { ELOG("Failed to save schematic to server because user is not a player"); return 1 }

        val schem = PlayerSchematics.schematics[uuid] ?: run {
            cc.source.playerOrException.sendSystemMessage(Component.literal("Failed to save schematic to sever because player has no schematic chosen. Choose schematic with a toolgun and try again."))
            return 1
        }
        PlayerSchematics.saveSchematic(name, schem)

        return 0
    }

    private var lastName = "a"

    private fun loadSchemFromServer(cc: CommandContext<CommandSourceStack>): Int {
        val name = StringArgumentType.getString(cc, "name")
        val uuid = UUID(0L, 0L)

        val schem = PlayerSchematics.loadSchematic(Paths.get("VMod-Schematics/$name")) ?: run {
            ELOG("Failed to load schematic because name doesn't exist.")
            return 1
        }

        PlayerSchematics.schematics[uuid] = schem
        lastName = name

        return 0
    }

    private var placeUUID = UUID(0L, 0L)

    private fun placeServerSchematic(cc: CommandContext<CommandSourceStack>, customName: Boolean): Int {
        val uuid = UUID(0L, 0L)

        val position = Vec3Argument.getVec3(cc, "position")
        val rotation = try {RelativeVector3Argument.getRelativeVector3(cc as MCSN, "rotation").toEulerRotation(0.0, 0.0, 0.0)} catch (e: Exception) {Quaterniond()}
        val name = try {StringArgumentType.getString(cc, "name")} catch (e: Exception) {lastName}

        val schem = PlayerSchematics.schematics[uuid] ?: run {
            ELOG("failed to place schematic because it's null.")
            return 1
        }

        placeUUID = UUID(placeUUID.mostSignificantBits, placeUUID.leastSignificantBits + 1)

        (schem as IShipSchematicDataV1).placeAt(cc.source.level, null, placeUUID, Vector3d(position).toJomlVector3d(), rotation) { ships ->
            if (!customName) {return@placeAt}
            if (ships.size == 1) {
                ships[0].slug = name
                return@placeAt
            }

            ships.forEachIndexed { i, it ->
                it.slug = name + i
            }
        }

        return 0
    }

    private fun setGravityFor(cc: CommandContext<CommandSourceStack>): Int {
        val ships = ShipArgument.getShips(cc, "ships")

        val loaded = cc.source.shipWorld.loadedShips

        val x = DoubleArgumentType.getDouble(cc, "x")
        val y = DoubleArgumentType.getDouble(cc, "y")
        val z = DoubleArgumentType.getDouble(cc, "z")

        ships
            .mapNotNull { loaded.getById(it.id) }
//            .forEach { GravityController.getOrCreate(it as LoadedServerShip).gravityVector = Vector3d(x, y, z) }

        return 0
    }

    private fun setGravityForConnected(cc: CommandContext<CommandSourceStack>): Int {
        val ships = ShipArgument.getShips(cc, "ships")

        val loaded = cc.source.shipWorld.loadedShips

        val traversed = mutableSetOf<ShipId>()
        ships.forEach {
            if (traversed.contains(it.id)) {return@forEach}
            traversed.addAll(traverseGetConnectedShips(it.id, traversed).traversedShipIds)
        }

        val x = DoubleArgumentType.getDouble(cc, "x")
        val y = DoubleArgumentType.getDouble(cc, "y")
        val z = DoubleArgumentType.getDouble(cc, "z")

        traversed
            .mapNotNull { loaded.getById(it) }
//            .forEach { GravityController.getOrCreate(it as LoadedServerShip).gravityVector = Vector3d(x, y, z) }

        return 0
    }

    private fun setGravityForConnectedAndTouching(cc: CommandContext<CommandSourceStack>): Int {
        val ships = ShipArgument.getShips(cc, "ships")

        val loaded = cc.source.shipWorld.loadedShips

        val traversed = mutableSetOf<ShipId>()
        ships.forEach {
            if (traversed.contains(it.id)) {return@forEach}
            traversed.addAll(traverseGetAllTouchingShips((cc as MCS).source.level, it.id, traversed))
        }

        val x = DoubleArgumentType.getDouble(cc, "x")
        val y = DoubleArgumentType.getDouble(cc, "y")
        val z = DoubleArgumentType.getDouble(cc, "z")

        traversed
            .mapNotNull { loaded.getById(it) }
//            .forEach { GravityController.getOrCreate(it as LoadedServerShip).gravityVector = Vector3d(x, y, z) }

        return 0
    }

    private fun resetGravityFor(cc: CommandContext<CommandSourceStack>): Int {
        val ships = ShipArgument.getShips(cc, "ships")

        val loaded = cc.source.shipWorld.loadedShips

        ships
            .mapNotNull { loaded.getById(it.id) }
            .forEach { GravityController.getOrCreate(it as LoadedServerShip).reset() }

        return 0
    }

    private fun resetGravityForEveryShipIn(cc: CommandContext<CommandSourceStack>): Int {
        val loaded = cc.source.shipWorld.loadedShips

        loaded.forEach { GravityController.getOrCreate(it as LoadedServerShip).reset() }

        return 0
    }

    private fun displayShipsInOrder(cc: CommandContext<CommandSourceStack>): Int {
        val player = try { cc.source.playerOrException } catch (e: Exception) {return 1}
        var page = try { IntegerArgumentType.getInteger(cc, "page") - 1 } catch (e: Exception) {0}

        val ordered = findClosestShips(cc)

        page = max(min(page, ordered.size / 10), 0)
        val start = page * 10

        val ships = mutableListOf<Ship>()
        for (i in min(start+10, ordered.size) -1 downTo  start) { ships.add(ordered[i]) }

        var string = ""
        for (ship in ships) {
            val bpos = Vector3d(ship.transform.positionInWorld).toBlockPos()
            string += "${ship.slug} |||| ${ship.transform.positionInWorld.distance(player.x, player.y, player.z).toInt()} |||| x=${bpos.x} y=${bpos.y} z=${bpos.z} \n"
        }

        string += "Page: ${page+1}/${ordered.size/10+1}"

        player.sendSystemMessage(Component.literal(string))

        return 0
    }

    private object OP {
//        fun changeDimensionGravity(cc: CommandContext<CommandSourceStack>): Int {
//            val dimension = DimensionArgument.getDimension(cc, "dimension")
//
//            val x = DoubleArgumentType.getDouble(cc, "x")
//            val y = DoubleArgumentType.getDouble(cc, "y")
//            val z = DoubleArgumentType.getDouble(cc, "z")
//
//            VSGravityManager.setGravity(dimension, Vector3d(x, y, z))
//
//            return 0
//        }

        fun clearVmodAttachments(cc: CommandContext<CommandSourceStack>): Int {
            val level = cc.source.level
            level.shipObjectWorld.loadedShips.forEach {
                it.getAttachment(GravityController::class.java)?.let { _ -> it.setAttachment(GravityController::class.java, null) }
                it.getAttachment(CustomMassSave::class.java)?.let { _ -> it.setAttachment(CustomMassSave::class.java, null) }
                it.getAttachment(WeightSynchronizer::class.java)?.let { _ -> it.setAttachment(WeightSynchronizer::class.java, null) }
            }
            return 0
        }

        //TODO
        fun deletePhysEntities(cc: CommandContext<CommandSourceStack>): Int {
            val level = cc.source.level

//            val entities = (level.shipObjectWorld as ShipObjectWorldAccessor).shipIdToPhysEntity.keys.toList().sorted()
//            entities.forEach {
//                try {
//                    level.shipObjectWorld.deletePhysicsEntity(it)
//                } catch (e: Exception) { ELOG(e.stackTraceToString())
//                } catch (e: Error) { ELOG(e.stackTraceToString()) }
//            }

            return 0
        }

        fun pruneShipyardChunks(cc: CommandContext<CommandSourceStack>): Int { thread(true, true) {
            cc.source.server.allLevels.forEach { level ->
            val path = (((level.chunkSource.chunkMap as ChunkStorageAccessor).`vmod$getWorker`() as IOWorkerAccessor).`vmod$getStorage`() as RegionFileStorageAccessor).`vmod$getPath`()

            if (path.notExists()) return@forEach

            val regionsToCheck = path.listDirectoryEntries()
                .map { it.fileName.name }
                .filter { it.startsWith("r.") && it.endsWith(".mca") }
                .map { it.removePrefix("r.").removeSuffix(".mca") }
                .mapNotNull { try { it.split(".").map { it.toInt() }.let { Pair(it[0], it[1]) } } catch (_: Exception) {null} }
                .map { (x, z) -> Pair(ChunkPos.minFromRegion(x, z), ChunkPos.maxFromRegion(x, z)) }
                .filter { (min, max) -> level.isChunkInShipyard(min.x, min.z) || level.isChunkInShipyard(max.x, max.z) }

            for ((i, bounds) in regionsToCheck.withIndex()) {
                var allUnused = true
                for (cPos in ChunkPos.rangeClosed(bounds.first, bounds.second)) {
                    if (level.getShipManagingPos(cPos) == null) {continue}
                    allUnused = false
                    break
                }
                if (allUnused) {
                    val regX = bounds.first.regionX
                    val regZ = bounds.first.regionZ
                    val path = path.resolve("r.$regX.${regZ}.mca")
                    path.deleteIfExists()
                    DLOG("Dimension: ${level.dimensionId} | Removed region ${bounds.first.regionX}.${bounds.first.regionZ} | ${i+1}/${regionsToCheck.size}")
                } else {
                    DLOG("Dimension: ${level.dimensionId} | Processed ${i+1}/${regionsToCheck.size}")
                }
            }
            } }
            return 0
        }
    }

    private object DEBUG {
        fun clearDebugRenderers(cc: CommandContext<CommandSourceStack>): Int {
            RenderingData.server.allIds.forEach {
                if (RenderingData.server.getRenderer(it) !is DebugRenderer) {return@forEach}
                RenderingData.server.removeRenderer(it)
            }
            return 0
        }

        fun testGIFLoader(cc: CommandContext<CommandSourceStack>): Int {
            val bytes = Minecraft.getInstance().resourceManager.getResourceOrThrow(ResourceLocation(MOD_ID, "textures/gif/test_gif2.gif")).open().readAllBytes()
            thread(isDaemon = true, priority = Thread.MAX_PRIORITY) {
                val numRounds = 25
                val ignoreFirstN = 5

                var totalTime = 0L
                var numFrames = 0
                var numPixels = 0L
                for (i in 0 until numRounds) {
                    ELOG("Started loading round ${i+1} out of $numRounds")
                    val start = getNow_ms()
                    val textures = GIFReader.readGifToTexturesFaster(bytes)
                    val stop = getNow_ms()
                    ELOG("Stopped loading")

                    textures.forEach { it.image.close() }

                    if (i < ignoreFirstN) continue

                    totalTime += stop - start
                    numFrames += textures.fold(0) { acc, it -> acc + it.numFrames }
                    numPixels += textures.fold(0) { acc, it -> acc + it.spriteWidth * it.spriteHeight }
                }

//                for (i in 0 until numRounds) {
//                    ELOG("Started loading round ${i+1} out of $numRounds")
//                    val start = getNow_ms()
//                    val textures = GIFReader.readGIF(ByteBufferBackedInputStream(Unpooled.wrappedBuffer(bytes).nioBuffer()))
//                    val stop = getNow_ms()
//                    ELOG("Stopped loading")
//
//                    if (i < ignoreFirstN) continue
//
//                    totalTime += stop - start
//
//                    numFrames += textures.size
//                    numPixels += textures.size * textures[0].image.width * textures[0].image.height
//                }

                ELOG("\nFrames per second: ${numFrames.toFloat() / totalTime * 1000}" +
                     "\nms per frame: ${totalTime.toFloat() / numFrames}" +
                     "\nPixels per second: ${(numPixels.toFloat() / totalTime * 1000).toInt()}" +
                     "\nProcessed frames: $numFrames")
            }
            return 0
        }
    }

    fun registerServerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            lt("vmod")
            .requires { hasPermission(it) }
            .then(
                lt("teleport").then(
                    arg("ship", ShipArgument.ships()).then(
                        arg("position", Vec3Argument.vec3()).executes { teleportCommand(it) }.then(
                            arg("euler-angles", RelativeVector3Argument.relativeVector3()).executes { teleportCommand(it) }//.then(
//                                arg("velocity", RelativeVector3Argument.relativeVector3()).executes { teleportCommand(it) }.then(
//                                    arg("angular-velocity", RelativeVector3Argument.relativeVector3()).executes { teleportCommand(it) }
//                                )
//                            )
                        )
                    ))
            ).then(
                lt("scale").then(
                    arg("ship", ShipArgument.ships()).then(
                        arg("scale", DoubleArgumentType.doubleArg(ServerLimits.instance.scale.minValue, ServerLimits.instance.scale.maxValue)).executes {
                            scaleCommand(it)
                        }
                    )
                )
            ).then(
                lt("vs-delete-massless-ships").executes { vsDeleteMasslessShips(it) }
            ).then(
                lt("display-ships-in-order").executes { displayShipsInOrder(it) }.then(
                    arg("page", IntegerArgumentType.integer(1)).executes { displayShipsInOrder(it) }
                )
            ).then(
                lt("schem")
                .then(
                    lt("save-to-server").then(
                        arg("name", StringArgumentType.string()).executes { saveSchemToServer(it) }
                    )
                ).then(
                    lt("load-from-sever").then(
                        arg("name", StringArgumentType.string()).executes { loadSchemFromServer(it) }
                    )
                ).then(
                    lt("place").then(
                        arg("position", Vec3Argument.vec3()).executes { placeServerSchematic(it, true) }.then(
                            arg("rotation", RelativeVector3Argument.relativeVector3()).executes { placeServerSchematic(it, true) }.then(
                                arg("name", StringArgumentType.string()).executes { placeServerSchematic(it, true) }
                            )
                        )
                    )
                ).then(
                    lt("place-no-custom-name").then(
                        arg("position", Vec3Argument.vec3()).executes { placeServerSchematic(it, false) }.then(
                            arg("rotation", RelativeVector3Argument.relativeVector3()).executes { placeServerSchematic(it, false) }
                        )
                    )
                )
//            ).then(
//                lt("gravity")
//                .then(
//                    lt("set-for").then(
//                        arg("ships", ShipArgument.ships()).then(
//                            arg("x", DoubleArgumentType.doubleArg()).then(
//                                arg("y", DoubleArgumentType.doubleArg()).then(
//                                    arg("z", DoubleArgumentType.doubleArg()).executes {
//                                        setGravityFor(it)
//                                    }
//                                )
//                            )
//                        )
//                    )
//                ).then(
//                    lt("set-for-connected").then(
//                        arg("ships", ShipArgument.ships()).then(
//                            arg("x", DoubleArgumentType.doubleArg()).then(
//                                arg("y", DoubleArgumentType.doubleArg()).then(
//                                    arg("z", DoubleArgumentType.doubleArg()).executes {
//                                        setGravityForConnected(it)
//                                    }
//                                )
//                            )
//                        )
//                    )
//                ).then(
//                    lt("set-for-connected-and-touching").then(
//                        arg("ships", ShipArgument.ships()).then(
//                            arg("x", DoubleArgumentType.doubleArg()).then(
//                                arg("y", DoubleArgumentType.doubleArg()).then(
//                                    arg("z", DoubleArgumentType.doubleArg()).executes {
//                                        setGravityForConnectedAndTouching(it)
//                                    }
//                                )
//                            )
//                        )
//                    )
//                ).then(
//                    lt("reset-for").then(
//                        arg("ships", ShipArgument.ships()).executes {
//                            resetGravityFor(it)
//                        }
//                    )
//                ).then(
//                    lt("reset-for-every-ship-in").then(
//                        arg("dimension", DimensionArgument.dimension()).executes {
//                            resetGravityForEveryShipIn(it)
//                        }
//                    )
//                )
            ).then(
                lt("op")
                .requires { it.hasPermission(VMConfig.SERVER.PERMISSIONS.VMOD_OP_COMMANDS_PERMISSION_LEVEL) }
//                ).then(
//                    lt("set-dimension-gravity").then(
//                        arg("dimension", DimensionArgument.dimension()).then(
//                            arg("x", DoubleArgumentType.doubleArg()).then(
//                                arg("y", DoubleArgumentType.doubleArg()).then(
//                                    arg("z", DoubleArgumentType.doubleArg()).executes {
//                                        0
////                                        OP.changeDimensionGravity(it)
//                                    }
//                                )
//                            )
//                        )
//                    )
                .then(lt("clear-vmod-attachments").executes { OP.clearVmodAttachments(it) }
//                ).then(lt("delete-phys-entities").executes { OP.deletePhysEntities(it) }
                ).then(lt("prune-shipyard-chunks").executes { OP.pruneShipyardChunks(it) }
                )
            ).also {
                if (!Platform.isDevelopmentEnvironment()) return@also
                it.then(
                    lt("debug")
                        .then(
                            lt("remove-debug-renderers").executes {
                                DEBUG.clearDebugRenderers(it)
                            }
                        ).then(
                            lt("test-gif-loader").executes {
                                DEBUG.testGIFLoader(it)
                            }
                        ).then(
                            lt("call-gc").executes {
                                System.gc()
                                0
                            }
                        )
                )
            }
        )
    }
}