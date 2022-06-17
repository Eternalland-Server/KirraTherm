package net.sakuragame.eternal.kirratherm.event

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class PlayerThermGainEvent(val player: Player, private val gainMap: MutableMap<String, Double>) : BukkitProxyEvent() {

    override fun toString() = "PlayerThermGainEvent: player = $player, gainMap = $gainMap"
}