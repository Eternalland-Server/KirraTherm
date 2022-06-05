package net.sakuragame.eternal.kirratherm

import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import taboolib.common.platform.Plugin
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import taboolib.platform.BukkitPlugin
import java.io.File

@Suppress("SpellCheckingInspection")
object KirraTherm : Plugin() {

    @Config
    lateinit var conf: Configuration
        private set

    val plugin by lazy {
        BukkitPlugin.getInstance()
    }

    val thermFile by lazy {
        val file = File(plugin.dataFolder, "therms.yml")
        if (!file.exists()) {
            file.createNewFile()
        }
        Configuration.loadFromFile(file, Type.YAML)
    }

    override fun onEnable() {
        Profile.profiles.values.forEach {
            it.armorStandEntity?.remove()
        }
    }

    override fun onActive() {
        Bukkit.getWorlds().forEach { 
            it.entities.filterIsInstance(ArmorStand::class.java).forEach {armorStand ->
                if (armorStand.customName != null) {
                    armorStand.remove()
                }
            }
        }
    }
}