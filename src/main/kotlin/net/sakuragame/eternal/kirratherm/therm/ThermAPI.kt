package net.sakuragame.eternal.kirratherm.therm

import net.sakuragame.eternal.kirratherm.KirraTherm.thermFile
import net.sakuragame.eternal.kirratherm.parseToString
import net.sakuragame.eternal.kirratherm.therm.data.ThermType
import org.bukkit.Location

object ThermAPI {

    fun saveAs(name: String, type: ThermType, locA: Location, locB: Location) {
        when (type) {
            ThermType.CUBE -> generateCubeConfig(name, locA, locB)
            ThermType.STANDALONE_SEAT -> generateStandaloneConfig(name, locA)
            ThermType.PLAYER_SEAT -> generatePlayerConfig(name)
        }
        generateGainMapConfig(name)
        thermFile.saveToFile(thermFile.file)
        ThermManager.i()
    }

    private fun generateCubeConfig(name: String, locA: Location, locB: Location) {
        thermFile["data.$name.type"] = "CUBE"
        thermFile["data.$name.loc-a"] = locA.parseToString()
        thermFile["data.$name.loc-b"] = locB.parseToString()
        thermFile["data.$name.particle.type"] = "SPELL"
        thermFile["data.$name.particle.counts"] = 5
        thermFile["data.$name.particle.distance"] = 0.5
    }

    private fun generateStandaloneConfig(name: String, loc: Location) {
        thermFile["data.$name.type"] = "STANDALONE_SEAT"
        thermFile["data.$name.loc"] = loc.parseToString()
        generateGeneralSeatConfig(name)
    }

    private fun generatePlayerConfig(name: String) {
        thermFile["data.$name.type"] = "PLAYER_SEAT"
        generateGeneralSeatConfig(name)
    }

    private fun generateGeneralSeatConfig(name: String) {
        thermFile["data.$name.entity-name"] = "椅子"
        thermFile["data.$name.delay-to-regen"] = 200
        thermFile["data.$name.regen-hearts-per-ticks"] = 1.0
        thermFile["data.$name.regen-ticks-per-ticks"] = 0.0
    }

    private fun generateGainMapConfig(name: String) {
        thermFile["data.$name.gain.金币"] = 100.0
        thermFile["data.$name.gain.点券"] = 50.0
        thermFile["data.$name.gain.神石"] = 20.0
    }
}