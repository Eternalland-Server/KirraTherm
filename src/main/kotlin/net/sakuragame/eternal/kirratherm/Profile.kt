package net.sakuragame.eternal.kirratherm

import net.sakuragame.eternal.kirratherm.therm.ThermManager
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent

class Profile(val player: Player) {

    var createMode = false

    var currentArea = ""
    var currentSeat = ""

    var armorStandEntity: ArmorStand? = null

    companion object {

        val profiles = mutableMapOf<String, Profile>()

        fun Player.getProfile() = profiles.values.firstOrNull { it.player.uniqueId == uniqueId }

        @SubscribeEvent(EventPriority.HIGHEST)
        fun e(e: PlayerJoinEvent) {
            Profile(e.player).apply {
                profiles[e.player.name] = this
                read()
            }
        }

        @SubscribeEvent(EventPriority.LOWEST)
        fun e(e: PlayerKickEvent) {
            dataRecycle(e.player)
        }

        @SubscribeEvent(EventPriority.LOWEST)
        fun e(e: PlayerQuitEvent) {
            dataRecycle(e.player)
        }

        private fun dataRecycle(player: Player) {
            player.getProfile()?.apply {
                save()
                remove()
            }
        }
    }

    /**
     * 读取相关信息.
     */
    fun read() {}

    /**
     * 保存相关信息.
     */
    fun save() {
        armorStandEntity?.remove()
    }

    /**
     * 销毁信息.
     */
    fun remove() = profiles.remove(player.name)

    fun getGainMap(): MutableMap<String, Double> {
        return mutableMapOf<String, Double>().apply {
            this += ThermManager.getByName(currentArea)?.gainMap ?: mutableMapOf()
            this += ThermManager.getByName(currentSeat)?.gainMap ?: mutableMapOf()
        }
    }
}
