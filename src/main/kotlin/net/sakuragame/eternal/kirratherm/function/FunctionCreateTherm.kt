package net.sakuragame.eternal.kirratherm.function

import net.sakuragame.eternal.kirratherm.isCreateMode
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.Baffle
import taboolib.module.chat.colored
import java.util.concurrent.TimeUnit

object FunctionCreateTherm {

    val selectPoints = ArrayList<Location>().apply { for (i in 1..2) add(emptyLocation) }

    fun isLegalLocations(): Boolean = !selectPoints.contains(emptyLocation)

    val emptyLocation = Bukkit.getWorlds()[0]!!.spawnLocation!!

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
                selectPoints[0] = e.clickedBlock!!.location
                player.sendMessage("&c[System] &7A 点已选中.".colored())
                return
            }
            selectPoints[1] = e.clickedBlock!!.location
            player.sendMessage("&c[System] &7B 点已选中.".colored())
        }
    }
}