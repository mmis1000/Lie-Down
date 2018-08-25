package me.mmis1000.lieDown

import com.google.inject.Inject
import eu.crushedpixel.sponge.packetgate.api.listener.PacketListener.ListenerPriority
import eu.crushedpixel.sponge.packetgate.api.registry.PacketGate
import me.mmis1000.lieDown.command.LieDownCommand
import me.mmis1000.lieDown.data.DataLieDown
import me.mmis1000.lieDown.network.Helper.Companion.sendLieDownToPlayer
import me.mmis1000.lieDown.network.Helper.Companion.sendWakeUpToPlayer
import me.mmis1000.lieDown.network.PlayerSpawnPacketListener
import net.minecraft.entity.Entity
import net.minecraft.network.play.server.SPacketSpawnPlayer
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
import org.spongepowered.api.event.game.state.GamePostInitializationEvent
import org.spongepowered.api.event.game.state.GamePreInitializationEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
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

    @Suppress("UNUSED_PARAMETER")
    @Listener
    fun construct(event: GameConstructionEvent) {
        logger.info("plugin loaded")
        main = this
    }

    @Suppress("UNUSED_PARAMETER")
    @Listener
    fun preIInit(event: GamePreInitializationEvent) {
        Sponge.getCommandManager().register(plugin, LieDownCommand.spec, "liedown")
    }

    @Suppress("UNUSED_PARAMETER")
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

    @Listener
    fun userJoin(event: ClientConnectionEvent.Join) {
        logger.info("Registering user packet listener")

        val packetGate = Sponge.getServiceManager().provide(PacketGate::class.java).get()
        val connection = packetGate.connectionByPlayer(event.targetEntity).get()

        packetGate.registerListener(
                PlayerSpawnPacketListener(),
                ListenerPriority.FIRST,
                connection,
                SPacketSpawnPlayer::class.java
        )
    }


    fun handleSet(human: Human, isLying: Boolean) {
        logger.info("setting $human")

        val id = (human as Entity).entityId

        if (humanToEntityId[human] != null) {
            logger.info("update lie down status of $id to $isLying")

            humanToEntityId[human] = id
            entityIdToState[id] = isLying
            entityIdToHuman[id] = human

            (human.world as World).players.forEach {
                if (isLying) {
                    sendLieDownToPlayer(it, human)
                } else {
                    sendWakeUpToPlayer(it, human)
                }
            }
        } else {
            logger.info("set lie down status of $id to $isLying")

            humanToEntityId[human] = id
            entityIdToState[id] = isLying
            entityIdToHuman[id] = human
        }
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

            human[DataLieDown::class.java].orElse(null)?.fill(human)?.orElse(null)?.let {
                human.offer(it)
            } ?: let {
                val container = DataLieDown(false, human)
                human.offer(container)
            }
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