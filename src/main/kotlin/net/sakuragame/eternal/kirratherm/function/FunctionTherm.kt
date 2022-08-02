package net.sakuragame.eternal.kirratherm.function

import com.taylorswiftcn.justwei.util.UnitConvert
import net.sakuragame.eternal.gemseconomy.api.GemsEconomyAPI
import net.sakuragame.eternal.gemseconomy.currency.EternalCurrency
import net.sakuragame.eternal.justmessage.api.MessageAPI
import net.sakuragame.eternal.kirratherm.KirraThermAPI
import net.sakuragame.eternal.kirratherm.Profile
import net.sakuragame.eternal.kirratherm.Profile.Companion.getProfile
import net.sakuragame.eternal.kirratherm.event.PlayerThermGainEvent
import net.sakuragame.eternal.kirratherm.therm.*
import net.sakuragame.eternal.kirratherm.therm.impl.CubeTherm
import net.sakuragame.eternal.kirratherm.therm.impl.PlayerSeatTherm
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.spigotmc.event.entity.EntityMountEvent
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
                ThermManager.therms
                    .mapNotNull { it as? CubeTherm }
                    .forEach {
                        if (player.isInArea(it.locationA, it.locationB) && profile.currentArea != it.id) {
                            ThermManager.join(player, it)
                        }
                    }
                if (profile.armorStandEntity == null && ThermManager.getByLoc(player.location) == null) {
                    if (profile.currentArea.isNotEmpty()) {
                        ThermManager.left(player, isSeat = false)
                    }
                }
            }
        }
        submit(async = true, period = KirraThermAPI.thermInterval) {
            Profile.profiles.values
                .filter { it.currentSeat.isNotEmpty() || it.currentArea.isNotEmpty() }
                .forEach { profile ->
                    val player = profile.player
                    val gainMap = profile.getGainMap()
                    val permission = player.getBelongPermission() ?: "default"
                    val finalGainMap = mutableMapOf<String, Double>().also {
                        it += gainMap
                        it.keys.forEach { name ->
                            KirraThermAPI.multipleMap[permission]!!.forEach { data ->
                                if (data.name == name) it[data.name] = it[data.name]!!.times(data.value)
                            }
                        }
                    }
                    finalGainMap.forEach gainForeach@{ (index, value) ->
                        val currency = EternalCurrency.values().find { it.identifier == index } ?: return@gainForeach
                        GemsEconomyAPI.deposit(player.uniqueId, value, currency)
                    }
                    PlayerThermGainEvent(player, finalGainMap).call()
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
        val therm = ThermManager.getNearestSeatByLoc(entity.location) ?: return
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
        ThermManager.join(player, therm)
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
        val therm = ThermManager.getByType<PlayerSeatTherm>().find { it.itemId == seatId } ?: return
        val entityName = therm.entityName
        val clickedBlock = e.clickedBlock ?: return
        if (!isAllowedToRideSeat(therm, player)) {
            return
        }
        profile.armorStandEntity = KirraThermAPI.generateSitEntity(
            clickedBlock.location
                .add(0.0, 1.0, 0.0)
                .setDirection(player.location.direction),
            entityName,
            player
        )
        ThermManager.join(player, therm)
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

    @SubscribeEvent
    fun e(e: EntityMountEvent) {
        val player = e.entity as? Player ?: return
        val profile = player.getProfile() ?: return
        if (profile.armorStandEntity != null) {
            return removePlayerSeat(player)
        }
    }

    private fun isAllowedToRideSeat(therm: ITherm, player: Player): Boolean {
        if (isAlreadySited(player, therm)) {
            if (getSitedPlayer(therm)?.uniqueId == player.uniqueId) {
                return false
            }
            player.sendLang("message-player-already-sit")
            return false
        }
        if (player.vehicle != null) {
            return false
        }
        if (!baffle.hasNext(player.name)) {
            return false
        }
        baffle.next(player.name)
        player.sendLang("message-player-baffle")
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
        val isStandalone = entity.hasMetadata(ThermManager.STANDALONE_SEAT_KEY)
        if (isStandalone) {
            entity.removePassenger(player)
        } else {
            entity.remove()
        }
        profile.armorStandEntity = null
        ThermManager.left(player, isSeat = true)
    }
}