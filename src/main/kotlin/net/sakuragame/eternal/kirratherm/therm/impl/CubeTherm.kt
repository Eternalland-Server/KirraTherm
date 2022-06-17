package net.sakuragame.eternal.kirratherm.therm.impl

import net.sakuragame.eternal.kirratherm.Profile
import net.sakuragame.eternal.kirratherm.debug
import net.sakuragame.eternal.kirratherm.therm.ITherm
import net.sakuragame.eternal.kirratherm.therm.ThermManager
import net.sakuragame.eternal.kirratherm.therm.data.ThermType
import net.sakuragame.eternal.kirratherm.therm.data.sub.RegenType
import net.sakuragame.eternal.kirratherm.therm.getHollowCube
import org.bukkit.Location
import org.bukkit.Particle
import taboolib.common.platform.function.submit

class CubeTherm(
    override val id: String,
    override val gainMap: MutableMap<String, Double>,

    val locationA: Location,
    val locationB: Location,

    private val particleType: Particle,
    private val particleCounts: Int,
    private val particleDistance: Double,

    override val regenType: RegenType,
    override val delayToRegen: Long,
    override val regenValue: Double
) : ITherm {

    override val type: ThermType = ThermType.CUBE

    init {
        initParticles()
    }

    private fun initParticles() {
        val locations = getHollowCube(locationA, locationB, particleDistance)
        ThermManager.tasks += submit(async = true, delay = 0L, period = 20L) {
            locations.forEach { loc ->
                Profile.profiles.values
                    .filter { it.player.world == loc.world }
                    .filter { it.player.location.distanceSquared(loc) < 20 * 20 }
                    .forEach {
                        it.player.spawnParticle(particleType, loc, particleCounts, 0.0, 0.0, 0.0, 0.02)
                    }
            }
        }
        debug("已开始播放 $id 的特效.")
    }
}