package net.sakuragame.eternal.kirratherm.therm

import net.sakuragame.eternal.justmessage.api.MessageAPI
import net.sakuragame.eternal.kirratherm.KirraTherm
import net.sakuragame.eternal.kirratherm.Profile
import net.sakuragame.eternal.kirratherm.Profile.Companion.getProfile
import net.sakuragame.eternal.kirratherm.debug
import net.sakuragame.eternal.kirratherm.parseToLoc
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal.ThermType.*
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal.ThermType.Companion.isCube
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal.ThermType.Companion.isPlayer
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal.ThermType.Companion.isStandAlone
import net.sakuragame.eternal.kirratherm.therm.data.ThermSeat
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.platform.util.sendLang

@Suppress("SpellCheckingInspection")
data class Therm(val name: String, val data: ThermInternal, val gainMap: MutableMap<String, Double>, val thermSeat: ThermSeat? = null) {

    companion object {

        const val PLAYER_SEAT_KEY = "KIRRATHERM_PLAYER_SEAT"
        const val STANDALONE_SEAT_KEY = "KIRRATHERM_STANDALONE_SEAT"
        const val CUBE_SEAT_KEY = "KIRRATHERM_CUBE_SEAT"

        val therms = mutableListOf<Therm>()

        private val tasks = mutableListOf<PlatformExecutor.PlatformTask>()

        fun getByName(name: String) = therms.find { it.name == name }

        fun getByLoc(loc: Location) = therms.find { it.data.type.isCube() && loc.isInArea(it.data.locA!!, it.data.locB!!) }

        fun getNearestSeatByLoc(loc: Location) = therms.find { it.data.type.isPlayer() && loc.distanceSquared(it.thermSeat?.loc) <= 1.0 }

        @Awake(LifeCycle.ENABLE)
        fun i() {
            clearTherms()
            clearTasks()
            val file = KirraTherm.thermFile
            val sections = file.getConfigurationSection("data") ?: return
            sections.getKeys(false).forEach { section ->
                val type = values().find { it.name == file.getString("data.$it.type") } ?: return@forEach
                val data = ThermInternal(type, null, null)
                val gainMap = mutableMapOf<String, Double>().also {
                    val keys = file.getConfigurationSection("datas.$section.gain")?.getKeys(false) ?: return@also
                    keys.forEach { gainSection ->
                        it[gainSection] = file.getDouble("data.$section.gain.$gainSection")
                    }
                }
                when (data.type) {
                    CUBE -> {
                        data.locA = file.getString("data.$section.loc-a")?.parseToLoc() ?: return@forEach
                        data.locB = file.getString("data.$section.loc-b")?.parseToLoc() ?: return@forEach
                        val therm = Therm(section, data, gainMap, null)
                        debug("已加载温泉: ${therm.name}.")
                        debug("正在加载 ${therm.name} 的特效.")
                        val particleType = Particle.values().find { it.name == file.getString("data.$section.particle.type") } ?: return@forEach
                        val particleCounts = file.getInt("datas.$section.particle.counts")
                        val particleDistance = file.getDouble("datas.$section.particle.distance")
                        val cubeLocs = getHollowCube(data.locA!!, data.locB!!, particleDistance)
                        tasks += submit(async = true, delay = 0L, period = 20L) {
                            cubeLocs.forEach { loc ->
                                Profile.profiles.values.filter { it.player.location.distanceSquared(loc) < 20 * 20 }.forEach {
                                    it.player.spawnParticle(particleType, loc, particleCounts)
                                }
                            }
                        }
                        debug("已开始播放 ${therm.name} 的特效.")
                    }
                    STANDALONE_SEAT, PLAYER_SEAT -> {
                        val delayToRegen = file.getLong("data.$section.delay-to-regen")
                        val regenHeartsPerTicks = file.getDouble("data.$section.regen-hearts-per-ticks")
                        val regenScalePerTicks = file.getDouble("data.$section.regen-scale-per-ticks")
                        val entityName = file.getString("data.$section.entity-name") ?: return@forEach
                        val seatData =
                            if (type.isStandAlone()) {
                                val loc = file.getString("data.$section.loc")?.parseToLoc() ?: return@forEach
                                ThermSeat(loc, entityName, delayToRegen, regenHeartsPerTicks, regenScalePerTicks, null)
                            } else {
                                val itemName = file.getString("data.$section.item-name") ?: return@forEach
                                ThermSeat(null, entityName, delayToRegen, regenHeartsPerTicks, regenScalePerTicks, itemName)
                            }
                        val therm = Therm(section, data, gainMap, seatData)
                        therms += therm
                        debug("已加载温泉: ${therm.name}.")
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


        fun join(player: Player, therm: Therm) {
            val profile = player.getProfile() ?: return
            profile.currentTherm = therm.name
            if (therm.data.type.isPlayer()) {
                // 座椅泡点.
                player.sendLang("message-player-sits-on-seat", profile.currentTherm)
                submit(delay = therm.thermSeat!!.delayToRegen) {
                    runRegenTask(player, therm)
                }
            } else {
                // 区域泡点.
                player.sendLang("message-player-join-therm", profile.currentTherm)
                runTotemParticleTask(player)
            }
        }

        fun left(player: Player, isSeat: Boolean = false) {
            val profile = player.getProfile() ?: return
            when (isSeat) {
                true -> player.sendLang("message-player-left-seat", profile.currentTherm)
                else -> player.sendLang("message-player-quit-therm", profile.currentTherm)
            }
            profile.currentTherm = ""
            MessageAPI.removeCrossHairTip(player)
        }
    }
}
