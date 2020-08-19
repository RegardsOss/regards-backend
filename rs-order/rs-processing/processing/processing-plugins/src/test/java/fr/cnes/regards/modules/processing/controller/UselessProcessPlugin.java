package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.duration.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.domain.size.IResultSizeForecast;
import fr.cnes.regards.modules.processing.plugins.impl.AbstractBaseProcessPlugin;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

@Plugin(id = "UselessProcessPlugin",
        version = "1.0.0-SNAPSHOT",
        description = "UselessProcessPlugin description",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class UselessProcessPlugin extends AbstractBaseProcessPlugin {

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
        return new IExecutable() {
            @Override
            public Mono<Seq<POutputFile>> execute(ExecutionContext context, FluxSink<PStep> stepSink) {
                return Mono.empty();
            }
        };
    }

    @Override public Seq<ExecutionParameterDescriptor> parameters() {
        return List.empty();
    }
}
