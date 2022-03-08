package net.sakuragame.eternal.kirratherm

import net.sakuragame.eternal.kirratherm.therm.Therm
import net.sakuragame.eternal.kirratherm.therm.data.MultipleData
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import taboolib.module.configuration.util.getStringListColored

@Suppress("SpellCheckingInspection")
object KirraThermAPI {

    val thermInterval by lazy {
        KirraTherm.conf.getLong("settings.therm-interval")
    }

    val particleInterval by lazy {
        KirraTherm.conf.getLong("settings.particle-interval")
    }

    val regenInterval by lazy {
        KirraTherm.conf.getLong("regen-interval")
    }

    val actionMessage by lazy {
        KirraTherm.conf.getStringListColored("settings.action-message")
    }

    val multipleMap by lazy {
        // (<权限名>, 列表(<货币名, 倍数>))
        mutableMapOf<String, MutableList<MultipleData>>().also {
            val permissionSections = KirraTherm.conf.getConfigurationSection("settings.multiple-by-permission")?.getKeys(false) ?: return@also
            permissionSections.forEach { section1 ->
                val coinNameSections = KirraTherm.conf.getConfigurationSection("settings.multiple-by-permission.$section1")?.getKeys(false) ?: return@forEach
                coinNameSections.forEach { section2 ->
                    val multipleData = MultipleData(section2, KirraTherm.conf.getDouble("settings.multiple-by-permission.$section1.$section2"))
                    it[section1]?.add(multipleData) ?: kotlin.run { it[section1] = mutableListOf(multipleData) }
                }
            }
        }
    }

    fun generateSitEntity(loc: Location, name: String, player: Player? = null): ArmorStand? {
        return (loc.world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand).also {
            if (!it.isValid) {
                it.remove()
                return null
            }
            it.setGravity(false)
            it.isMarker = true
            it.customName = name
            it.isCustomNameVisible = false
            if (player != null) {
                it.addPassenger(player)
                it.setMetadata(Therm.PLAYER_SEAT_KEY, FixedMetadataValue(KirraTherm.plugin, ""))
                return@also
            }
            it.setMetadata(Therm.STANDALONE_SEAT_KEY, FixedMetadataValue(KirraTherm.plugin, ""))
        }
    }
}