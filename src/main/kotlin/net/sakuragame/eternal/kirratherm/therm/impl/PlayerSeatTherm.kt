package net.sakuragame.eternal.kirratherm.therm.impl

import net.sakuragame.eternal.kirratherm.therm.ITherm
import net.sakuragame.eternal.kirratherm.therm.data.ThermType
import net.sakuragame.eternal.kirratherm.therm.data.sub.RegenType

class PlayerSeatTherm(
    override val id: String,
    override val gainMap: MutableMap<String, Double>,

    private val entityName: String,

    override val regenType: RegenType,
    override val delayToRegen: Long,
    override val regenValue: Double,
) : ITherm {

    override val type: ThermType = ThermType.PLAYER_SEAT
}