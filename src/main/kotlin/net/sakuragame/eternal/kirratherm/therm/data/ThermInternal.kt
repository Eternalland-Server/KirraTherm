package net.sakuragame.eternal.kirratherm.therm.data

import org.bukkit.Location

data class ThermInternal(val type: ThermType, var locA: Location?, var locB: Location?, val allowedRegion: String?) {

    enum class ThermType {
        CUBE, STANDALONE_SEAT, PLAYER_SEAT;

        companion object {

            fun ThermType.isCube() = this == CUBE

            fun ThermType.isStandAlone() = this == STANDALONE_SEAT

            fun ThermType.isPlayer() = this == PLAYER_SEAT
        }
    }
}
