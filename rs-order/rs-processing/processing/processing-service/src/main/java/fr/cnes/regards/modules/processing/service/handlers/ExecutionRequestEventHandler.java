package fr.cnes.regards.modules.processing.service.handlers;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.service.IExecutionService;
import fr.cnes.regards.modules.processing.service.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.service.events.PExecutionResultEvent;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ExecutionRequestEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<PExecutionRequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionRequestEventHandler.class);

    private IRuntimeTenantResolver runtimeTenantResolver;
    private ISubscriber subscriber;
    private IPublisher publisher;
    private IExecutionService execService;

    @Autowired
    public ExecutionRequestEventHandler(IRuntimeTenantResolver runtimeTenantResolver, ISubscriber subscriber,
            IPublisher publisher, IExecutionService execService) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
        this.publisher = publisher;
        this.execService = execService;
    }

    @Override public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(PExecutionRequestEvent.class, this);
    }

    @Override public void handle(String tenant, PExecutionRequestEvent message) {
        runtimeTenantResolver.forceTenant(tenant); // Needed in order to publish events

        LOGGER.info("exec={} - Execution request received", message.getExecutionId());
        execService.launchExecution(message)
            .subscribe(
                exec -> LOGGER.info("exec={} - Execution request registered correctly", message.getExecutionId()),
                err -> {
                    LOGGER.error("exec={} - Execution request error: {}", message.getExecutionId(), err.getMessage());
                    publisher.publish(new PExecutionResultEvent(
                            message.getExecutionId(),
                            message.getBatchId(),
                            ExecutionStatus.FAILURE,
                            List.empty(),
                            List.of(err.getClass().getName(), err.getMessage())
                    ));
                }
            );
    }
}
