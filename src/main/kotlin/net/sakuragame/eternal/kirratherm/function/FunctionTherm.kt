package net.sakuragame.eternal.kirratherm.function

import com.taylorswiftcn.justwei.util.UnitConvert
import net.sakuragame.eternal.gemseconomy.api.GemsEconomyAPI
import net.sakuragame.eternal.gemseconomy.currency.EternalCurrency
import net.sakuragame.eternal.justmessage.api.MessageAPI
import net.sakuragame.eternal.kirratherm.KirraThermAPI
import net.sakuragame.eternal.kirratherm.Profile
import net.sakuragame.eternal.kirratherm.Profile.Companion.getProfile
import net.sakuragame.eternal.kirratherm.event.PlayerThermGainEvent
import net.sakuragame.eternal.kirratherm.getRegion
import net.sakuragame.eternal.kirratherm.therm.*
import net.sakuragame.eternal.kirratherm.therm.data.ThermInternal.ThermType.Companion.isCube
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common5.Baffle
import taboolib.platform.util.sendLang
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

object FunctionTherm {

    private val baffle by lazy {
        Baffle.of(1, TimeUnit.SECONDS)
    }

    @Awake(LifeCycle.ACTIVE)
    fun i() {
        submit(async = true, period = 2L) {
            Profile.profiles.values.forEach { profile ->
                val player = profile.player
                Therm.getAll()
                    .filter { it.data.type.isCube() }
                    .filter { player.isInArea(it.data.locA!!, it.data.locB!!) && profile.currentTherm != it.name }
                    .forEach {
                        Therm.join(player, it)
                    }
                if (profile.armorStandEntity == null && Therm.getByLoc(player.location) == null && profile.currentTherm.isNotEmpty()) {
                    Therm.left(player)
                }
            }
        }
        submit(async = true, period = KirraThermAPI.thermInterval) {
            Profile.profiles.values
                .filter { it.currentTherm.isNotEmpty() }
                .forEach { profile ->
                    val player = profile.player
                    val therm = Therm.getByName(profile.currentTherm) ?: return@forEach
                    val belongPermission = player.getBelongPermission() ?: "default"
                    val finalGainMap = mutableMapOf<String, Double>().also {
                        it += therm.gainMap
                        it.forEach { (name, _) ->
                            KirraThermAPI.multipleMap[belongPermission]!!.forEach { data ->
                                if (data.name == name) it[data.name] = it[data.name]!!.times(data.value)
                            }
                        }
                    }
                    finalGainMap.forEach gainForeach@{ (index, value) ->
                        val currency = EternalCurrency.values().find { it.identifier == index } ?: return@gainForeach
                        GemsEconomyAPI.deposit(player.uniqueId, value, currency, "泡点奖励.")
                    }
                    PlayerThermGainEvent(player, therm.name, finalGainMap).call()
                    playActions(player, finalGainMap)
                }
        }
    }

    @SubscribeEvent
    fun onRideStandaloneSeat(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) {
            return
        }
        val player = e.player
        val entity = getStandaloneEntity(player) ?: return
        val profile = player.getProfile() ?: return
        val therm = Therm.getNearestSeatByLoc(entity.location) ?: return
        if (!baffle.hasNext(player.name)) {
            return
        }
        baffle.next(player.name)
        if (!isAllowedToRideSeat(therm, player)) {
            return
        }
        profile.armorStandEntity = entity.apply {
            addPassenger(player)
        }
        Therm.join(player, therm)
    }

    @SubscribeEvent
    fun onTryToSpawnSeat(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) {
            return
        }
        val item = e.item ?: return
        if (!item.isSeat()) return
        val player = e.player
        val profile = player.getProfile() ?: return
        val seatId = item.getSeatId() ?: return
        val therm = Therm.getByName(seatId) ?: return
        val clickedBlock = e.clickedBlock ?: return
        if (!isAllowedToRideSeat(therm, player)) {
            return
        }
        profile.armorStandEntity = KirraThermAPI.generateSitEntity(clickedBlock.location
            .add(0.0, 1.0, 0.0)
            .setDirection(player.location.direction),
            seatId,
            player
        )
        Therm.join(player, therm)
    }

    @SubscribeEvent
    fun e(e: EntityDamageEvent) {
        if (e.isCancelled) return
        val player = e.entity as? Player ?: return
        val profile = player.getProfile() ?: return
        if (profile.armorStandEntity != null) {
            removePlayerSeat(player)
        }
    }

    @SubscribeEvent
    fun e(e: PlayerDropItemEvent) {
        if (e.isCancelled) return
        if (!e.itemDrop.itemStack.isSeat()) return
        val player = e.player
        val profile = player.getProfile() ?: return
        if (profile.armorStandEntity != null) {
            removePlayerSeat(player)
        }
    }

    @SubscribeEvent
    fun e(e: PlayerQuitEvent) {
        val profile = e.player.getProfile() ?: return
        if (profile.armorStandEntity != null) {
            removePlayerSeat(e.player)
        }
    }

    @SubscribeEvent
    fun e(e: PlayerMoveEvent) {
        val player = e.player
        val profile = player.getProfile() ?: return
        if (profile.armorStandEntity != null) {
            return removePlayerSeat(player)
        }
    }

    private fun isAllowedToRideSeat(therm: Therm, player: Player): Boolean {
        if (isAlreadySited(therm.name)) {
            if (getSitedPlayer(therm.name)?.uniqueId == player.uniqueId) {
                return false
            }
            player.sendLang("message-player-already-sit")
            return false
        }
        if (player.vehicle != null) {
            return false
        }
        if (baffle.hasNext(player.name)) {
            player.sendLang("message-player-baffle")
            return false
        }
        return true
    }

    private fun playActions(player: Player, gainMap: MutableMap<String, Double>) {
        KirraThermAPI.actionMessage.forEachIndexed { index, str ->
            MessageAPI.setCrossHairTip(player, index + 1, toReplaced(str, player, gainMap))
        }
    }

    private fun toReplaced(str: String, player: Player, gainMap: MutableMap<String, Double>): String {
        var toReturn = str
        gainMap.forEach { (index, value) ->
            val currency = EternalCurrency.values().find { it.identifier == index } ?: return@forEach
            val currencyBalance = GemsEconomyAPI.getBalance(player.uniqueId, currency).roundToLong()
            toReturn = toReturn.replace("<$index>", UnitConvert.formatCN(UnitConvert.TenThousand, currencyBalance.toDouble()))
            toReturn = toReturn.replace("<$index-added>", UnitConvert.formatCN(UnitConvert.TenThousand, value))
        }
        return toReturn
    }

    private fun removePlayerSeat(player: Player) {
        val profile = player.getProfile() ?: return
        val entity = profile.armorStandEntity ?: return
        val isStandalone = entity.hasMetadata(Therm.STANDALONE_SEAT_KEY)
        if (isStandalone) {
            entity.removePassenger(player)
        } else {
            entity.remove()
        }
        profile.armorStandEntity = null
        Therm.left(player, true)
    }
}