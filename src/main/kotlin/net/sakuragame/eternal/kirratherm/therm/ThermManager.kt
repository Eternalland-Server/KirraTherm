package net.sakuragame.eternal.kirratherm.therm

import net.sakuragame.eternal.justmessage.api.MessageAPI
import net.sakuragame.eternal.kirratherm.KirraTherm
import net.sakuragame.eternal.kirratherm.KirraThermAPI
import net.sakuragame.eternal.kirratherm.Profile.Companion.getProfile
import net.sakuragame.eternal.kirratherm.debug
import net.sakuragame.eternal.kirratherm.event.PlayerThermQuitEvent
import net.sakuragame.eternal.kirratherm.parseToLoc
import net.sakuragame.eternal.kirratherm.therm.data.ThermType.*
import net.sakuragame.eternal.kirratherm.therm.data.sub.RegenType
import net.sakuragame.eternal.kirratherm.therm.impl.CubeTherm
import net.sakuragame.eternal.kirratherm.therm.impl.PlayerSeatTherm
import net.sakuragame.eternal.kirratherm.therm.impl.StandaloneSeatTherm
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.chat.colored
import taboolib.module.configuration.util.getStringColored
import taboolib.platform.util.asLangText

@Suppress("SpellCheckingInspection")
object ThermManager {

    const val PLAYER_SEAT_KEY = "KIRRATHERM_PLAYER_SEAT"
    const val STANDALONE_SEAT_KEY = "KIRRATHERM_STANDALONE_SEAT"

    val therms = mutableListOf<ITherm>()

    val tasks = mutableListOf<PlatformExecutor.PlatformTask>()

    inline fun <reified T : ITherm> getByType(): List<T> {
        return therms.filterIsInstance(T::class.java)
    }

    fun getByName(name: String) = therms.find { it.id == name }

    fun getByLoc(loc: Location): CubeTherm? {
        val therms = therms.mapNotNull { it as? CubeTherm }
        return therms.find { loc.isInArea(it.locationA, it.locationB) }
    }

    fun getNearestSeatByLoc(loc: Location): ITherm? {
        return therms.mapNotNull { it as? StandaloneSeatTherm }.filter { it.spawnLocation.world == loc.world }.find { loc.distanceSquared(it.entity?.location) <= 1.0 }
    }

    @Awake(LifeCycle.ENABLE)
    fun i() {
        recycle()
        val file = KirraTherm.thermFile
        val sections = file.getConfigurationSection("data")?.getKeys(false) ?: return
        sections.forEach { id ->
            val type = values().find { it.name == file.getString("data.$id.type") } ?: return@forEach
            val gainMap = mutableMapOf<String, Double>().also {
                val keys = file.getConfigurationSection("data.$id.gain")?.getKeys(false) ?: return@also
                keys.forEach { gainSection ->
                    it[gainSection] = file.getDouble("data.$id.gain.$gainSection")
                }
            }
            val delayToRegen = file.getLong("data.$id.delay-to-regen")
            val regenType = RegenType.values().find { it.name.equals(file.getString("data.$id.regen-type"), true) } ?: return@forEach
            val regenValue = file.getDouble("data.$id.regen-value")
            when (type) {
                CUBE -> {
                    val locA = file.getString("data.$id.loc-a")?.parseToLoc() ?: return@forEach
                    val locB = file.getString("data.$id.loc-b")?.parseToLoc() ?: return@forEach
                    val particleType = Particle.values().find { it.name == file.getString("data.$id.particle.type") } ?: return@forEach
                    val particleCounts = file.getInt("data.$id.particle.counts")
                    val particleDistance = file.getDouble("data.$id.particle.distance")
                    val therm = CubeTherm(
                        id = id,
                        gainMap = gainMap,
                        locationA = locA,
                        locationB = locB,
                        particleType = particleType,
                        particleCounts = particleCounts,
                        particleDistance = particleDistance,
                        regenType = regenType,
                        delayToRegen = delayToRegen,
                        regenValue = regenValue
                    )
                    therms += therm
                    debug("已开始播放 ${therm.id} 的特效.")
                }
                STANDALONE_SEAT, PLAYER_SEAT -> {
                    val entityName = file.getStringColored("data.$id.entity-name") ?: return@forEach
                    val therm = when (type == STANDALONE_SEAT) {
                        false -> {
                            val itemId = file.getString("data.$id.item-id") ?: return@forEach
                            PlayerSeatTherm(
                                id = id,
                                gainMap = gainMap,
                                entityName = entityName,
                                itemId = itemId,
                                regenType = regenType,
                                delayToRegen = delayToRegen,
                                regenValue = regenValue
                            )
                        }
                        true -> {
                            val loc = file.getString("data.$id.loc")?.parseToLoc() ?: return@forEach
                            StandaloneSeatTherm(
                                id = id,
                                gainMap = gainMap,
                                entityName = entityName,
                                spawnLocation = loc,
                                regenType = regenType,
                                delayToRegen = delayToRegen,
                                regenValue = regenValue
                            )
                        }
                    }
                    therms += therm
                    debug("已加载座椅: ${therm.id}.")
                }
            }
        }
    }

    private fun recycle() {
        clearTherms()
        clearTasks()
        clearEntities()
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
            world.entities.filter { it.type == EntityType.ARMOR_STAND && !it.customName.isNullOrEmpty() }.forEach {
                it.remove()
            }
        }
    }

    fun join(player: Player, therm: ITherm) {
        val profile = player.getProfile() ?: return
        KirraThermAPI.actionMessageEmpty.forEachIndexed { index, str ->
            MessageAPI.setCrossHairTip(player, index + 1, str.colored())
        }
        submit(delay = therm.delayToRegen) {
            runRegenTask(player, therm)
        }
        when (therm.type) {
            CUBE -> {
                profile.currentArea = therm.id
                player.sendTitle("", player.asLangText("message-player-join-therm", 0, 25, 0))
                runTotemParticleTask(player)
            }

            STANDALONE_SEAT, PLAYER_SEAT -> {
                profile.currentSeat = therm.id
                player.sendTitle("", player.asLangText("message-player-sits-on-seat", therm.id))
            }
        }
    }

    fun left(player: Player, isSeat: Boolean) {
        val profile = player.getProfile() ?: return
        val thermId = if (isSeat) {
            profile.currentSeat
        } else {
            profile.currentArea
        }
        val message = when (isSeat) {
            true -> player.asLangText("message-player-left-seat", thermId)
            else -> player.asLangText("message-player-quit-therm")
        }
        player.sendTitle("", message, 0, 25, 0)
        when (isSeat) {
            true -> profile.currentSeat = ""
            false -> profile.currentArea = ""
        }
        PlayerThermQuitEvent(player, thermId).call()
        if (profile.currentSeat.isEmpty() && profile.currentArea.isEmpty()) {
            MessageAPI.removeCrossHairTip(player)
        }
    }
}
