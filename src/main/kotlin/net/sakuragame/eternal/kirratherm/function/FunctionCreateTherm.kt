package net.sakuragame.eternal.kirratherm.function

import net.sakuragame.eternal.kirratherm.isCreateMode
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.Baffle
import java.util.concurrent.TimeUnit

object FunctionCreateTherm {

    val selectPointsList = ArrayList<Location>().apply { for (i in 1..2) add(emptyLocation()) }

    fun isLegalLocations(): Boolean = !selectPointsList.contains(emptyLocation())

    private fun emptyLocation(): Location = Bukkit.getWorlds()[0]!!.spawnLocation

    private val baffle by lazy {
        Baffle.of(3, TimeUnit.MILLISECONDS)
    }

    @SubscribeEvent
    fun e(e: PlayerInteractEvent) {
        val player = e.player
        if (player.isCreateMode() && e.action != Action.PHYSICAL && e.clickedBlock != null) {
            e.isCancelled = true
            if (!baffle.hasNext(player.name)) return
            baffle.next(player.name)
            if (e.action == Action.LEFT_CLICK_BLOCK || e.action == Action.LEFT_CLICK_AIR) {
                selectPointsList[0] = e.clickedBlock!!.location
                player.sendMessage("&c[System] &7A 点已选中.")
                return
            }
            selectPointsList[1] = e.clickedBlock!!.location
            player.sendMessage("&c[System] &7B 点已选中.")
        }
    }
}