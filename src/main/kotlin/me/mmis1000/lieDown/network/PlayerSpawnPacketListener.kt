package me.mmis1000.lieDown.network

import eu.crushedpixel.sponge.packetgate.api.event.PacketEvent
import eu.crushedpixel.sponge.packetgate.api.listener.PacketListenerAdapter
import eu.crushedpixel.sponge.packetgate.api.registry.PacketConnection
import me.mmis1000.lieDown.Main
import me.mmis1000.lieDown.Main.Companion.main
import me.mmis1000.lieDown.network.Helper.Companion.sendLieDown
import net.minecraft.network.play.server.SPacketSpawnPlayer
import org.spongepowered.api.scheduler.Task

class PlayerSpawnPacketListener : PacketListenerAdapter() {
    override fun onPacketWrite(packetEvent: PacketEvent, connection: PacketConnection) {
        (packetEvent.packet as SPacketSpawnPlayer).let {
            if (main.shouldLieDown(it.entityId)) {
                main.humanById(it.entityId)?.let { human ->
                    Task.builder()
                            .name("lie down - next tick")
                            .execute { _ ->
                                sendLieDown(connection, human)
                            }
                            .delayTicks(1)
                            .submit(Main.main.plugin)
                }
            }
        }
    }
}