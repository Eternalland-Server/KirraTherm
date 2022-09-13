package net.sakuragame.eternal.kirratherm

import org.bukkit.Bukkit
import org.bukkit.Location

@Suppress("SpellCheckingInspection")
fun debug(message: String) = KirraTherm.plugin.server.consoleSender.sendMessage("[KirraTherm] $message")

fun Location.parseToString(): String {
    return "${world.name}@$x@$y@$z@$yaw@$pitch"
}

fun String.parseToLoc(): Location? {
    val split = split("@")
    if (split.size != 6) {
        return null
    }
    val world = Bukkit.getWorld(split[0]) ?: return null
    val x = split[1].toDoubleOrNull() ?: return null
    val y = split[2].toDoubleOrNull() ?: return null
    val z = split[3].toDoubleOrNull() ?: return null
    val yaw = split[4].toFloatOrNull() ?: return null
    val pitch = split[5].toFloatOrNull() ?: return null
    return Location(world, x, y, z, yaw, pitch)
}

fun Location.toCenter(offset: Double): Location {
    return Location(world, blockX + offset, blockY.toDouble(), blockZ + offset)
}