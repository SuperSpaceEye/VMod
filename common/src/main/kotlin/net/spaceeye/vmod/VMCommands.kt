package net.spaceeye.vmod

import com.google.common.primitives.Ints.min
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.network.chat.TextComponent
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.debug.DebugRenderer
import net.spaceeye.vmod.schematic.placeAt
import net.spaceeye.vmod.shipAttachments.CustomMassSave
import net.spaceeye.vmod.shipAttachments.GravityController
import net.spaceeye.vmod.shipAttachments.PhysgunController
import net.spaceeye.vmod.shipAttachments.ThrustersController
import net.spaceeye.vmod.shipAttachments.WeightSynchronizer
import net.spaceeye.vmod.toolgun.PlayerAccessManager
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.state.PlayerSchematics
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.teleportShipWithConnected
import net.spaceeye.vmod.utils.vs.traverseGetAllTouchingShips
import net.spaceeye.vmod.utils.vs.traverseGetConnectedShips
import net.spaceeye.vmod.vsStuff.VSGravityManager
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.command.RelativeVector3Argument
import org.valkyrienskies.mod.common.command.ShipArgument
import org.valkyrienskies.mod.common.command.shipWorld
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.mixinducks.feature.command.VSCommandSource
import java.nio.file.Paths
import java.util.UUID
import kotlin.math.max

typealias VSCS = CommandContext<VSCommandSource>
typealias MCS = CommandContext<CommandSourceStack>
typealias MCSN = CommandContext<CommandSourceStack?>

object VMCommands {
    private fun lt(name: String) = LiteralArgumentBuilder.literal<CommandSourceStack>(name)
    private fun <T> arg(name: String, type: ArgumentType<T>) = RequiredArgumentBuilder.argument<CommandSourceStack, T>(name, type)

    var permissionLevel: Int
        get() = VMConfig.SERVER.PERMISSIONS.VMOD_COMMANDS_PERMISSION_LEVEL
        set(value) {
            VMConfig.SERVER.PERMISSIONS.VMOD_COMMANDS_PERMISSION_LEVEL = value
        }
    //do not change
    const val permissionString = "Allow Command Usage"

    init {
        PlayerAccessManager.addPermission(permissionString)
    }

