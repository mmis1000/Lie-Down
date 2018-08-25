package me.mmis1000.lieDown.command

import com.google.common.collect.ImmutableList
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandArgs
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.Human
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.selector.Selector
import java.lang.management.PlatformLoggingMXBean


class HumanSelector(key: Text) : CommandElement(key) {
    override fun parseValue(source: CommandSource, args: CommandArgs): Any? {
        val nextArg = args.peek()

        if (nextArg.startsWith("@")) {
            val selectedEntities: List<Human>

            try {
                selectedEntities = Selector.parse(args.next()).resolve(source).filter {
                    it is Human
                }.map {
                    it as Human
                }
            } catch (e: IllegalArgumentException) {
                throw args.createError(Text.of("Could not parse selector."))
            }

            if (selectedEntities.isEmpty()) {
                throw args.createError(Text.of("No entities selected."))
            }

            (source as? Player)?.sendMessage(Text.of("selected ${selectedEntities.size} humans"))

            return selectedEntities
        }

        throw args.createError(Text.of("Not a selector."))
    }

    override fun complete(src: CommandSource, args: CommandArgs, context: CommandContext): MutableList<String> {
        val snapshot = args.snapshot
        val nextArg = args.nextIfPresent()

        args.applySnapshot(snapshot)

        return if (nextArg.isPresent) {
            Selector.complete(nextArg.get())
        } else {
            ImmutableList.of()
        }
    }

    override fun getUsage(src: CommandSource?): Text {
        return Text.of("<selector>")
    }
}