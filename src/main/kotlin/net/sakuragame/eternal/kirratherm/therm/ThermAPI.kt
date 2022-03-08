package net.sakuragame.eternal.kirratherm.therm

import net.sakuragame.eternal.kirratherm.KirraTherm.thermFile
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
        thermFile.saveToFile(thermFile.file)
        Therm.i()
    }

    private fun generateCubeConf(name: String, thermInternal: ThermInternal) {
        thermFile["data.$name.type"] = "CUBE"
        thermFile["data.$name.loc-a"] = thermInternal.locA!!.parseToString()
        thermFile["data.$name.loc-b"] = thermInternal.locB!!.parseToString()
        thermFile["data.$name.particle.type"] = "SPELL"
        thermFile["data.$name.particle.counts"] = 5
        thermFile["data.$name.particle.distance"] = 0.5
    }

    private fun generateStandaloneConf(name: String, thermInternal: ThermInternal) {
        thermFile["data.$name.type"] = "STANDALONE_SEAT"
        thermFile["data.$name.loc"] = thermInternal.locA!!.parseToString()
    }

    private fun generatePlayerConf(name: String) {
        thermFile["data.$name.type"] = "PLAYER_SEAT"
    }

    private fun generateGeneralSeatConf(name: String) {
        thermFile["data.$name.entity-name"] = "椅子"
        thermFile["data.$name.delay-to-regen"] = 200
        thermFile["data.$name.regen-hearts-per-ticks"] = 1.0
        thermFile["data.$name.regen-ticks-per-ticks"] = 0.0
    }

    private fun generateGeneralConf(name: String) {
        thermFile["data.$name.gain.金币"] = 100.0
        thermFile["data.$name.gain.点券"] = 50.0
        thermFile["data.$name.gain.神石"] = 20.0
    }
}