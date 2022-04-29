package net.sakuragame.eternal.kirratherm.therm

import net.sakuragame.eternal.justmessage.api.MessageAPI
import net.sakuragame.eternal.kirratherm.KirraTherm
import net.sakuragame.eternal.kirratherm.Profile
import net.sakuragame.eternal.kirratherm.Profile.Companion.getProfile
import net.sakuragame.eternal.kirratherm.debug
import net.sakuragame.eternal.kirratherm.event.PlayerThermJoinEvent
import net.sakuragame.eternal.kirratherm.event.PlayerThermQuitEvent
import net.sakuragame.eternal.kirratherm.parseToLoc
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal.ThermType.Companion.isCube
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal.ThermType.Companion.isPlayer
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal.ThermType.Companion.isStandAlone
import net.sakuragame.eternal.kirratherm.therm.data.ThermSeat
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.platform.util.asLangText
import taboolib.platform.util.sendLang

@Suppress("SpellCheckingInspection")
data class Therm(val name: String, val data: ThermInternal, val gainMap: MutableMap<String, Double>, val thermSeat: ThermSeat? = null) {

    companion object {

        const val PLAYER_SEAT_KEY = "KIRRATHERM_PLAYER_SEAT"
        const val STANDALONE_SEAT_KEY = "KIRRATHERM_STANDALONE_SEAT"
        const val CUBE_SEAT_KEY = "KIRRATHERM_CUBE_SEAT"

        private val therms = mutableListOf<Therm>()

        private val tasks = mutableListOf<PlatformExecutor.PlatformTask>()

        fun getAll() = therms

        fun getByName(name: String) = therms.find { it.name == name }

        fun getByLoc(loc: Location) = therms.find { it.data.type.isCube() && loc.isInArea(it.data.locA!!, it.data.locB!!) }

        fun getNearestSeatByLoc(loc: Location) = therms.find { it.data.type.isPlayer() && loc.world == it.thermSeat?.loc?.world && loc.distanceSquared(it.thermSeat?.loc) <= 1.0 }

        @Awake(LifeCycle.ENABLE)
        fun i() {
            clearTherms()
            clearTasks()
            clearEntities()
            val file = KirraTherm.thermFile
            val sections = file.getConfigurationSection("data") ?: return
            sections.getKeys(false).forEach { section ->
                val type = ThermInternal.ThermType.values().find { it.name == file.getString("data.$section.type") } ?: return@forEach
                val data = ThermInternal(type, null, null)
                val gainMap = mutableMapOf<String, Double>().also {
                    val keys = file.getConfigurationSection("data.$section.gain")?.getKeys(false) ?: return@also
                    keys.forEach { gainSection ->
                        it[gainSection] = file.getDouble("data.$section.gain.$gainSection")
                    }
                }
                when (data.type) {
                    ThermInternal.ThermType.CUBE -> {
                        data.locA = file.getString("data.$section.loc-a")?.parseToLoc() ?: return@forEach
                        data.locB = file.getString("data.$section.loc-b")?.parseToLoc() ?: return@forEach
                        val therm = Therm(section, data, gainMap, null)
                        therms += therm
                        debug("已加载温泉: ${therm.name}.")
                        val particleType = Particle.values().find { it.name == file.getString("data.$section.particle.type") } ?: return@forEach
                        val particleCounts = file.getInt("data.$section.particle.counts")
                        val particleDistance = file.getDouble("data.$section.particle.distance")
                        val cubeLocs = getHollowCube(data.locA!!, data.locB!!, particleDistance)
                        tasks += submit(async = true, delay = 0L, period = 20L) {
                            cubeLocs.forEach { loc ->
                                Profile.profiles.values
                                    .filter { it.player.world == loc.world }
                                    .filter { it.player.location.distanceSquared(loc) < 20 * 20 }
                                    .forEach {
                                    it.player.spawnParticle(particleType, loc, particleCounts, 0.0, 0.0, 0.0, 0.02)
                                }
                            }
                        }
                        debug("已开始播放 ${therm.name} 的特效.")
                    }
                    ThermInternal.ThermType.STANDALONE_SEAT, ThermInternal.ThermType.PLAYER_SEAT -> {
                        val delayToRegen = file.getLong("data.$section.delay-to-regen")
                        val regenHeartsPerTicks = file.getDouble("data.$section.regen-hearts-per-ticks")
                        val regenScalePerTicks = file.getDouble("data.$section.regen-scale-per-ticks")
                        val entityName = file.getString("data.$section.entity-name") ?: return@forEach
                        val seatData =
                            if (type.isStandAlone()) {
                                val loc = file.getString("data.$section.loc")?.parseToLoc() ?: return@forEach
                                ThermSeat(loc, entityName, delayToRegen, regenHeartsPerTicks, regenScalePerTicks)
                            } else {
                                ThermSeat(null, entityName, delayToRegen, regenHeartsPerTicks, regenScalePerTicks)
                            }
                        val therm = Therm(section, data, gainMap, seatData)
                        therms += therm
                        debug("已加载座椅: ${therm.name}.")
                    }
                }
            }
        }

        private fun clearTherms() = therms.clear()

        private fun clearTasks() {
            tasks.forEach {
                it.cancel()
            }
            tasks.clear()
        }

        private fun clearEntities() {
            Bukkit.getWorlds().forEach { world ->
                world.entities
                    .filter { it.type == EntityType.ARMOR_STAND }
                    .filter { it.hasMetadata(STANDALONE_SEAT_KEY) || it.hasMetadata(PLAYER_SEAT_KEY) }
                    .forEach {
                    it.remove()
                }
            }
        }

        fun join(player: Player, therm: Therm) {
            val profile = player.getProfile() ?: return
            profile.currentTherm = therm.name
            if (therm.data.type.isPlayer()) {
                // 座椅泡点.
                MessageAPI.sendActionTip(player, player.asLangText("message-player-sits-on-seat", profile.currentTherm))
                submit(delay = therm.thermSeat!!.delayToRegen) {
                    runRegenTask(player, therm)
                }
            } else {
                // 区域泡点.
                MessageAPI.sendActionTip(player, player.asLangText("message-player-join-therm", profile.currentTherm))
                runTotemParticleTask(player)
            }
            PlayerThermJoinEvent(player, therm.name).call()
        }

        fun left(player: Player, isSeat: Boolean = false) {
            val profile = player.getProfile() ?: return
            PlayerThermQuitEvent(player, profile.currentTherm).call()
            val message = when (isSeat) {
                true -> player.asLangText("message-player-left-seat", profile.currentTherm)
                else -> player.asLangText("message-player-quit-therm", profile.currentTherm)
            }
            MessageAPI.sendActionTip(player, message)
            profile.currentTherm = ""
            MessageAPI.removeCrossHairTip(player)
        }
    }
}
