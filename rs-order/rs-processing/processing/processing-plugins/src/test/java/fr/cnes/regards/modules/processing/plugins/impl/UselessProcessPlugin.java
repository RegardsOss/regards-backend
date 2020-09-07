package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import reactor.core.publisher.Mono;

@Plugin(id = "UselessProcessPlugin",
        version = "1.0.0-SNAPSHOT",
        description = "UselessProcessPlugin description",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class UselessProcessPlugin implements IProcessDefinition {

    @Override public Seq<DataType> requiredDataTypes() {
        return List.empty();
    }

    @Override public Try<IResultSizeForecast> sizeForecast() {
        return Try.success(IResultSizeForecast.zeroSize());
    }

    @Override public Try<IRunningDurationForecast> durationForecast() {
        return Try.success(IRunningDurationForecast.defaultDuration());
    }

    @Override public IExecutable executable() {
        return IExecutable.wrap(Mono::just);
    }

    @Override public Seq<ExecutionParameterDescriptor> parameters() {
        return List.empty();
    }
}
