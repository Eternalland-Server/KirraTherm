package net.sakuragame.eternal.kirratherm

import net.sakuragame.eternal.kirratherm.Profile.Companion.getProfile
import org.bukkit.entity.Player

fun Player.isCreateMode(): Boolean {
    val profile = getProfile() ?: return false
    return profile.createMode
}