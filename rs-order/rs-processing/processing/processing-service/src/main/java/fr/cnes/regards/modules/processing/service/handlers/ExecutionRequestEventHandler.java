package fr.cnes.regards.modules.processing.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.service.IExecutionService;
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

    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final ISubscriber subscriber;
    private final IExecutionService execService;

    @Autowired
    public ExecutionRequestEventHandler(
            IRuntimeTenantResolver runtimeTenantResolver,
            ISubscriber subscriber,
            IExecutionService execService
    ) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
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
                err -> LOGGER.error("exec={} - Execution request error: {}", message.getExecutionId(), err.getMessage())
            );
    }
}
