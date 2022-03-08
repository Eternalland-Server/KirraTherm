package net.sakuragame.eternal.kirratherm

import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import net.sakuragame.eternal.kirratherm.Profile.Companion.getProfile
import org.bukkit.entity.Player

fun Player.isCreateMode(): Boolean {
    val profile = getProfile() ?: return false
    return profile.createMode
}

fun Player.getRegion(): String? {
    WorldGuardPlugin.inst().getRegionManager(world).regions.values.forEach {
        if (it.contains(location.x.toInt(), location.y.toInt(), location.z.toInt())) {
            return it.id
        }
    }
    return null
}