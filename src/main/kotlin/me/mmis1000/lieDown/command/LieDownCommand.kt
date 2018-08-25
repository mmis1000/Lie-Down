package me.mmis1000.lieDown.command

import me.mmis1000.lieDown.data.DataLieDown
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.entity.EntityTypes
import org.spongepowered.api.entity.living.Human
import org.spongepowered.api.text.Text

class LieDownCommand : CommandExecutor {
    companion object {
        val spec: CommandSpec = CommandSpec.builder()
                .description(Text.of("Make human entity lie down"))
                .arguments(
                        GenericArguments.firstParsing(
                                HumanSelector(Text.of("human")),
                                GenericArguments.entity(Text.of("human"), EntityTypes.HUMAN)
                        ),
                        GenericArguments.optional(GenericArguments.bool(Text.of("is_lying_down")))
                )
                .permission("me.mmis1000.liedown")
                .executor(LieDownCommand())
                .build()
    }

    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        val humans = args.getAll<Human>("human")
        val isLyingDown = args.getOne<Boolean>("is_lying_down").orElse(true)

        humans.forEach {
            it.offer(DataLieDown.key, isLyingDown)
        }

        return CommandResult.affectedEntities(humans.size)
    }
}