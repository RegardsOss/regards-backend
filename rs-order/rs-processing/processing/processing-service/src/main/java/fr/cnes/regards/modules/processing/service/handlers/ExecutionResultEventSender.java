package fr.cnes.regards.modules.processing.service.handlers;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import fr.cnes.regards.modules.processing.domain.handlers.IExecutionResultEventSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException.mustWrap;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.SEND_EXECUTION_RESULT_ERROR;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.errorWithContextMono;

@Component
public class ExecutionResultEventSender implements IExecutionResultEventSender {

    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final IPublisher publisher;

    @Autowired
    public ExecutionResultEventSender(
            IRuntimeTenantResolver runtimeTenantResolver,
            IPublisher publisher
    ) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.publisher = publisher;
    }

    @Override public Mono<PExecutionResultEvent> send(String tenant, PExecutionResultEvent message) {
        return Mono.fromCallable(() -> {
            runtimeTenantResolver.forceTenant(tenant);
            publisher.publish(message);
            return message;
        })
        .onErrorResume(
            mustWrap(),
            errorWithContextMono(
                PExecution.class,
                (exec, t) -> new SendExecutionResultException(exec, "Sending execution result failed: " + message, t)
            )
        );
    }

    public static class SendExecutionResultException extends ProcessingExecutionException {
        public SendExecutionResultException(PExecution exec, String message,
                Throwable throwable) {
            super(SEND_EXECUTION_RESULT_ERROR, exec, message, throwable);
        }
    }
}
