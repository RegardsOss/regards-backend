package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;

public class ProcessingExecutionResultEventHandler implements IProcessingExecutionResultEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingExecutionResultEventHandler.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final ISubscriber subscriber;

    @Autowired
    public ProcessingExecutionResultEventHandler(
            IRuntimeTenantResolver runtimeTenantResolver,
            ISubscriber subscriber
    ) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
    }

    @Override public void onApplicationEvent(ApplicationEvent event) {
        subscriber.subscribeTo(PExecutionResultEvent.class, this);
    }

    @Override public void handle(String tenant, PExecutionResultEvent message) {
        runtimeTenantResolver.forceTenant(tenant);
        // TODO
    }

}
