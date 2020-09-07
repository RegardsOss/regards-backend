package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.Function2;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;

public interface IExecutable {

    /**
     * An executable receives execution context, and returns a new context in a Mono.
     *
     * @param context the execution parameters
     */
    Mono<ExecutionContext> execute(ExecutionContext context);

    static IExecutable wrap(Mono<ExecutionContext> mono) {
        return context -> mono;
    }

    static IExecutable wrap(Function<ExecutionContext, Mono<ExecutionContext>> fn) {
        return fn::apply;
    }

    static IExecutable sendEvent(Supplier<ExecutionEvent> event) {
        return context -> context.sendEvent(event);
    }

    static IExecutable sendEvent(ExecutionEvent event) {
        return context -> context.sendEvent(() -> event);
    }

    /**
     * IExecutable instances are chainable, thanks to the {@link Mono#flatMap(Function)} method.
     * @param next the next executable to launch after this one.
     * @return a new executable with this and next in sequence.
     */
    default IExecutable andThen(IExecutable next) {
        return ctx1 -> execute(ctx1).flatMap(next::execute);
    }

    default IExecutable onError(Function2<ExecutionContext, Throwable, Mono<ExecutionContext>> recover) {
        return context -> execute(context).onErrorResume(recover.apply(context));
    }

    default IExecutable interrupt() {
        return context -> execute(context).flatMap(c -> Mono.empty());
    }

}
