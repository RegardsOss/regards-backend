package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Handler for new {@link StepPropertyUpdateRequestEvent}s
 *
 * @author Iliana Ghazali
 **/
@Component
@Profile("!nohandler")
public class SessionAgentHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<StepPropertyUpdateRequestEvent> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private SessionAgentHandlerService sessionAgentHandlerService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(StepPropertyUpdateRequestEvent.class, this);
    }

    @Override
    public boolean validate(String tenant, StepPropertyUpdateRequestEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<StepPropertyUpdateRequestEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.trace("[STEP EVENT HANDLER] Handling {} StepEvents...", messages.size());
            long start = System.currentTimeMillis();
            sessionAgentHandlerService.createStepRequests(messages);
            LOGGER.trace("[STEP EVENT HANDLER] {} StepEvents handled in {} ms", messages.size(),
                        System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}