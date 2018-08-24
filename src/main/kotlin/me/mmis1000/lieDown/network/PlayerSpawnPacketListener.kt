package me.mmis1000.lieDown.network

import eu.crushedpixel.sponge.packetgate.api.event.PacketEvent
import eu.crushedpixel.sponge.packetgate.api.listener.PacketListenerAdapter
import eu.crushedpixel.sponge.packetgate.api.registry.PacketConnection
import me.mmis1000.lieDown.Main
import me.mmis1000.lieDown.Main.Companion.main
import me.mmis1000.lieDown.network.Helper.Companion.sendLieDown
import net.minecraft.network.play.server.SPacketSpawnPlayer

class PlayerSpawnPacketListener : PacketListenerAdapter() {
    override fun onPacketWrite(packetEvent: PacketEvent?, connection: PacketConnection?) {
        packetEvent ?: return
        connection ?: return

        (packetEvent.packet as? SPacketSpawnPlayer)?.let {
            if (main.shouldLieDown(it.entityID)) {
                main.humanById(it.entityID)?.let { human ->
                    sendLieDown(connection, human)
                }
            } else {
                Main.main.logger.info("don't make ${it.entityID} lie down")
            }
        }
    }
}