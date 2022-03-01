package net.sakuragame.eternal.kirratherm

import net.sakuragame.eternal.kirratherm.Profile.Companion.getProfile
import net.sakuragame.eternal.kirratherm.function.FunctionCreateTherm
import net.sakuragame.eternal.kirratherm.function.FunctionCreateTherm.selectPointsList
import net.sakuragame.eternal.kirratherm.therm.Therm
import net.sakuragame.eternal.kirratherm.therm.ThermAPI
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.event.SubscribeEvent
import taboolib.expansion.createHelper
import taboolib.module.chat.colored

@Suppress("SpellCheckingInspection")
@CommandHeader(name = "KirraTherm", aliases = ["therm"])
object Commands {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val mode = subCommand {
        execute<Player> { player, _, _ ->
            val profile = player.getProfile() ?: return@execute
            profile.createMode = !profile.createMode
            player.sendMessage("&c[System] &7您的模式已被切换为: ${profile.createMode}".colored())
        }
    }

    @CommandBody
    val create = subCommand {
        dynamic(commit = "type") {
            dynamic(commit = "name") {
                execute<Player> { player, context, _ ->
                    val type = when (context.get(1).lowercase()) {
                        "cube" -> ThermInternal.ThermType.CUBE
                        "standalone" -> ThermInternal.ThermType.STANDALONE_SEAT
                        "player" -> ThermInternal.ThermType.PLAYER_SEAT
                        else -> {
                            player.sendMessage("&c[System] &7错误的类型.".colored())
                            return@execute
                        }
                    }
                    val data = if (type == ThermInternal.ThermType.PLAYER_SEAT) {
                        ThermInternal(ThermInternal.ThermType.PLAYER_SEAT, null, null)
                    } else {
                        if (!FunctionCreateTherm.isLegalLocations()) {
                            player.sendMessage("&c[System] &7请选择点的坐标.".colored())
                            return@execute
                        }
                        ThermInternal(type, selectPointsList[0], selectPointsList[1])
                    }
                    val name = context.get(2)
                    if (Therm.getByName(name) != null) {
                        player.sendMessage("&c[System] &7该点位已存在.".colored())
                        return@execute
                    }
                    ThermAPI.saveAs(name, data)
                }
            }
        }
    }

    @CommandBody
    val list = subCommand {
        execute<Player> { player, _, _ ->
            player.sendMessage("&c[System] &7当前存在的点位列表: ".colored())
            Therm.therms.forEach {
                player.sendMessage("&c[System] &7$it".colored())
            }
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<Player> { player, _, _ ->
            Therm.i()
            player.sendMessage("&c[System] &7重载完成.".colored())
        }
    }
}