package me.mmis1000.lieDown

import com.google.inject.Inject
import eu.crushedpixel.sponge.packetgate.api.listener.PacketListener.ListenerPriority
import eu.crushedpixel.sponge.packetgate.api.registry.PacketGate
import me.mmis1000.lieDown.data.DataLieDown
import me.mmis1000.lieDown.network.Helper.Companion.sendLieDownToPlayer
import me.mmis1000.lieDown.network.Helper.Companion.sendWakeUpToPlayer
import me.mmis1000.lieDown.network.PlayerSpawnPacketListener
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.CPacketChatMessage
import net.minecraft.network.play.server.SPacketChat
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.data.DataRegistration
import org.spongepowered.api.data.key.Key
import org.spongepowered.api.entity.living.Human
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.entity.DestructEntityEvent
import org.spongepowered.api.event.entity.SpawnEntityEvent
import org.spongepowered.api.event.game.GameRegistryEvent
import org.spongepowered.api.event.game.state.GameConstructionEvent
import org.spongepowered.api.event.game.state.GamePreInitializationEvent
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.world.World
import java.util.concurrent.ConcurrentHashMap


@Plugin(
        id = "lie_down",
        name = "A Plugin that make sponge human lie down",
        version = "1.0"
)
class Main {
    companion object {
        lateinit var main: Main
    }

    @Inject
    lateinit var plugin: PluginContainer

    @Inject
    lateinit var logger: Logger

    private val humanToEntityId = ConcurrentHashMap<Human, Int>()
    private val entityIdToHuman = ConcurrentHashMap<Int, Human>()
    private val entityIdToState = ConcurrentHashMap<Int, Boolean>()


    fun shouldLieDown(id: Int): Boolean {
        return entityIdToState[id] == true
    }

    fun humanById(id: Int): Human? {
        return entityIdToHuman[id]
    }

    @SuppressWarnings("unused")
    @Listener
    fun construct(event: GameConstructionEvent) {
        logger.info("plugin loaded")
        main = this
    }

    @SuppressWarnings("unused")
    @Listener
    fun onDataRegistration(event: GameRegistryEvent.Register<DataRegistration<*, *>>) {
        DataRegistration.builder()
                .dataName("Lie Down")
                .manipulatorId("lie_down")
                .dataClass(DataLieDown::class.java)
                .immutableClass(DataLieDown.Immutable::class.java)
                .builder(DataLieDown.Builder())
                .buildAndRegister(plugin)
    }

    @Listener
    fun onKeyRegistration(event: GameRegistryEvent.Register<Key<*>>) {
        event.register(DataLieDown.key)
    }

    @SuppressWarnings("unused")
    @Listener
    fun preInit(event: GamePreInitializationEvent) {
        Sponge.getServiceManager().provide(PacketGate::class.java).orElse(null)?.registerListener(
                PlayerSpawnPacketListener(),
                ListenerPriority.DEFAULT,
                CPacketChatMessage::class.java,
                SPacketChat::class.java
        )
    }


    fun handleSet(human: Human, isLying: Boolean) {
        logger.info("setting $human")

        val id = (human as Entity).entityId

        if (humanToEntityId[human] != null) {
            logger.info("update lie down status of $id to $isLying")

            if (isLying) {
                (human.world as World).players.forEach {
                    sendLieDownToPlayer(it, human)
                }
            } else {
                (human.world as World).players.forEach {
                    sendWakeUpToPlayer(it, human)
                }
            }
        } else {
            logger.info("set lie down status of $id to $isLying")
        }

        humanToEntityId[human] = id
        entityIdToState[id] = isLying
        entityIdToHuman[id] = human

    }

    fun handleUnset(human: Human) {
        human as? Human ?: return
        logger.info("removing $human")

        val id = (human as Entity).entityId
        humanToEntityId -= human
        entityIdToState -= id
        entityIdToHuman -= id
    }

    @Listener
    fun onEntitySpawn(event: SpawnEntityEvent) {
        event.entities.forEach { human ->
            human as? Human ?: return@forEach

            human[DataLieDown::class.java].orElse(null).fill(human).orElse(null)?.let {
                human.offer(it)
            } ?: let {
                val container = DataLieDown(false, human)
                human.offer(container)
            }

            val result2 = human.offer(DataLieDown.key, true)
            logger.info("offer data result2 ${result2.isSuccessful}, ${result2.type}, ${result2.rejectedData}")
        }
    }

    @Listener
    fun onHumanDead(event: DestructEntityEvent) {
        val target = event.targetEntity as? Human ?: return
        logger.info("killing $target")
        handleUnset(target)
    }

    @Listener
    fun onChunkUnload(event: UnloadChunkEvent) {
        event.targetChunk.entities.forEach {
            val human = it as? Human ?: return@forEach
            logger.info("unloading $human")
            handleUnset(human)
        }
    }
}