    //TODO this is stupid
    private fun hasPermission(it: CommandSourceStack): Boolean {
        val player = try {
            it.playerOrException
        } catch (e: Exception) {
            return false
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

        val mainShip = ShipArgument.getShip(cc as VSCS, "ship") as ServerShip
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
        val mainShip = ShipArgument.getShip(cc as VSCS, "ship") as ServerShip
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
            cc.source.playerOrException.sendMessage(TextComponent("Failed to save schematic to sever because player has no schematic chosen. Choose schematic with a toolgun and try again."), uuid)
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
        val ships = ShipArgument.getShips(cc as VSCS, "ships")

        val loaded = cc.source.shipWorld.loadedShips

        val x = DoubleArgumentType.getDouble(cc, "x")
        val y = DoubleArgumentType.getDouble(cc, "y")
        val z = DoubleArgumentType.getDouble(cc, "z")

        ships
            .mapNotNull { loaded.getById(it.id) }
            .forEach { GravityController.getOrCreate(it as LoadedServerShip).gravityVector = Vector3d(x, y, z) }

        return 0
    }

    private fun setGravityForConnected(cc: CommandContext<CommandSourceStack>): Int {
        val ships = ShipArgument.getShips(cc as VSCS, "ships")

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
            .forEach { GravityController.getOrCreate(it as LoadedServerShip).gravityVector = Vector3d(x, y, z) }

        return 0
    }

    private fun setGravityForConnectedAndTouching(cc: CommandContext<CommandSourceStack>): Int {
        val ships = ShipArgument.getShips(cc as VSCS, "ships")

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
            .forEach { GravityController.getOrCreate(it as LoadedServerShip).gravityVector = Vector3d(x, y, z) }

        return 0
    }

    private fun resetGravityFor(cc: CommandContext<CommandSourceStack>): Int {
        val ships = ShipArgument.getShips(cc as VSCS, "ships")

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

        player.sendMessage(TextComponent(string), player.uuid)

        return 0
    }

    private object OP {
        fun changeDimensionGravity(cc: CommandContext<CommandSourceStack>): Int {
            val dimension = DimensionArgument.getDimension(cc, "dimension")

            val x = DoubleArgumentType.getDouble(cc, "x")
            val y = DoubleArgumentType.getDouble(cc, "y")
            val z = DoubleArgumentType.getDouble(cc, "z")

            VSGravityManager.setGravity(dimension, Vector3d(x, y, z))

            return 0
        }

        fun clearVmodAttachments(cc: CommandContext<CommandSourceStack>): Int {
            val level = cc.source.level
            level.shipObjectWorld.loadedShips.forEach {
                it.getAttachment(GravityController::class.java)?.let { _ -> it.setAttachment(GravityController::class.java, null) }
                it.getAttachment(PhysgunController::class.java)?.let { _ -> it.setAttachment(PhysgunController::class.java, null) }
                it.getAttachment(ThrustersController::class.java)?.let { _ -> it.setAttachment(ThrustersController::class.java, null) }
                it.getAttachment(CustomMassSave::class.java)?.let { _ -> it.setAttachment(CustomMassSave::class.java, null) }
                it.getAttachment(WeightSynchronizer::class.java)?.let { _ -> it.setAttachment(WeightSynchronizer::class.java, null) }
            }
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
    }

    fun registerServerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            lt("vmod")
            .requires { it.hasPermission(permissionLevel) || hasPermission(it) }
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
                    lt("save-to-sever").then(
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
            ).then(
                lt("gravity")
                .then(
                    lt("set-for").then(
                        arg("ships", ShipArgument.ships()).then(
                            arg("x", DoubleArgumentType.doubleArg()).then(
                                arg("y", DoubleArgumentType.doubleArg()).then(
                                    arg("z", DoubleArgumentType.doubleArg()).executes {
                                        setGravityFor(it)
                                    }
                                )
                            )
                        )
                    )
                ).then(
                    lt("set-for-connected").then(
                        arg("ships", ShipArgument.ships()).then(
                            arg("x", DoubleArgumentType.doubleArg()).then(
                                arg("y", DoubleArgumentType.doubleArg()).then(
                                    arg("z", DoubleArgumentType.doubleArg()).executes {
                                        setGravityForConnected(it)
                                    }
                                )
                            )
                        )
                    )
                ).then(
                    lt("set-for-connected-and-touching").then(
                        arg("ships", ShipArgument.ships()).then(
                            arg("x", DoubleArgumentType.doubleArg()).then(
                                arg("y", DoubleArgumentType.doubleArg()).then(
                                    arg("z", DoubleArgumentType.doubleArg()).executes {
                                        setGravityForConnectedAndTouching(it)
                                    }
                                )
                            )
                        )
                    )
                ).then(
                    lt("reset-for").then(
                        arg("ships", ShipArgument.ships()).executes {
                            resetGravityFor(it)
                        }
                    )
                ).then(
                    lt("reset-for-every-ship-in").then(
                        arg("dimension", DimensionArgument.dimension()).executes {
                            resetGravityForEveryShipIn(it)
                        }
                    )
                )
            ).then(
                lt("op")
                .requires { it.hasPermission(VMConfig.SERVER.PERMISSIONS.VMOD_OP_COMMANDS_PERMISSION_LEVEL) }
                .then(
                    lt("set-command-permission-level").then(
                        arg("level", IntegerArgumentType.integer(0, 4)).executes {
                            permissionLevel = IntegerArgumentType.getInteger(it, "level")
                            0
                        }
                    )
                ).then(
                    lt("set-dimension-gravity").then(
                        arg("dimension", DimensionArgument.dimension()).then(
                            arg("x", DoubleArgumentType.doubleArg()).then(
                                arg("y", DoubleArgumentType.doubleArg()).then(
                                    arg("z", DoubleArgumentType.doubleArg()).executes {
                                        OP.changeDimensionGravity(it)
                                    }
                                )
                            )
                        )
                    )
                ).then(
                    lt("clear-vmod-attachments").executes { OP.clearVmodAttachments(it) }
                )
            ).then(
                lt("debug")
                .requires { it.hasPermission(4) }
                .then(
                    lt("remove-debug-renderers").executes {
                        DEBUG.clearDebugRenderers(it)
                    }
                )
            )
        )
    }
}