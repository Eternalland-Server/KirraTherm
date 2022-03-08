package net.sakuragame.eternal.kirratherm.therm.data

import net.sakuragame.eternal.kirratherm.KirraThermAPI
import org.bukkit.Location

data class ThermSeat(
    val loc: Location? = null,
    val entityName: String,
    val delayToRegen: Long,
    val regenHeartsPerTicks: Double,
    val regenScalePerTicks: Double
) {

    init {
        if (loc != null) KirraThermAPI.generateSitEntity(loc, entityName)
    }
}
