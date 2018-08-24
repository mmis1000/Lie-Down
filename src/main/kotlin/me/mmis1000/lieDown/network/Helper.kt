package me.mmis1000.lieDown.network

import com.mojang.authlib.GameProfile
import eu.crushedpixel.sponge.packetgate.api.registry.PacketConnection
import eu.crushedpixel.sponge.packetgate.api.registry.PacketGate
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketAnimation
import net.minecraft.network.play.server.SPacketUseBed
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.Human
import org.spongepowered.api.entity.living.player.Player

class Helper {
    class HumanWrapper(human: Human) : EntityPlayer(human.world as World, GameProfile(null, "place_holder")) {
        override fun isSpectator() = false
        override fun isCreative() = false
    }

    companion object {
        fun sendLieDown(connection: PacketConnection, human: Human) {
            connection.sendPacket(SPacketUseBed(HumanWrapper(human), human.location.blockPosition.run { BlockPos(x, y, z) }))
        }

        fun sendWakeUp(connection: PacketConnection, human: Human) {
            connection.sendPacket(SPacketAnimation(human as Entity, 2))
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
                    ?.let {sendWakeUp(it, human)}
        }
    }
}