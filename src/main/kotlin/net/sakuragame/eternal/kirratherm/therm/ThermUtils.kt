package net.sakuragame.eternal.kirratherm.therm

import ink.ptms.zaphkiel.ZaphkielAPI
import net.sakuragame.eternal.kirratherm.KirraThermAPI
import net.sakuragame.eternal.kirratherm.Profile
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submit
import taboolib.platform.util.isAir

fun getRandomDouble() = ((100..150).random() / 100).toDouble()

fun getRandomInt() = (5..10).random()

fun Player.isInArea(locA: Location, locB: Location) = location.isInArea(locA, locB)

fun Player.getBelongPermission() = KirraThermAPI.multipleMap.keys.firstOrNull { it != "default" && hasPermission(it) }

fun Location.isInArea(locA: Location, locB: Location): Boolean {
    return (x - locA.x) * (x - locB.x) <= 0.0 && (y - locA.y) * (y - locB.y) <= 0.0 && (z - locA.z) * (z - locB.z) <= 0.0
}

fun ItemStack.isSeat(): Boolean {
    return getSeatId() != null
}

fun ItemStack.getSeatId(): String? {
    if (isAir()) return null
    val itemStream = ZaphkielAPI.read(this)
    if (itemStream.isVanilla()) {
        return null
    }
    if (itemStream.getZaphkielData().getDeep("fishing.id") == null) {
        return null
    }
    return itemStream.getZaphkielData().getDeep("fishing.id").asString()
}

fun isAlreadySited(name: String) = Profile.profiles.values.firstOrNull { it.currentTherm == name } != null

fun getSitedPlayer(name: String) = Profile.profiles.values.firstOrNull { it.currentTherm == name }?.player

fun getStandaloneEntity(player: Player): ArmorStand? {
    return player
        .getNearbyEntities(2.0, 2.0, 2.0)
        .firstOrNull { it.hasMetadata(Therm.STANDALONE_SEAT_KEY) && getLookingAt(player, it) } as? ArmorStand
}

fun getHollowCube(locA: Location, locB: Location, particleDistance: Double): List<Location> {
    return mutableListOf<Location>().also {
        val world = locA.world
        val minX = locA.x.coerceAtMost(locB.x)
        val minY = locA.y.coerceAtMost(locB.y)
        val minZ = locA.z.coerceAtMost(locB.z)
        val maxX = locA.x.coerceAtLeast(locB.x)
        val maxY = locA.y.coerceAtLeast(locB.y)
        val maxZ = locA.z.coerceAtLeast(locB.z)
        var x = minX
        while (x <= maxX) {
            var y = minY
            while (y <= maxY) {
                var z = minZ
                while (z <= maxZ) {
                    var components = 0
                    if (x == minX || x == maxX) components++
                    if (y == minY || y == maxY) components++
                    if (z == minZ || z == maxZ) components++
                    if (components >= 2) {
                        it.add(Location(world, x, y, z))
                    }
                    z += particleDistance
                }
                y += particleDistance
            }
            x += particleDistance
        }
    }
}

fun runRegenTask(player: Player, therm: Therm) {
    val seat = therm.thermSeat!!
    submit(async = true, period = KirraThermAPI.regenInterval) {
        if (player.isOnline && player.vehicle != null) {
            spawnParticle(player)
            if (seat.regenHeartsPerTicks > 0.0) {
                player.health = (therm.thermSeat.regenHeartsPerTicks + player.health).coerceAtMost(player.maxHealth)
                return@submit
            }
            player.healthScale = (player.healthScale + therm.thermSeat.regenScalePerTicks).coerceAtMost(1.0)
        } else {
            cancel()
            return@submit
        }
    }
}

fun runTotemParticleTask(player: Player) {
    submit(async = true, period = KirraThermAPI.particleInterval) {
        if (Therm.getByLoc(player.location) != null && player.isOnline) {
            player.spawnParticle(Particle.TOTEM, player.location.add(0.0, 1.5, 0.0), getRandomInt())
            return@submit
        }
        cancel()
        return@submit
    }
}

private fun spawnParticle(player: Player) {
    player.world.spawnParticle(Particle.HEART,
        player.location.x,
        player.location.y,
        player.location.z,
        5,
        getRandomDouble(),
        getRandomDouble(),
        getRandomDouble()
    )
}

fun getLookingAt(player: Player, livingEntity: Entity): Boolean {
    val eye = player.eyeLocation
    val toEntity = (livingEntity as LivingEntity).eyeLocation.toVector().subtract(eye.toVector())
    val dot: Double = toEntity.normalize().dot(eye.direction)
    return dot > 0.3
}