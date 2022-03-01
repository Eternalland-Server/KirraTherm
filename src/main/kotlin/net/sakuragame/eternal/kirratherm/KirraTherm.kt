package net.sakuragame.eternal.kirratherm

import taboolib.common.platform.Plugin
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
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
        val file = File(plugin.dataFolder, "coords.yml")
        if (!file.exists()) {
            file.createNewFile()
        }
        Configuration.loadFromFile(file)
    }
}