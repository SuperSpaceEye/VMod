package net.spaceeye.vmod

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.ToolgunPermissionManager
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.teleportShipWithConnected
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.command.RelativeVector3Argument
import org.valkyrienskies.mod.common.command.ShipArgument
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.mixinducks.feature.command.VSCommandSource

typealias VSCS = CommandContext<VSCommandSource>
typealias MCS = CommandContext<CommandSourceStack>
typealias MCSN = CommandContext<CommandSourceStack?>

object VMCommands {
    private fun lt(name: String) = LiteralArgumentBuilder.literal<CommandSourceStack>(name)
    private fun <T> arg(name: String, type: ArgumentType<T>) = RequiredArgumentBuilder.argument<CommandSourceStack, T>(name, type)

    private var permissionLevel_: Int = 4
    var permissionLevel: Int
        get() = permissionLevel_
        set(value) {
            permissionLevel_ = value
            VMConfig.SERVER.PERMISSIONS.VMOD_COMMANDS_PERMISSION_LEVEL = value
        }

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

    private fun isHoldingToolgun(it: CommandSourceStack): Boolean {
        val player = try {
            it.playerOrException
        } catch (e: Exception) {
            return false
        }

        return player.mainHandItem.item == VMItems.TOOLGUN.get().asItem()
    }


    private fun allowPlayerToUseToolgun(cc: CommandContext<CommandSourceStack>): Int {
        val uuid = EntityArgument.getPlayer(cc, "player").uuid
        ToolgunPermissionManager.allowedPlayersAdd(uuid)
        return 0
    }

    private fun disallowPlayerFromUsingToolgun(cc: CommandContext<CommandSourceStack>): Int {
        val uuid = EntityArgument.getPlayer(cc, "player").uuid
        ToolgunPermissionManager.disallowedPlayersAdd(uuid)
        return 0
    }

    private fun clearPlayerFromSpecialPermission(cc: CommandContext<CommandSourceStack>): Int {
        val uuid = EntityArgument.getPlayer(cc, "player").uuid
        ToolgunPermissionManager.allowedPlayersRemove(uuid)
        ToolgunPermissionManager.disallowedPlayersRemove(uuid)
        return 0
    }

    fun registerServerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
                lt("vmod")
            .requires { it.hasPermission(permissionLevel) || isHoldingToolgun(it) }
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
                        lt("allow-player-to-use-toolgun").then(
                            arg("player", EntityArgument.player()).executes {
                                allowPlayerToUseToolgun(it)
                            }
                        )
                    ).then(
                        lt("disallow-player-from-using-toolgun").then(
                            arg("player", EntityArgument.player()).executes {
                                disallowPlayerFromUsingToolgun(it)
                            }
                        )
                    ).then(
                        lt("clear-player-from-special-permissions").then(
                            arg("player", EntityArgument.player()).executes {
                                clearPlayerFromSpecialPermission(it)
                            }
                        )
                    )
            )
        )
    }
}