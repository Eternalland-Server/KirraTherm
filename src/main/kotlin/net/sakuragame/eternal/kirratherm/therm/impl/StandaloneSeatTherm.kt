package net.sakuragame.eternal.kirratherm.therm.impl

import net.sakuragame.eternal.kirratherm.KirraThermAPI
import net.sakuragame.eternal.kirratherm.therm.ITherm
import net.sakuragame.eternal.kirratherm.therm.data.ThermType
import net.sakuragame.eternal.kirratherm.therm.data.sub.RegenType
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

class StandaloneSeatTherm(
    override val id: String,
    override val gainMap: MutableMap<String, Double>,

    private val entityName: String,
    val spawnLocation: Location,

    override val regenType: RegenType,
    override val delayToRegen: Long,
    override val regenValue: Double,
) : ITherm {

    override val type: ThermType = ThermType.STANDALONE_SEAT

    var entity: ArmorStand? = null

    init {
        entity = KirraThermAPI.generateSitEntity(spawnLocation, entityName)
    }
}