package fr.cnes.regards.modules.processing.demo.process;

import fr.cnes.regards.modules.processing.demo.engine.event.StartWithProfileEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.demo.DemoConstants.*;

public class DemoProcessExecutable implements IExecutable {

    private final DemoSimulatedAsyncProcessFactory asyncProcessFactory;

    public DemoProcessExecutable(DemoSimulatedAsyncProcessFactory asyncProcessFactory) {
        this.asyncProcessFactory = asyncProcessFactory;
    }

    @Override public Mono<ExecutionContext> execute(ExecutionContext context) {
        return Mono.fromCallable(() -> {
            // Extract the parameters
            String profile = context.getBatch()
                .getUserSuppliedParameters()
                .filter(v -> v.getName().equals(PROFILE))
                .map(v -> v.getValue())
                .headOption()
                .getOrElse(NO_PROFILE_FOUND);

            // Call async service, which will provide the steps as amqp messages
            asyncProcessFactory.make().send(
                context,
                new StartWithProfileEvent(
                    profile,
                    context.getExec()
                        .getInputFiles()
                        .map(f -> f.getUrl().toString())
                        .toList()
                )
            );

            return context;
        });
    }
}
