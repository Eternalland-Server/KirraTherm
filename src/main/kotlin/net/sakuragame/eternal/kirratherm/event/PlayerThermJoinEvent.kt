package net.sakuragame.eternal.kirratherm.event

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class PlayerThermJoinEvent(val player: Player, val thermName: String) : BukkitProxyEvent()