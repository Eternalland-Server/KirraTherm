package net.sakuragame.eternal.kirratherm.therm

import net.sakuragame.eternal.kirratherm.therm.data.ThermType
import net.sakuragame.eternal.kirratherm.therm.data.sub.RegenType

interface ITherm {

    val id: String

    val type: ThermType

    val gainMap: MutableMap<String, Double>

    val regenType: RegenType
    val delayToRegen: Long
    val regenValue: Double
}