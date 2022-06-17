package net.sakuragame.eternal.kirratherm.hook

import net.sakuragame.eternal.kirratherm.Profile.Companion.getProfile
import org.bukkit.entity.Player

@Suppress("SpellCheckingInspection")
class PlaceholderAPI : taboolib.platform.compat.PlaceholderExpansion {

    override val identifier: String
        get() = "KirraTherm"

    override fun onPlaceholderRequest(player: Player?, args: String): String {
        if (player == null) return "__"
        val profile = player.getProfile() ?: return "__"
        when (args.lowercase()) {
            "create_mode" -> return profile.createMode.toString()
            "current_therm" -> return profile.currentArea
            "current_therm_is_seat" -> return (profile.armorStandEntity != null).toString()
        }
        return "__"
    }
}