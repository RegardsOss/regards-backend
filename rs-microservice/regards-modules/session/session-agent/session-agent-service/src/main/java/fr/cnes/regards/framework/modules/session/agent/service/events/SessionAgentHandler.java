package fr.cnes.regards.framework.modules.session.agent.service.events;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.domain.StepEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author Iliana Ghazali
 **/
public class SessionAgentHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<StepEvent> {

    @Autowired(required = false)
    private IAgentSnapshotListener listener;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(StepEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage FileReferenceEvent bus messages !!");
        }
    }

    @Override
    public boolean validate(String tenant, StepEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<StepEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.debug("[STEP EVENT HANDLER] Handling {} StepEvents...", messages.size());
            long start = System.currentTimeMillis();
            handle(messages);
            LOGGER.debug("[STEP EVENT HANDLER] {} StepEvents handled in {} ms", messages.size(), System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Handle event by calling the listener method associated to the event type.
     * @param events {@link StepEvent}s
     */
    private void handle(List<StepEvent> events) {
        listener.onStepEventAvailable(events);
    }
}