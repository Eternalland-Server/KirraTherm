package net.sakuragame.eternal.kirratherm.therm

import net.sakuragame.eternal.kirratherm.KirraTherm
import net.sakuragame.eternal.kirratherm.parseToString
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal

object ThermAPI {

    fun saveAs(name: String, thermInternal: ThermInternal) {
        when (thermInternal.type) {
            ThermInternal.ThermType.CUBE -> generateCubeConf(name, thermInternal)
            ThermInternal.ThermType.STANDALONE_SEAT -> generateStandaloneConf(name, thermInternal)
            ThermInternal.ThermType.PLAYER_SEAT -> generatePlayerConf(name)
        }
        if (thermInternal.type == ThermInternal.ThermType.STANDALONE_SEAT || thermInternal.type == ThermInternal.ThermType.PLAYER_SEAT) {
            generateGeneralSeatConf(name)
        }
        generateGeneralConf(name)
        Therm.i()
    }

    private fun generateCubeConf(name: String, thermInternal: ThermInternal) {
        KirraTherm.thermFile["data.$name.type"] = "CUBE"
        KirraTherm.thermFile["data.$name.loc-a"] = thermInternal.locA!!.parseToString()
        KirraTherm.thermFile["data.$name.loc-b"] = thermInternal.locB!!.parseToString()
        KirraTherm.thermFile["data.$name.particle.type"] = "SPELL"
        KirraTherm.thermFile["data.$name.particle.counts"] = 5
        KirraTherm.thermFile["data.$name.particle.distance"] = 0.5
    }

    private fun generateStandaloneConf(name: String, thermInternal: ThermInternal) {
        KirraTherm.thermFile["data.$name.type"] = "STANDALONE_SEAT"
        KirraTherm.thermFile["data.$name.loc"] = thermInternal.locA!!.parseToString()
    }

    private fun generatePlayerConf(name: String) {
        KirraTherm.thermFile["data.$name.type"] = "PLAYER_SEAT"
        KirraTherm.thermFile["data.$name.item-name"] = "椅子召唤器"
    }

    private fun generateGeneralSeatConf(name: String) {
        KirraTherm.thermFile["data.$name.entity-name"] = "椅子"
        KirraTherm.thermFile["data.$name.delay-to-regen"] = 200
        KirraTherm.thermFile["data.$name.regen-hearts-per-ticks"] = 1.0
        KirraTherm.thermFile["data.$name.regen-ticks-per-ticks"] = 0.0
    }

    private fun generateGeneralConf(name: String) {
        KirraTherm.thermFile["data.$name.gain.金币"] = 100.0
        KirraTherm.thermFile["data.$name.gain.点券"] = 50.0
        KirraTherm.thermFile["data.$name.gain.神石"] = 20.0
    }
}