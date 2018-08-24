package me.mmis1000.lieDown.data

import me.mmis1000.lieDown.Main.Companion.main
import org.spongepowered.api.data.DataContainer
import org.spongepowered.api.data.DataHolder
import org.spongepowered.api.data.DataQuery
import org.spongepowered.api.data.DataView
import org.spongepowered.api.data.key.Key
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableBooleanData
import org.spongepowered.api.data.manipulator.mutable.common.AbstractBooleanData
import org.spongepowered.api.data.merge.MergeFunction
import org.spongepowered.api.data.persistence.AbstractDataBuilder
import org.spongepowered.api.data.value.mutable.Value
import org.spongepowered.api.entity.living.Human
import org.spongepowered.api.util.TypeTokens
import java.util.*

class DataLieDown(bool: Boolean = false, var human: Human? = null) : AbstractBooleanData<DataLieDown, DataLieDown.Immutable>(bool, key, false) {
    companion object {
        val key: Key<Value<Boolean>> = Key.builder()
                .type(TypeTokens.BOOLEAN_VALUE_TOKEN)
                .id("lie_down")
                .name("Lie Down")
                .query(DataQuery.of("LieDown"))
                .build()
    }

    init {
        main.logger.info("data created, $bool")

        registerFieldSetter(key) {
            this@DataLieDown.value = it

            human?.let { human ->
                main.handleSet(human, it)
            }
        }
    }

    override fun getContentVersion() = 1
    override fun asImmutable() = Immutable(value, human)
    override fun copy() = DataLieDown(value, human)

    override fun from(container: DataContainer): Optional<DataLieDown> {
        val result = container.getBoolean(key.query)

        if (!result.isPresent) {
            return Optional.empty()
        }

        value = result.get()
        return Optional.of(this)
    }

    override fun fill(dataHolder: DataHolder, overlap: MergeFunction): Optional<DataLieDown> {
        dataHolder as? Human ?: return Optional.of(this)

        human = dataHolder

        value = overlap.merge(this, dataHolder[DataLieDown::class.java].orElse(null)).value

        human?.let { human ->
            main.handleSet(human, value)
        }

        return Optional.of(this)
    }

    class Immutable(bool: Boolean = false, var human: Human? = null) : AbstractImmutableBooleanData<Immutable, DataLieDown>(false, key, bool) {
        override fun getContentVersion() = 1
        override fun asMutable() = DataLieDown(value, human)
    }

    class Builder : AbstractDataBuilder<DataLieDown>(DataLieDown::class.java, 1),
            DataManipulatorBuilder<DataLieDown, Immutable> {
        override fun createFrom(dataHolder: DataHolder): Optional<DataLieDown> {
            if (dataHolder as? Human == null) {
                Optional.empty<DataLieDown>()
            }

            return create().fill(dataHolder)
        }

        override fun create() = DataLieDown()
        override fun buildContent(container: DataView) = create().from(container.copy())
    }
}