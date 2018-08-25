package me.mmis1000.lieDown.network

import com.mojang.authlib.GameProfile
import eu.crushedpixel.sponge.packetgate.api.registry.PacketConnection
import eu.crushedpixel.sponge.packetgate.api.registry.PacketGate
import me.mmis1000.lieDown.Y_OFFSET
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketAnimation
import net.minecraft.network.play.server.SPacketEntity
import net.minecraft.network.play.server.SPacketUseBed
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.Human
import org.spongepowered.api.entity.living.player.Player

class Helper {
    class HumanWrapper(human: Human) : EntityPlayer(human.world as World, GameProfile(null, "place_holder")) {
        init {
            entityId = (human as Entity).entityId
        }

        override fun isSpectator() = false
        override fun isCreative() = false
    }

    companion object {
        private val Double.mc_l: Long
            get() = (this * 32.0 * 128.0).toLong()

        fun sendLieDown(connection: PacketConnection, human: Human) {
            connection.sendPacket(SPacketUseBed(HumanWrapper(human), human.location.blockPosition.run { BlockPos(x, y, z) }))

            val (x, y, z) = human.location.run { Triple(x, y, z) }

            val (blockX, blockY, blockZ) = human.location.blockPosition.run { Triple(x, y, z) }

            val (offsetX, offsetY, offsetZ) =
                    Triple(x - blockX, y - blockY + Y_OFFSET, z - blockZ)

            connection.sendPacket(SPacketEntity.S15PacketEntityRelMove((human as Entity).entityId, offsetX.mc_l, offsetY.mc_l, offsetZ.mc_l, false))
        }

        fun sendWakeUp(connection: PacketConnection, human: Human) {
            connection.sendPacket(SPacketAnimation(human as Entity, 2))
            connection.sendPacket(SPacketEntity.S15PacketEntityRelMove((human as Entity).entityId, 0, -(Y_OFFSET * 32.0 * 128.0).toLong(), 0, true))
        }

        fun sendLieDownToPlayer(player: Player, human: Human) {
            Sponge.getServiceManager()
                    .provide(PacketGate::class.java)
                    .orElse(null)
                    ?.connectionByPlayer(player)
                    ?.orElse(null)
                    ?.let { sendLieDown(it, human) }
        }

        fun sendWakeUpToPlayer(player: Player, human: Human) {
            Sponge.getServiceManager()
                    .provide(PacketGate::class.java)
                    .orElse(null)
                    ?.connectionByPlayer(player)
                    ?.orElse(null)
                    ?.let { sendWakeUp(it, human) }
        }
    }
}