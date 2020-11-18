package fr.cnes.regards.modules.processing.domain.execution;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@Value @AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionContext {

    @With PExecution exec;
    PBatch batch;
    PProcess process;

    IExecutionEventNotifier eventNotifier;

    @With Map<Class<?>, Object> params;

    public ExecutionContext(PExecution exec, PBatch batch, PProcess process, IExecutionEventNotifier notifierFor) {
        this(exec, batch, process, notifierFor, HashMap.empty());
    }

    public Mono<ExecutionContext> sendEvent(Supplier<ExecutionEvent> event) {
        return getEventNotifier()
                .notifyEvent(event.get())
                .map(this::withExec);
    }

    public <T> ExecutionContext withParam(Class<T> type, T value) {
        return withParams(params.put(type, value));
    }

    @SuppressWarnings("unchecked")
    public <T> Mono<T> getParam(Class<T> type) {
        return params.get(type).map(o -> (T)o)
            .map(Mono::just)
            .getOrElse(() -> Mono.error(new MissingExecutionContextParameterException("Param for type '" + type.getSimpleName() + "' not found")));
    }

    public static class MissingExecutionContextParameterException extends ProcessingException {
        public MissingExecutionContextParameterException(String desc) {
            super(ProcessingExceptionType.MISSING_EXECUTION_CONTEXT_PARAM_ERROR, desc);
        }
        @Override public String getMessage() {
            return desc;
        }
    }

}
